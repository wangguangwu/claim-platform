# 签收流程设计

## 总体架构

平台级服务边界和分层方向遵循 [项目基线](../baseline.md)。本 Feature 在既定 Sign 上下文内编排子流程，不新增微服务或独立查询服务。

```mermaid
flowchart LR
    Caller[租户调用方] --> Gateway[claim-gateway\n协议、安全、限流、TenantContext]
    Gateway --> Interfaces[claim-sign-service interfaces\nsign / material / issue / internal]
    Interfaces --> App[application\n命令、查询、任务编排]
    App --> Domain[domain\nSign / Material / SignTask / ClaimIssue / CaseRelation / InteractionLog]
    App --> DB[(claim_sign_db)]
    App --> MQ[RocketMQ]
    MQ --> Consumer[异步 Consumer\n领取、幂等、重试]
    Consumer --> Adapter[Adapter 防腐层]
    Adapter --> OSS[OSS 材料原文]
    Adapter --> Insured[承保系统]
    Adapter --> Audit[审核系统]
    Adapter --> InternalCase[内部案件系统]
```

- Controller、MQ Consumer 和 JobHandler 只作为入口；事务编排、任务创建和流程推进在 `application`，状态合法性集中在领域聚合。
- 材料上传入口只保存材料元信息和 OSS 引用；材料原文不进入 MySQL、日志、指标或 Trace。
- `SignTask` 是异步工作的事实与原子意图记录。Dispatcher 只投递已提交任务，Consumer 领取后再调用 Adapter；数据库事务中不调用外部系统。

## 核心数据模型

```mermaid
erDiagram
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_MATERIAL : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_TASK : "logical sign_no"
    T_CLAIM_SIGN_TASK ||--o{ T_CLAIM_SIGN_TASK_LOG : "logical task_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_ISSUE : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_CASE_REL : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_INTERACTION_LOG : "logical sign_no"

    T_CLAIM_SIGN {
        bigint id PK
        varchar tenant_code
        varchar external_apply_no
        varchar sign_no UK
        varchar sign_status
        varchar current_stage
        varchar applicant_name
        json ext_json
    }
    T_CLAIM_SIGN_MATERIAL {
        bigint id PK
        varchar tenant_code
        varchar sign_no
        varchar material_no UK
        varchar material_status
        varchar oss_url
    }
    T_CLAIM_SIGN_TASK {
        bigint id PK
        varchar tenant_code
        varchar sign_no
        varchar task_no UK
        varchar task_type
        varchar task_status
        int retry_count
        datetime started_at
        datetime finished_at
        bigint cost_ms
    }
    T_CLAIM_SIGN_TASK_LOG {
        bigint id PK
        varchar tenant_code
        varchar sign_no
        varchar task_no
        int attempt_no
        varchar event_type
        datetime occurred_at
        bigint cost_ms
        varchar result
        varchar error_code
    }
    T_CLAIM_SIGN_ISSUE {
        bigint id PK
        varchar tenant_code
        varchar sign_no
        varchar issue_no UK
        varchar issue_status
        varchar resume_stage
    }
    T_CLAIM_SIGN_CASE_REL {
        bigint id PK
        varchar tenant_code
        varchar sign_no
        varchar case_no
    }
    T_CLAIM_SIGN_INTERACTION_LOG {
        bigint id PK
        varchar tenant_code
        varchar sign_no
        varchar task_no
        varchar interaction_type
        varchar direction
        varchar trace_id
        bigint cost_ms
        varchar result
    }
```

- 图中的连线是业务逻辑关联，**不是外键**。所有子表保留 `tenant_code + sign_no`，任务相关表再保留 `task_no`；这些列建立组合索引以支持租户隔离、恢复和审计查询。
- `t_claim_sign` 的唯一键为 `(tenant_code, external_apply_no)`，`sign_no` 为全局唯一。`id` 仅为内部主键。
- `t_claim_sign_task` 保存最新执行快照；`t_claim_sign_task_log` 追加记录 `CREATED`、`DISPATCHED`、`STARTED`、`SUCCEEDED`、`RETRY_SCHEDULED`、`FAILED`、`CANCELLED`，记录每次尝试的开始/结束与耗时。它是 `SignTask` 的历史实体，不是新聚合。
- `t_claim_sign_interaction_log` 只记录跨系统交互摘要；任务执行日志记录内部生命周期。两者共同回答“哪个子流程、何时开始/结束、耗时多久、结果如何”。

## 主状态机设计

```mermaid
stateDiagram-v2
    [*] --> SIGNED: 落表成功 / MATERIAL
    SIGNED --> PROCESSING: 材料任务开始 / MATERIAL
    PROCESSING --> PROCESSING: 材料成功 / INSURED_QUERY
    PROCESSING --> PROCESSING: 承保成功 / AUDIT_SYNC
    PROCESSING --> PROCESSING: 审核同步成功 / WAIT_RESULT
    PROCESSING --> ISSUE: 缺件、承保不符或审核驳回\n保留恢复阶段
    ISSUE --> PROCESSING: 补材/回复完成\n恢复至 resumeStage
    PROCESSING --> COMPLETED: 收到内部案件结果 / COMPLETED
    SIGNED --> REVOKED: 撤销
    PROCESSING --> REVOKED: 撤销
    ISSUE --> REVOKED: 撤销
    COMPLETED --> [*]
    REVOKED --> [*]
```

```mermaid
stateDiagram-v2
    [*] --> PENDING: 任务随业务事实创建
    PENDING --> RUNNING: Consumer 成功领取
    RUNNING --> SUCCEEDED: 子流程成功
    RUNNING --> RETRY_WAIT: 技术失败且可重试
    RETRY_WAIT --> PENDING: 到达下一次执行时间
    RUNNING --> FAILED: 重试耗尽或需人工介入
    PENDING --> CANCELLED: Sign 进入终态前取消
    RETRY_WAIT --> CANCELLED: Sign 进入终态前取消
    SUCCEEDED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
```

## 核心流程设计

```mermaid
flowchart TD
    A[提交 Sign] --> B[同事务：Sign、初始材料、MATERIAL_PROCESS 任务、任务日志、提交交互日志]
    B --> C[材料上传/处理异步任务]
    C -->|技术失败| C1[重试、死信或人工介入]
    C -->|缺件/业务不满足| I[创建 ClaimIssue\n记录 resumeStage=MATERIAL]
    C -->|成功| D[创建 INSURED_QUERY 任务]
    D -->|技术失败| D1[重试、死信或人工介入]
    D -->|承保不符| J[创建 ClaimIssue\n记录 resumeStage=INSURED_QUERY]
    D -->|成功| E[创建 AUDIT_SYNC 任务]
    E -->|技术失败| E1[重试、死信或人工介入]
    E -->|审核驳回| K[创建 ClaimIssue\n记录 resumeStage=AUDIT_SYNC]
    E -->|同步成功| F[WAIT_RESULT]
    F -->|内部案件结果| G[维护 CaseRelation]
    G --> H[COMPLETED]
    I --> L[补材/问题件回复]
    J --> L
    K --> L
    L --> M[ISSUE_REPLY_SYNC 任务]
    M --> N[按 resumeStage 创建恢复任务]
    N --> C
```

- `MATERIAL_PROCESS`、`INSURED_QUERY`、`AUDIT_SYNC` 是三项主异步子流程；问题件通知和回复同步使用既定的 `ISSUE_NOTIFY`、`ISSUE_REPLY_SYNC` 任务类型。
- 同一时刻每个 `signNo + taskType + stage` 只允许一个有效任务被领取；状态推进使用乐观锁或条件更新，数据库唯一索引只保障事实幂等，不承担流程锁。
- 业务失败与技术失败分流：前者创建问题件并暂停签收，后者保留签收阶段、进入任务重试，不把依赖超时伪装为缺件。

## 异步一致性与日志设计

```mermaid
sequenceDiagram
    autonumber
    participant App as 应用层
    participant DB as claim_sign_db
    participant MQ as RocketMQ
    participant C as Consumer
    participant X as 外部系统

    App->>DB: 事务写入 SignTask + TaskLog(CREATED)
    App->>DB: 提交事务
    App->>MQ: 投递 taskNo
    MQ->>C: 重复或正常投递
    C->>DB: 条件领取 + TaskLog(STARTED)
    C->>X: Adapter 调用，携带 taskNo / signNo
    X-->>C: 结果或超时
    C->>DB: 更新任务快照 + TaskLog(结束) + InteractionLog(costMs)
    alt 成功
        C->>DB: 推进 Sign，创建下一阶段任务
    else 技术失败
        C->>DB: RETRY_WAIT 或 FAILED
    else 业务不满足
        C->>DB: 创建 ClaimIssue，Sign 置 ISSUE
    end
```

- Producer 只发送已持久化的任务；若消息发送失败，由定时恢复任务扫描可投递任务，形成等效原子意图而非“先写库再直接发消息”。
- Consumer 条件领取失败代表重复消费或并发赢家已处理，应记录安全的重复处理结果后成功结束，不再调用外部系统。
- `started_at`、`finished_at` 与 `cost_ms` 记录本次任务执行耗时；交互日志的 `costMs` 单独记录一次外部调用耗时。查询流程 SLA 时以任务日志聚合，排查外部依赖时以交互日志定位。

## 接口与契约

计划暴露签收、材料、问题件与内部结果四类版本化入口，均以 `/api/v1/signs` 为根路径。具体方法、请求响应字段、材料上传方式、内部结果事件版本及 MQ Topic/Tag 尚未确认；这些稳定契约须先得到用户确认并回写基线，再创建 Controller、DTO 和消息定义。

## 测试策略

- 聚合测试：状态/阶段推进、终态保护、问题件恢复阶段与撤销前置条件。
- 任务测试：领取竞争、重复消息、重试耗尽、超时恢复、任务开始/结束/耗时日志。
- 集成测试：无外键 DDL、业务唯一键、租户过滤、逻辑关联校验和跨表事务回滚。
- Adapter 与接口测试：材料、承保、审核的成功、业务拒绝、超时、重复调用及脱敏响应。
- 验收测试：从签收落表到 `WAIT_RESULT`、问题件补材恢复、内部案件结果完成的端到端流程。
