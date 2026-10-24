# 签收流程设计

## 架构与边界

平台级服务边界和分层方向遵循 [项目基线](../baseline.md)。本 Feature 在既定 Sign 上下文内编排业务流程，不新增微服务、独立查询服务或通用工作流引擎。

```mermaid
flowchart LR
    Caller[租户调用方] --> Gateway[claim-gateway\n协议、安全、限流、TenantContext]
    Gateway --> Interfaces[claim-sign-service interfaces\nsign / material / issue / internal]
    Interfaces --> App[application\n命令、查询、任务编排]
    App --> Domain[domain\nSign / Material / SignTask / ClaimIssue / CaseRelation / InteractionLog]
    App --> DB[(claim_sign_db)]
    Dispatcher[Dispatcher / JobHandler] --> DB
    Dispatcher --> MQ[RocketMQ]
    MQ --> Consumer[Consumer]
    Consumer --> App
    App --> Adapter[Adapter 防腐层]
    Adapter --> OSS[OSS]
    Adapter --> Insured[承保系统]
    Adapter --> Audit[审核系统]
    Adapter --> InternalCase[内部案件系统]
```

- Gateway 负责协议、安全、租户和流量；签收服务只处理平台标准报文，并再次校验调用方租户范围。
- Controller、Consumer、JobHandler 都只是入口。事务编排与流程推进位于 `application`，状态合法性位于领域聚合。
- `SignTask` 是“当前需要执行什么、执行是否成功、失败何时重试”的异步执行事实与可靠消息载体，不承担通用工作流、完整执行事件审计或 SLA 平台职责。
- 外部系统调用仅经 Adapter，且不在数据库事务中执行。

## 核心数据模型

```mermaid
erDiagram
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_MATERIAL : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_TASK : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_ISSUE : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_CASE_REL : "logical sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_INTERACTION_LOG : "logical sign_no"
    T_CLAIM_SIGN_TASK ||--o{ T_CLAIM_SIGN_INTERACTION_LOG : "logical task_no"

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
        varchar biz_version
        int retry_count
        datetime next_retry_at
        datetime started_at
        datetime finished_at
        bigint cost_ms
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

- 图中连线是业务逻辑关联，**不是物理外键**。写入前由应用层校验关联事实，逻辑关联列建立查询索引，删除或状态变更不依赖数据库级联。
- `t_claim_sign` 的业务唯一键是 `(tenant_code, external_apply_no)`，`sign_no` 全局唯一；`id` 只作内部主键。
- `t_claim_sign_task` 只保存当前/最近一次执行快照。创建幂等使用 `sign_no + task_type + biz_version` 业务键；`biz_version` 表示该阶段的第几次有效执行，用于区分问题件恢复后的新任务。
- `t_claim_sign_interaction_log` 保留每次外部交互。一条 `SignTask` 可以对应多条交互日志，例如审核同步过程中的鉴权、附件传递和结果查询；它不记录材料原文或个人信息。

## 状态与流程

`signStatus` 只表示签收生命周期，`currentStage` 只表示当前处理位置；`SignTask.taskStatus` 表示子流程的执行状态，三者不混用。

```mermaid
stateDiagram-v2
    [*] --> SIGNED: 落表成功 / MATERIAL
    SIGNED --> PROCESSING: 材料任务开始 / MATERIAL
    PROCESSING --> PROCESSING: 材料成功 / INSURED_QUERY
    PROCESSING --> PROCESSING: 承保成功 / AUDIT_SYNC
    PROCESSING --> PROCESSING: 审核同步成功 / WAIT_RESULT
    PROCESSING --> ISSUE: 业务不满足\n记录 resumeStage
    ISSUE --> PROCESSING: 回复成功并恢复对应阶段
    PROCESSING --> COMPLETED: 收到 CASE_COMPLETE 结果事件
    SIGNED --> REVOKED: 撤销
    PROCESSING --> REVOKED: 撤销
    ISSUE --> REVOKED: 撤销
    COMPLETED --> [*]
    REVOKED --> [*]
```

```mermaid
flowchart TD
    A[提交 Sign] --> B[同事务：Sign、初始材料元信息、MATERIAL_PROCESS 任务、SIGN_SUBMIT 交互日志]
    B --> C[MATERIAL_PROCESS\n完整性检查、标准化、归档]
    C -->|技术失败| CR[任务重试、死信或人工介入]
    C -->|缺件/业务不满足| IM[创建 ClaimIssue\nresumeStage=MATERIAL]
    C -->|成功| D[INSURED_QUERY]
    D -->|技术失败| DR[任务重试、死信或人工介入]
    D -->|承保不符| II[创建 ClaimIssue\nresumeStage=INSURED_QUERY]
    D -->|成功| E[AUDIT_SYNC]
    E -->|技术失败| ER[任务重试、死信或人工介入]
    E -->|审核驳回| IA[创建 ClaimIssue\nresumeStage=AUDIT_SYNC]
    E -->|同步成功| F[WAIT_RESULT]
    F -->|CASE_BIND| G[维护 CaseRelation\n仍处于 WAIT_RESULT]
    F -->|CASE_COMPLETE| H[COMPLETED]
    IM --> R[补材/问题件回复]
    II --> R
    IA --> R
    R --> S[ISSUE_REPLY_SYNC]
    S --> T{resumeStage}
    T -->|MATERIAL| C
    T -->|INSURED_QUERY| D
    T -->|AUDIT_SYNC| E
```

- 材料上传与材料处理不是同一概念：上传入口只接收并保存 OSS 引用和材料元信息；`MATERIAL_PROCESS` 异步执行材料完整性检查、标准化或归档等后续处理，不代替调用方上传原文。
- `CaseRelation` 仅维护 `signNo ↔ caseNo`。收到 `CASE_BIND` 只建立或更新关系；只有明确的 `CASE_COMPLETE` 内部结果事件才将 `Sign` 推至 `COMPLETED`，因此可支持一个签收关联多个案件。
- 业务不满足创建 `ClaimIssue` 并暂停流程；技术失败只改变任务状态并重试。问题件解决后依据 `resumeStage` 创建对应任务，不统一回到材料处理。

## 异步任务与一致性

任务状态保持最小集合：`PENDING → RUNNING → SUCCEEDED`；可重试技术失败走 `RUNNING → RETRY_WAIT → PENDING`，重试耗尽或人工介入走 `FAILED`，签收终态前未执行任务可置 `CANCELLED`。`startedAt`、`finishedAt`、`costMs` 仅保存最近一次执行摘要，不建立独立任务事件历史表。

```mermaid
sequenceDiagram
    autonumber
    participant App as Application
    participant DB as claim_sign_db
    participant D as Dispatcher / Recovery Job
    participant MQ as RocketMQ
    participant C as Consumer
    participant X as 外部系统

    App->>DB: 事务写入 SignTask(PENDING)
    App->>DB: 提交事务
    D->>DB: 扫描 PENDING 或到期 RETRY_WAIT 任务
    D->>MQ: 投递 taskNo
    MQ->>C: 正常或重复投递
    C->>DB: CAS: PENDING -> RUNNING
    alt 领取成功
        C->>X: Adapter 调用，携带 taskNo / signNo
        X-->>C: 结果或超时
        C->>DB: 更新 Task 快照 + InteractionLog
        alt 成功
            C->>DB: 推进 Sign，创建下一阶段任务
        else 技术失败
            C->>DB: RETRY_WAIT 或 FAILED
        else 业务不满足
            C->>DB: 创建 ClaimIssue，Sign 置 ISSUE
        end
    else 已被其他消费者领取或完成
        C-->>MQ: 安全结束，不调用外部系统
    end
```

- Application 不直接发送 MQ。Dispatcher 扫描已提交的 `PENDING` 任务及到期 `RETRY_WAIT` 任务后投递；投递失败保留任务事实并由恢复 Job 再次扫描，实现可靠消息而不引入额外 Outbox 表。
- 数据库唯一键负责“不要重复创建”：同一 `signNo + taskType + bizVersion` 只能创建一个有效任务。条件更新负责“不要重复执行”：`UPDATE ... SET task_status = 'RUNNING' WHERE task_no = ? AND task_status = 'PENDING'` 仅影响一行时才允许调用外部系统。
- `SignTask` 记录一次业务动作的当前执行事实；`InteractionLog` 记录其执行过程中每一次跨系统交互，二者不重复承担任务事件历史。

## 接口与测试

计划暴露签收、材料、问题件与内部结果四类版本化入口，均以 `/api/v1/signs` 为根路径。具体 HTTP 字段、材料上传方式、内部结果事件版本和 MQ Topic/Tag 尚未确认；确认后须先回写基线再创建 Controller、DTO 和消息定义。

- 聚合测试：生命周期/阶段推进、终态保护、问题件恢复阶段与撤销前置条件。
- 任务测试：创建幂等、条件领取、重复消息、重试耗尽、到期恢复与最近执行摘要更新。
- 集成测试：无外键 DDL、业务唯一键、租户过滤、逻辑关联校验与跨表事务回滚。
- Adapter 与接口测试：材料、承保、审核的成功、业务拒绝、超时、重复调用及脱敏响应。
- 验收测试：签收至 `WAIT_RESULT`、问题件按阶段恢复、多个 `CaseRelation` 与 `CASE_COMPLETE` 事件完成流程。
