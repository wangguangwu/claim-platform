# claim-sign-service 架构基准

本文件是签收服务域内架构的唯一长期基准：分层与租户适配、状态机、数据模型、核心时序、幂等与补偿。签收服务的实现以本文件为准，交付后不删除。

跨服务的公共约束仍以 [项目基线](../specs/baseline.md) 为准：命名词表、技术栈、服务边界、对外契约固定值、租户隔离与脱敏红线。域内细节与本文件冲突时按"平台级优先"报告并修订，不得各写一份。

活动 Feature 的稳定领域内核见 [签收内核需求](../specs/sign-core/requirements.md) 与 [签收内核设计](../specs/sign-core/design.md)；租户适配与外部协同见 [签收适配与协同需求](../specs/sign-adaptation-collaboration/requirements.md)。本文件不复述这些增量。

## 1. 架构

### 1.1 分层

- **Gateway**：独立网关服务，负责鉴权、tenantCode 识别、路由、入口限流
- **适配层**：处理租户协议差异，完成验签、加解密、解析、校验、转换
- **业务层**：签收、材料、承保、审核交付、问题件、补材、状态维护
- **异步执行层**：消费签收、审核、问题件等业务事件
- **基础设施层**：MySQL、Redis、RocketMQ、XXL-Job、Nacos

```mermaid
flowchart TB
    subgraph CALLER["调用方"]
        T["租户系统"]
    end

    GW["Gateway<br/>鉴权 / tenantCode / 限流"]

    subgraph SIGN["理赔签收平台"]
        ADAPT["适配层 · Tenant Adapter + Pipeline<br/>验签 / 加解密 / Parser / Validator / Converter"]
        CORE["业务层<br/>签收 / 材料 / 承保 / 审核交付 / 问题件 / 补材 / 状态"]
        CONSUMER["异步执行层<br/>MQ Consumer"]
    end

    subgraph EXT["外部协同"]
        OSS["OSS"]
        INSURED["承保系统"]
        AUDIT["审核系统"]
    end

    subgraph INFRA["基础设施层"]
        DB[("MySQL")]
        REDIS[("Redis")]
        MQ["RocketMQ"]
        JOB["XXL-Job"]
        NACOS["Nacos"]
    end

    T --> GW --> ADAPT --> CORE

    CORE --> DB
    CORE --> REDIS
    CORE --> MQ

    MQ --> CONSUMER
    CONSUMER --> DB
    CONSUMER --> REDIS
    CONSUMER --> OSS
    CONSUMER --> INSURED

    CONSUMER --> MQ
    MQ <--> AUDIT

    JOB --> DB
    JOB --> MQ

    GW -. 限流配置 .-> NACOS
```

租户 HTTP 请求经过独立 Gateway 后进入理赔签收平台，由 Tenant Adapter + Pipeline 完成租户协议标准化。

Pipeline 的角色划分与编排顺序是域内长期结构；某个真实租户的验签算法、加解密方式与报文字段属于该租户自己的接入范围，由对应的租户接入 Spec 落地，平台不代替租户决定协议。本地开发用 `local` profile 的测试租户 Pipeline 验证抽象与编排，它不代表任何真实租户的报文。

落点映射（包结构归属见基线 §6）：适配层 = `interfaces/acl`，业务层 = `application` + `domain`，异步执行层 = `interfaces` 的消息入口 + `application/task` 编排，基础设施层 = `infrastructure`。

### 1.2 同步与异步分界

同步签收只完成：

**申请唯一性判断 → signNo 生成 → 申请及初始材料关系落库 → 返回签收结果**

事务提交后发布签收受理事件，影像处理、承保判断、自动承保、审核交付均异步执行。

XXL-Job 扫描长时间未推进的申请并重新发布对应业务事件，用于 MQ 投递失败或流程中断后的补偿，不维护独立任务模型。签收服务必须注册执行器、注册补偿 Handler，并由部署配置创建对应调度；仅启动 XXL-Job Admin 容器不构成补偿能力已交付。

### 1.3 外部协同

审核系统统一通过 RocketMQ 与签收平台交互：

- 平台 → 审核系统：审核任务、问题件回复
- 审核系统 → 平台：签收结果、问题件

承保系统为同步调用，按 `signNo` 幂等。材料原文与影像存 OSS，库内只保留材料信息与地址引用。

## 2. 核心状态流转

| 状态                  | 含义       | 进入条件            |
| ------------------- | -------- | --------------- |
| `PENDING_ACCEPT`    | 待受理      | 申请落库成功          |
| `ACCEPT_SUCCESS`    | 受理成功     | 影像处理成功          |
| `ACCEPT_FAILED`     | 受理失败     | 影像处理失败          |
| `AUTO_UNDERWRITING` | 自动承保中    | 未查询到有效保单，发起自动承保 |
| `WAIT_AUDIT_ACCEPT` | 等待审核系统签收 | 审核任务已成功发送       |
| `SIGN_COMPLETED`    | 签收完成     | 审核系统返回签收成功及案件信息 |
| `ISSUE_CASE`        | 问题件      | 审核系统发起问题件       |
| `CLAIM_REVOKED`     | 已撤销      | 租户发起撤销          |

`sign_status` 是签收生命周期的唯一状态字段，环节语义由状态本身表达，不再维护第二个阶段字段。

```mermaid
stateDiagram-v2
    [*] --> PENDING_ACCEPT: 申请落库

    PENDING_ACCEPT --> ACCEPT_SUCCESS: 影像处理成功
    PENDING_ACCEPT --> ACCEPT_FAILED: 影像处理失败

    ACCEPT_SUCCESS --> WAIT_AUDIT_ACCEPT: 存在有效保单 / 提交审核
    ACCEPT_SUCCESS --> AUTO_UNDERWRITING: 无有效保单 / 自动承保

    AUTO_UNDERWRITING --> WAIT_AUDIT_ACCEPT: 自动承保成功 / 提交审核

    WAIT_AUDIT_ACCEPT --> SIGN_COMPLETED: 审核系统签收成功
    WAIT_AUDIT_ACCEPT --> ISSUE_CASE: 审核系统发起问题件

    ISSUE_CASE --> WAIT_AUDIT_ACCEPT: 问题件回复 / 重新提交审核

    PENDING_ACCEPT --> CLAIM_REVOKED: 租户撤销
    ACCEPT_SUCCESS --> CLAIM_REVOKED: 租户撤销
    AUTO_UNDERWRITING --> CLAIM_REVOKED: 租户撤销
    WAIT_AUDIT_ACCEPT --> CLAIM_REVOKED: 租户撤销
    ISSUE_CASE --> CLAIM_REVOKED: 租户撤销

    ACCEPT_FAILED --> [*]
    SIGN_COMPLETED --> [*]
    CLAIM_REVOKED --> [*]
```

`PENDING_ACCEPT`、`ACCEPT_SUCCESS`、`AUTO_UNDERWRITING`、`WAIT_AUDIT_ACCEPT`、`ISSUE_CASE` 为可推进状态；`ACCEPT_FAILED`、`SIGN_COMPLETED`、`CLAIM_REVOKED` 为终态。

## 3. 核心数据模型

```mermaid
erDiagram
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_MATERIAL : "sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_ISSUE : "sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_CASE_REL : "sign_no"
    T_CLAIM_SIGN ||--o{ T_CLAIM_SIGN_INTERACTION_LOG : "sign_no"

    T_CLAIM_SIGN {
        bigint id PK "主键ID"
        varchar tenant_code "租户编码"
        varchar sign_no UK "平台签收编号"
        varchar external_apply_no "外部申请编号"
        varchar applicant_name "申请人姓名"
        varchar sign_status "签收状态"
        json applicant_info "申请人信息"
        json accident_person_info "出险人信息"
        json main_insured_info "主被保险人信息"
        json payee_info "领款人信息"
        json ext_json "扩展信息"
        int protocol_version "协议版本"
        int version "乐观锁版本"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint is_deleted "是否删除"
    }

    T_CLAIM_SIGN_MATERIAL {
        bigint id PK "主键ID"
        varchar tenant_code "租户编码"
        varchar sign_no "平台签收编号"
        varchar batch_no "材料上传批次号"
        varchar type "材料类型"
        varchar name "材料名称"
        varchar path "平台材料地址"
        varchar original_path "原始材料地址"
        tinyint is_downloaded "是否已下载处理"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint is_deleted "是否删除"
    }

    T_CLAIM_SIGN_ISSUE {
        bigint id PK "主键ID"
        varchar tenant_code "租户编码"
        varchar sign_no "平台签收编号"
        varchar issue_no "平台问题件定位号"
        varchar auditor "审核人"
        varchar reason "问题件原因"
        json ext_json "问题件扩展信息"
        tinyint is_sync_tenant "是否已同步租户"
        tinyint is_supplement "是否需要补充材料"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint is_deleted "是否删除"
    }

    T_CLAIM_SIGN_CASE_REL {
        bigint id PK "主键ID"
        varchar tenant_code "租户编码"
        varchar sign_no "平台签收编号"
        varchar case_no "平台案件编号"
        varchar external_case_no "外部案件编号"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint is_deleted "是否删除"
    }

    T_CLAIM_SIGN_INTERACTION_LOG {
        bigint id PK "主键ID"
        varchar tenant_code "租户编码"
        varchar sign_no "平台签收编号"
        varchar interaction_type "交互类型"
        varchar direction "交互方向"
        varchar source_system "发起系统"
        varchar target_system "对端系统"
        varchar request_id "单次请求ID"
        varchar trace_id "链路TraceId"
        varchar result "处理结果"
        varchar error_code "错误码"
        varchar message "处理结果说明"
        bigint cost_ms "调用耗时"
        datetime created_at "创建时间"
    }
```

补充约束：

- `t_claim_sign`：`sign_no` 全局唯一，`tenant_code + external_apply_no` 业务唯一。
- `t_claim_sign_issue`：`issue_no` 由签收服务在保存问题时发放，在 `tenant_code + sign_no` 范围内唯一，不要求全局唯一，因此不占用跨租户发号通道。
- `t_claim_sign_case_rel`：一个 `sign_no` 可以关联多个 `case_no`。
- 表之间通过 `tenant_code + sign_no` 逻辑关联，不建立物理外键。
- `t_claim_sign_interaction_log` 为追加型日志表，只保留 `created_at`，不需要 `updated_at` 和 `is_deleted`。
- 人员四类信息（申请人、出险人、主被保险人、领款人）以 JSON 列整体入库，不建领域对象与结构化子字段；只有 `applicant_name` 作为可检索列。
- `interaction_type`、`direction`、`source_system`、`target_system` 取值按基线 §10 登记；`error_code`、`cost_ms` 用于可定位信号与诊断，不得记录请求响应原文与材料内容。

一个签收申请可以对应多个案件：

```
signNo
├─ caseNo A
├─ caseNo B
└─ caseNo C
```

审核系统签收成功后返回案件信息，平台保存 `1:N` 的 CaseRelation，再将申请推进至 `SIGN_COMPLETED`。

问题件的多次打回绑定不依赖对外契约：每条 `ClaimIssue` 由平台自有的 `issue_no` 定位，租户回复与补充材料按 `signNo + issueNo` 绑定，`ext_json` 承载审核侧原样摘要。平台契约 A 档只决定**对外字段名、消息载荷与回复绑定语义**，一律映射到已存在的 `issue_no`，不因对外命名变更 DDL；确实需要新的可检索列时，必须先回写本节列定义，再作为独立版本化 `ALTER` 迁移交付，不得在实现里猜测列名。

## 4. 核心时序

### 4.1 理赔申请签收

同步签收只负责申请标准化、幂等校验、生成 `signNo` 和数据落库，不执行影像、承保和审核处理。

```mermaid
sequenceDiagram
    autonumber

    participant T as 租户系统
    participant G as Gateway
    participant C as TenantContext
    participant R as TenantAdapterRouter
    participant P as Tenant Pipeline
    participant S as SignService
    participant L as Redisson
    participant DB as MySQL
    participant EP as EventPublisher 端口

    T->>G: 提交理赔申请
    G->>G: 鉴权 / tenantCode / 限流
    G->>C: 建立 tenantCode + protocolVersion 上下文

    C->>R: 获取租户适配组件
    R-->>P: 返回对应 Pipeline

    P->>P: 验签 / 解密
    P->>P: Parser：解析租户报文
    P->>P: Validator：参数及组合校验
    P->>P: Converter：转换标准申请模型

    P->>S: 标准理赔申请

    Note over S,DB: 第一层：DB 快速查询

    S->>DB: 查询 tenantCode + externalApplyNo

    alt 申请已存在
        DB-->>S: 返回原申请
    else 申请不存在
        DB-->>S: 不存在

        Note over S,L: 第二层：Redisson 业务锁

        S->>L: 获取 tenantCode + externalApplyNo 业务锁
        L-->>S: 获取成功

        S->>DB: 锁内二次查询

        alt 已被其他请求创建
            DB-->>S: 返回原申请
        else 仍不存在
            DB-->>S: 不存在

            S->>S: 生成 signNo

            Note over S,DB: 第三层：MySQL UNIQUE 最终兜底

            S->>DB: 事务创建申请 / 材料关系 / 初始状态

            alt 保存成功
                DB-->>S: 创建成功
            else 唯一键冲突
                DB-->>S: DuplicateKeyException
                S->>DB: 查询已创建申请
                DB-->>S: 返回原申请
            end
        end

        S->>L: 释放业务锁

        opt 本次创建成功
            S->>EP: 事务提交后表达 SIGN_ACCEPT 事件意图
            EP-->>S: 投递结果；失败只记录结构化日志与恢复事实
        end
    end

    S-->>P: SignResult
    P->>P: 响应转换 / 签名 / 加密
    P-->>G: 租户响应
    G-->>T: 返回 signNo
```

签收创建采用三层幂等控制：

- MySQL 快速查询，直接返回已存在申请
- `tenantCode + externalApplyNo` 业务锁，保证并发创建串行化
- `UNIQUE(tenant_code, external_apply_no)` 作为最终数据库兜底

同步请求结束后，平台异步发布 `SIGN_ACCEPT` 事件启动后续受理流程。创建事务提交后，内核只通过 `EventPublisher` 端口表达事件意图，具体 MQ 投递由适配与协同实现；端口失败不回滚已提交事实、不改变同步响应，由 XXL-Job 按 `PENDING_ACCEPT` 状态补偿重发。回放已存在申请时不重复表达事件意图。

### 4.2 异步受理与审核交付

异步受理负责影像处理、保单判断、自动承保和审核任务提交。

分布式锁只保护 Sign 状态的读取、校验和更新，不覆盖文件 IO、外部调用和 MQ 投递。

```mermaid
sequenceDiagram
    autonumber

    participant MQ as RocketMQ
    participant C as SignConsumer
    participant L as Redisson
    participant DB as MySQL
    participant O as OSS
    participant I as 承保系统
    participant A as 审核系统

    MQ->>C: SIGN_ACCEPT(signNo)

    C->>L: 获取 signNo 业务锁
    L-->>C: 获取成功

    critical 状态校验
        C->>DB: 查询 Sign 当前状态

        alt 状态不是 PENDING_ACCEPT
            C-->>C: 幂等结束
        end
    end

    C->>L: 释放业务锁

    C->>O: 获取并处理影像
    O-->>C: 影像处理结果

    alt 影像处理失败
        C->>L: 获取 signNo 业务锁

        critical 状态更新
            C->>DB: 校验当前状态
            C->>DB: Sign -> ACCEPT_FAILED
        end

        C->>L: 释放业务锁

    else 影像处理成功
        C->>L: 获取 signNo 业务锁

        critical 状态更新
            C->>DB: 校验当前状态
            C->>DB: Sign -> ACCEPT_SUCCESS
        end

        C->>L: 释放业务锁

        C->>I: 查询有效保单
        I-->>C: 查询结果

        alt 存在有效保单
            C->>MQ: 发布审核任务
            MQ-->>C: 投递结果
            MQ-->>A: 审核任务

        else 无有效保单
            C->>L: 获取 signNo 业务锁

            critical 状态更新
                C->>DB: 校验 ACCEPT_SUCCESS
                C->>DB: Sign -> AUTO_UNDERWRITING
            end

            C->>L: 释放业务锁

            C->>I: 发起自动承保（signNo 幂等）
            I-->>C: 承保结果

            alt 自动承保成功
                C->>MQ: 发布审核任务
                MQ-->>C: 投递结果
                MQ-->>A: 审核任务
            else 自动承保失败
                C-->>C: 保持 AUTO_UNDERWRITING / 等待 §4.3 恢复
            end
        end

        opt 审核任务已成功投递
            C->>L: 获取 signNo 业务锁

            critical 状态更新
                C->>DB: 校验当前状态

                alt 未被撤销
                    C->>DB: Sign -> WAIT_AUDIT_ACCEPT
                else CLAIM_REVOKED
                    C-->>C: 不再推进状态
                end
            end

            C->>L: 释放业务锁
        end
    end
```

外部副作用调用需要具备业务幂等能力：

- 自动承保以 `signNo` 作为业务幂等标识
- 审核任务使用稳定业务标识避免重复创建
- 重复 MQ 消息通过当前 `sign_status` 校验安全结束
- `WAIT_AUDIT_ACCEPT` 只在审核任务投递成功后进入；投递或承保失败时保持原状态，由 §4.3 恢复，不得凭“已消费消息”推进状态
- 每次外部调用（OSS、承保、审核任务）以独立事务追加 `InteractionLog`，记录不进入状态锁范围

### 4.3 XXL-Job 补偿

XXL-Job 不统一"重放上一动作"，而是根据当前状态执行明确的恢复策略。告警通道未交付期间，表中“人工处理”与“超时”均输出基线 §10 规定的结构化日志；跨系统动作另写 `InteractionLog`，不宣称已发送生产告警。

| 当前状态 | 恢复策略 |
|---|---|
| `PENDING_ACCEPT` | 补发 `SIGN_ACCEPT` |
| `ACCEPT_SUCCESS` | 重新查询有效保单，并继续后续流程 |
| `AUTO_UNDERWRITING` | 承保接口支持幂等时允许重试；否则记录人工处理信号 |
| `WAIT_AUDIT_ACCEPT` | 不重复提交审核任务，记录超时信号 |
| `ISSUE_CASE` | 不自动恢复，等待租户回复 |
| `ACCEPT_FAILED` | 不自动恢复 |
| `SIGN_COMPLETED` | 不处理 |
| `CLAIM_REVOKED` | 如已进入审核阶段且撤销通知未成功，则补发撤销事件 |

```mermaid
sequenceDiagram
    autonumber

    participant J as XXL-Job
    participant S as SignService
    participant DB as MySQL
    participant MQ as RocketMQ
    participant I as 承保系统

    J->>DB: 扫描长时间未推进的 Sign
    DB-->>J: 返回待检查申请

    loop 每个 Sign
        alt PENDING_ACCEPT
            J->>MQ: 补发 SIGN_ACCEPT

        else ACCEPT_SUCCESS
            J->>S: 恢复承保判断
            S->>I: 重新查询有效保单
            I-->>S: 查询结果

        else AUTO_UNDERWRITING
            J->>S: 检查承保重试条件

            alt 承保接口支持幂等
                S->>I: 使用 signNo 重试自动承保
            else 不满足自动恢复条件
                S-->>S: 记录人工处理信号
            end

        else WAIT_AUDIT_ACCEPT
            J->>S: 检查等待时长
            S-->>S: 记录超时信号，不重复提交审核

        else CLAIM_REVOKED
            J->>S: 检查撤销通知结果

            alt 已提交审核但撤销未同步成功
                S->>MQ: 补发 CLAIM_REVOKE
            end
        end
    end
```

补偿只重发事件或重试幂等外部调用，不直接改写业务状态；状态推进始终经 Consumer 的锁内校验。

### 4.4 审核结果

审核系统签收成功后返回一个或多个案件号，平台保存 `1:N` 案件关系后完成签收。

`SIGN_COMPLETED` 和 `CLAIM_REVOKED` 均执行终态保护。

```mermaid
sequenceDiagram
    autonumber

    participant A as 审核系统
    participant MQ as RocketMQ
    participant C as SignConsumer
    participant L as Redisson
    participant DB as MySQL

    A->>MQ: 审核签收结果
    MQ->>C: 消费审核结果

    C->>L: 获取 signNo 业务锁
    L-->>C: 获取成功

    critical 审核结果处理
        C->>DB: 查询 Sign 当前状态

        alt WAIT_AUDIT_ACCEPT + 签收成功
            C->>DB: 保存 1:N CaseRelation
            C->>DB: Sign -> SIGN_COMPLETED

        else CLAIM_REVOKED
            C->>DB: 记录迟到回调
            C-->>C: 拒绝状态推进并记录可定位信号

        else SIGN_COMPLETED
            C-->>C: 重复消息幂等结束

        else 状态不允许
            C->>DB: 记录异常事件
            C-->>C: 拒绝状态推进并记录可定位信号
        end
    end

    C->>L: 释放业务锁
```

### 4.5 问题件下发

只有审核系统的问题件事件可以使 Sign 进入 `ISSUE_CASE`。

审核系统可以多次发起问题件；已完成或已撤销的申请不得再次进入问题件状态。

```mermaid
sequenceDiagram
    autonumber

    participant A as 审核系统
    participant MQ as RocketMQ
    participant C as SignConsumer
    participant L as Redisson
    participant DB as MySQL
    participant AD as Tenant Adapter
    participant T as 租户系统

    A->>MQ: 问题件事件
    MQ->>C: 消费问题件

    C->>L: 获取 signNo 业务锁
    L-->>C: 获取成功

    critical 问题件状态处理
        C->>DB: 查询 Sign 当前状态

        alt SIGN_COMPLETED / CLAIM_REVOKED
            C->>DB: 记录迟到问题件
            C-->>C: 拒绝状态推进并记录可定位信号

        else 正常处理
            C->>C: 发放 issueNo
            C->>DB: 保存 ClaimIssue（issueNo；审核侧摘要入 ext_json）
            C->>DB: Sign -> ISSUE_CASE
        end
    end

    C->>L: 释放业务锁

    opt 问题件保存成功
        C->>AD: 转换租户问题件模型（携带平台 issueNo）
        AD->>T: 下发问题件
        C->>DB: 独立事务追加 ISSUE_NOTIFY 交互日志
    end
```

状态约束：

- 只有审核系统可以使 Sign **进入** `ISSUE_CASE`
- 平台处理问题件回复后可以使 Sign **离开** `ISSUE_CASE`
- 后续审核系统再次打回时，可以再次进入 `ISSUE_CASE`
- 每次打回是一条独立的 `ClaimIssue`，由 `issueNo` 定位，新的问题件不覆盖历史回复与补材关系

### 4.6 问题件回复与补材

租户提交问题件回复时，同步链路只保存回复和补材关系；文件处理和审核提交继续异步执行。

回复与补材必须绑定到一条具体的 `ClaimIssue`：租户侧定位字段的名称与载荷由平台契约 A 档确认，在 `interfaces/acl` 内立即映射为平台 `issueNo`，应用层与领域层只认识 `issueNo`，不引入第二套问题件标识。

```mermaid
sequenceDiagram
    autonumber

    participant T as 租户系统
    participant G as Gateway
    participant AD as Tenant Adapter
    participant S as SignService
    participant DB as MySQL
    participant MQ as RocketMQ
    participant C as SignConsumer
    participant L as Redisson
    participant O as OSS
    participant A as 审核系统

    T->>G: 提交问题件回复 / 补充材料
    G->>AD: 转发租户报文

    AD->>AD: 验签 / 解密 / 校验 / 转换
    AD->>S: 标准问题件回复

    S->>DB: 校验 Sign = ISSUE_CASE 且 issueNo 属于该 signNo
    S->>DB: 按 issueNo 保存回复及补材关系
    S->>MQ: 发布 ISSUE_REPLY（signNo + issueNo）
    S-->>AD: 接收成功
    AD-->>G: 租户响应
    G-->>T: 返回接收结果

    MQ->>C: ISSUE_REPLY(signNo + issueNo)

    C->>L: 获取 signNo 业务锁
    L-->>C: 获取成功

    critical 状态校验
        C->>DB: 校验 Sign = ISSUE_CASE
        C->>DB: 校验该 issueNo 仍待处理
    end

    C->>L: 释放业务锁

    opt 存在补充材料
        C->>O: 获取并处理补充材料
        O-->>C: 处理结果
        C->>DB: 更新材料结果
    end

    C->>MQ: 发布问题件回复（携带 issueNo）
    MQ-->>A: 问题件回复

    C->>L: 获取 signNo 业务锁
    L-->>C: 获取成功

    critical 状态更新
        C->>DB: 再次校验 Sign 当前状态

        alt 仍为 ISSUE_CASE
            C->>DB: 更新该 issueNo 的问题件
            C->>DB: Sign -> WAIT_AUDIT_ACCEPT

        else CLAIM_REVOKED
            C-->>C: 不再推进状态

        else 其他状态
            C->>DB: 记录异常
        end
    end

    C->>L: 释放业务锁
```

一个问题件允许多次回复和补材；重新提交审核后进入 `WAIT_AUDIT_ACCEPT`，审核系统仍可再次发起新的问题件。

### 4.7 申请撤销

撤销由租户发起。

进入 `CLAIM_REVOKED` 后，本地 Sign 不再接受其他业务结果推进；如果申请已经进入审核阶段，还需要通知审核系统执行撤销或作废。

```mermaid
sequenceDiagram
    autonumber

    participant T as 租户系统
    participant G as Gateway
    participant AD as Tenant Adapter
    participant S as SignService
    participant L as Redisson
    participant DB as MySQL
    participant MQ as RocketMQ
    participant A as 审核系统

    T->>G: 发起撤销申请
    G->>G: 鉴权 / tenantCode / 限流
    G->>AD: 转发租户报文

    AD->>AD: 验签 / 解密 / 参数校验
    AD->>S: 标准撤销请求

    S->>L: 获取 signNo 业务锁
    L-->>S: 获取成功

    critical 撤销状态变更
        S->>DB: 查询 Sign 当前状态

        alt 当前状态允许撤销
            S->>DB: Sign -> CLAIM_REVOKED
            S-->>AD: 撤销成功

        else 当前状态不允许撤销
            S-->>AD: 撤销失败
        end
    end

    S->>L: 释放业务锁

    opt 已进入审核阶段
        S->>MQ: 发布 CLAIM_REVOKE
        MQ-->>A: 撤销 / 作废通知
    end

    AD->>AD: 响应转换 / 签名 / 加密
    AD-->>G: 租户响应
    G-->>T: 返回撤销结果
```

`CLAIM_REVOKED` 为强终态：

- 审核成功回调不得再推进为 `SIGN_COMPLETED`
- 问题件不得再推进为 `ISSUE_CASE`
- 承保、补材等迟到结果只记录交互结果并产生可定位信号
- 已进入审核阶段的申请需要向审核系统发送撤销 / 作废通知

## 5. 事件与交互

| 类型 | 名称 | 方向 | 触发 |
| --- | --- | --- | --- |
| 领域事件 | `SIGN_ACCEPT` | 平台内 | 签收落库成功，启动异步受理 |
| 领域事件 | `ISSUE_REPLY` | 平台内 | 问题件回复与补材接收成功，载荷绑定 `issueNo` |
| 领域事件 | `CLAIM_REVOKE` | 平台 → 审核系统 | 撤销已进入审核阶段的申请 |
| 交互 | `AUTO_UNDERWRITING` | 平台 → 承保系统 | 未查询到有效保单，按 `signNo` 幂等发起 |
| 交互 | `AUDIT_SYNC` | 平台 → 审核系统 | 受理成功或问题件恢复后提交审核 |
| 交互 | `AUDIT_RESULT` | 审核系统 → 平台 | 审核签收结果，携带一个或多个案件号 |
| 交互 | `AUDIT_ISSUE` | 审核系统 → 平台 | 审核发起问题件 |
| 交互 | `ISSUE_NOTIFY` | 平台 → 租户 | 问题件下发，携带平台 `issueNo` 供回复与补材绑定 |
| 交互 | `MATERIAL_FETCH` | 平台 → OSS | 影像与补充材料下载处理 |

领域事件载荷必须携带基线 §7 规定的 `eventId`、事件版本、`tenantCode`、聚合类型、聚合标识、发生时间与 `traceId`；`signNo` 是签收的业务定位标识，问题件相关事件另带 `issueNo`。完整 InteractionType 取值清单以基线 §10 为准。
