# 项目基线

本文件是 `claim-platform` 唯一长期有效的**平台级**公共约束来源：产品范围、核心业务规则、技术约束、工程结构、架构分层、全局需求与范围之外。

签收服务的**域内架构**——分层与租户适配、状态机、数据模型、核心时序、幂等与补偿——以 [签收服务架构基准](../architecture/claim-sign-service.md) 为准，本文件不重复这些细节，只在跨服务必要的地方索引。两份文档冲突时按"平台级优先"报告并修订，不得各写一份权威。

功能级需求写在 `docs/specs/<feature-name>/`，只描述本功能的增量，不重复以下内容，也不得降低本文件的约束。

规则的单一分工：任务流程与全部 Git 操作见 `docs/engineering/workflow.md`，Spec 的建与不建见 `docs/engineering/specification.md`，代码写法与 HTTP 响应细则见 `docs/engineering/implementation.md`，签收域内架构见 `docs/architecture/claim-sign-service.md`，项目导航见 `README.md`。本文件不重复这四份规范的内容，只定义"所有功能都必须遵守"的边界。

## 1. 项目定位

Claim Platform（理赔平台）负责理赔链路的**前段交互**与**后段交互**，不负责中间的案件审核、理算与核赔：

- 前段：理赔申请签收、影像与材料处理、承保判断（有效保单查询与自动承保）、审核交付、问题件、补材、撤销等与租户及审核系统的交互。
- 后段：案件生成后，案件推送、回执、重试、状态同步等与保司的交互。

前段的内部链路固定为：`签收受理 → 影像与材料处理 → 承保判断（有效保单或自动承保）→ 审核交付 → 等待审核签收结果 → 案件关系落库 → 签收完成`；问题件与补材是可多次往复的分支，撤销是终态分支。承保系统与审核系统是签收服务的外部依赖，其内部处理不属于平台范围；案件业务事实由内部案件系统拥有，签收侧只维护 `Sign → ClaimCase` 关系，案件号随审核签收结果返回。

一次理赔申请签收在领域内统一称为 `Sign`，其平台标识统一称为 `SignNo`。平台不使用"受理（Intake）"作为**领域对象**词汇；类型、字段、表和契约命名统一使用 `Sign`。状态与事件中的"受理（ACCEPT）"只描述签收落库后的处理阶段，不构成领域对象命名。

## 2. 核心命名

下表是平台内跨文档、跨代码、跨契约的词汇权威，中文与英文命名一一对应，不得并存同义替代词：

| 中文 | 命名 | 归属 |
| --- | --- | --- |
| 理赔平台 | Claim Platform | 项目 |
| 前端项目 | claim-platform-frontend | 前端 |
| 后端项目 | claim-platform-backend | 后端 |
| 网关 | claim-gateway | Edge & Platform |
| 签收服务 | claim-sign-service | Sign |
| 案件推送服务 | claim-case-push-service | Push |
| 签收 | Sign | Sign |
| 签收号 | SignNo | Sign |
| 签收状态 | SignStatus | Sign |
| 材料 | Material | Sign |
| 问题件 | ClaimIssue | Sign |
| 问题件回复 | IssueReply | Sign（MVP 不单独建模，见 §7） |
| Sign 与案件关系 | CaseRelation | Sign |
| 跨系统交互日志 | InteractionLog | Sign |
| 交互方向 | IN / OUT | Sign |
| 案件 | ClaimCase | 内部案件系统（Sign 只维护 `CaseRelation`） |
| 租户上下文 | TenantContext | Edge & Platform |
| 租户适配层 | Tenant Adapter + Pipeline（固定角色名：`Parser`、`Validator`、`Converter`） | Sign |
| 保司推送适配器 | InsurerPushAdapter | Push |
| 推送任务 | PushTask | Push |
| 推送记录 | PushRecord | Push |
| 推送回执 | PushReceipt | Push |
| 推送策略 | PushStrategy | Push |

代码内已固化、跨契约使用的补充命名：`tenantCode`（租户编码）、`externalApplyNo`（租户侧申请号，与 `tenantCode` 组成签收幂等键）、`issueNo`（平台问题件定位号，在 `tenantCode + signNo` 范围内唯一）、`protocolVersion`（租户协议版本）、`traceId`（链路标识）。

命名禁用规则：

- 理赔申请的核心领域统一使用 `Sign`，不使用 `Intake` 或 `Application` 作为类型命名。`Claim` 前缀只用于 `ClaimIssue`、`ClaimCase` 以及撤销的状态 `CLAIM_REVOKED` 与事件 `CLAIM_REVOKE`，不得用来表示签收本身（不存在 `ClaimApplication`、`ClaimIntake`）。
- `application` 一词只表示 DDD 应用层，不得用作业务概念（不存在 `Application` 领域对象、`applicationNo` 业务号）。
- 推送业务统一使用 `Push`，不引入 `Delivery`、`Send`、`Transmit` 等同义命名。
- 撤销统一使用 `Revoke`（状态 `CLAIM_REVOKED`、领域事件 `CLAIM_REVOKE`、交互 `SIGN_REVOKE`、`Sign` 聚合行为 `sign.revoke(...)`），不再使用 `Withdraw`、`withdrawal`、`CASE_REVOKE`。
- 签收生命周期只用单一 `SignStatus`，取值见签收服务架构基准 §2；不再维护独立的阶段字段 `SignStage`，环节语义由状态本身表达。
- 异步执行不建任务模型，因此不存在 `SignTask`、`taskNo`、`TaskStatus` 等命名；执行审计一律走 `InteractionLog`。
- 跨系统交互只使用 §10 登记的 `InteractionType` 常量，不引入同义动作名（如 `POLICY_QUERY`、`AUDIT_PUSH`）。

## 3. 技术约束

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Alibaba、MySQL 8、Redis 7、RocketMQ、XXL-Job。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios。
- 可观测：OpenTelemetry、Prometheus、Grafana、Loki。
- 容器：Docker、Docker Compose；本地中间件编排与建表脚本由交付相应能力的 Spec 一并提供。
- 持久化：所有持久化服务统一使用 MyBatis-Plus（版本以根 `pom.xml` 为准）；不使用 JPA。
- 关系型表不建立物理外键；聚合间关联通过稳定业务标识与必要索引维护，写入时由应用层校验关联事实，删除与状态变更不得依赖数据库级级联。
- 对象映射：MapStruct（版本以根 `pom.xml` 为准），只做对象转换，不承载业务规则与状态流转。
- 签收服务的生产存储与中间件固定为 MySQL（业务事实）、Redis（发号、缓存、锁）、RocketMQ（异步事件）、XXL-Job（状态补偿与对账）、Nacos（网关配置）、OSS（材料原文与影像）；不引入其他生产存储。Docker Compose 中的 OSS 兼容实现仅用于本地开发，可替换且不得被表述为生产 OSS 选型。材料原文不落库，库内只保留材料信息与地址引用。
- 所有服务自有库的建表与变更统一使用 Flyway 版本化 SQL：脚本置于所属服务的 `src/main/resources/db/migration`，随该服务发布执行；MyBatis-Plus 仍是唯一的业务数据访问框架。不得以手工建表、启动时临时 DDL 或其他迁移框架替代。
- 版本以根 `pom.xml` 与各模块 `package.json` 为准，不得自行升级 JDK、Spring Boot 或核心依赖。
- 交付必须通过 `docs/engineering/workflow.md` 第 6 节规定的验证门禁。

## 4. 工程结构与服务边界

后端只有三个可执行服务：网关、签收服务、案件推送服务。**不增加新的微服务，也不建立独立查询服务**。下列结构是目标形态。

```text
claim-platform
├─ claim-platform-frontend          # 统一前端：sign / material / issue / case / push / tenant
└─ claim-platform-backend
   ├─ claim-gateway                 # 租户接入边缘层：tenant / auth / route / limiter / trace
   ├─ claim-sign-service            # 理赔签收：sign / material / issue / internal / acl
   ├─ claim-case-push-service       # 案件推送：task / push / receipt / strategy
   └─ claim-common                  # 无领域事实的技术共享模块，不计入服务结构
```

```mermaid
flowchart LR
    FE[claim-platform-frontend\nVue 3 管理端] --> GW[claim-gateway]
    GW --> SIGN[claim-sign-service\n理赔签收]
    GW --> PUSH[claim-case-push-service\n案件推送]
    SIGN --> SIGNDB[(claim_sign_db)]
    PUSH --> PUSHDB[(push_db)]
    SIGN --> REDIS[(Redis)]
    SIGN --> MQ[RocketMQ]
    SIGN --> OSS[OSS]
    SIGN <--> POLICY[承保系统]
    SIGN <--> AUDIT[审核系统]
    AUDIT -->|案件生成| CASE[内部案件系统]
    CASE -->|案件生成事件| PUSH
```

- `claim-common` 只能包含无领域含义的技术能力和稳定共享契约（包括 `ApiResult<T>` 响应信封）；业务实体、Repository、领域规则和跨上下文共享表不得进入该模块。
- 三个子项目职责保持独立，不跨项目混合业务；一个服务的领域类型不得被另一个服务直接引用。
- 每个限界上下文独立拥有自己的数据库及其建表、变更脚本；禁止跨库 SQL、跨模块 Repository、共享业务实体和共享业务表。
- 跨上下文只能通过版本化同步 API、领域事件或显式反腐层通信。

### 签收域数据表

签收上下文独立拥有下列表，全部归 `claim-sign-service`，字段定义见签收服务架构基准 §3：

| 表 | 职责 |
| --- | --- |
| `t_claim_sign` | 签收主数据与状态、四类人员信息与扩展信息 |
| `t_claim_sign_material` | 材料信息、上传批次、来源与地址引用、下载处理标记 |
| `t_claim_sign_issue` | 问题件：平台问题件定位号、审核人、原因、是否已同步租户、是否需要补材 |
| `t_claim_sign_case_rel` | `Sign → ClaimCase` 关系（一个签收可关联多个案件） |
| `t_claim_sign_interaction_log` | 跨系统交互日志 |

自增 `id` 只作内部主键，不参与业务语义；业务查询与跨系统对账一律使用 `signNo`、`externalApplyNo`、`caseNo`、`batchNo`、`issueNo`。其中 `issueNo` 只在 `tenantCode + signNo` 范围内唯一定位一条问题件，不要求全局唯一。建表脚本直接按上表命名。

## 5. 限界上下文与职责

| 限界上下文 | 模块 | 拥有的领域事实 | 对外责任 | 禁止事项 |
| --- | --- | --- | --- | --- |
| 理赔签收（Sign） | `claim-sign-service` | 签收、材料、问题件、Sign-案件关系、交互日志 | 签收提交与查询、影像与材料处理、有效保单查询、自动承保、审核交付、问题件下发与回复、撤销、维护 `Sign → ClaimCase` 关系 | 案件审核、理算、核赔；持有 `ClaimCase` 业务事实；写入案件推送数据库 |
| 案件推送（Push） | `claim-case-push-service` | 推送任务、推送记录、推送回执、推送策略 | 接收案件生成事件并受控推送保司、回执处理、失败重试、状态同步 | 假设案件必须由签收服务创建；写入签收数据库 |
| Edge & Platform | `claim-gateway` | 无领域事实 | 租户识别、认证、路由、入口限流、Trace 透传 | 租户报文内容解析与协议适配；签收、问题件、案件推送等业务规则；持久化领域状态 |
| Platform Common | `claim-common` | 无领域事实 | 响应信封、租户上下文、Trace、通用错误等稳定技术契约 | 放入申请、案件、策略、审核等领域模型 |

网关与签收适配层分工（租户差异只由这两层承载，不得散落）：

- **网关只管身份、路由与流量**：认证、`tenantCode` 识别与上下文建立、路由、入口限流、Trace 透传。网关不解析租户报文内容，不承担租户协议适配。
- **租户协议与业务语义差异统一由签收服务 `interfaces/acl` 的 Tenant Adapter + Pipeline 承载**：验签与加解密、`Parser` 解析租户报文、`Validator` 参数与业务校验、`Converter` 标准模型转换与响应的签名加密。租户分支不得写成 `if (tenantCode)` 散落，也不得进入用例与领域模型。
- 每个下游服务必须再次校验调用者与租户范围，不能只依赖网关透传。
- 承保、审核系统的调用同样通过对应出站 Adapter 隔离，具体类型名与租户清单由租户接入 Spec 定义。
- 生产认证协议交付前，`local` profile 仅可由 Gateway 校验本地开发测试凭证并建立测试 `TenantContext`；凭证与租户映射必须来自本地受控配置，不得从请求体、路径或查询参数取得。该能力不得在非 `local` profile 启用，且不得使用真实生产凭证；下游服务仍须校验 Gateway 发出的调用者与租户范围。

## 6. 分层与依赖方向

领域服务统一采用四层，依赖只能向内，`domain` 不依赖其他层，也不依赖 MySQL、Redis、RocketMQ、Spring MVC、MyBatis、Feign 或 OSS SDK：

```mermaid
flowchart LR
    Interfaces[interfaces\nHTTP、消息、定时任务] --> Application[application\n用例、命令、查询、事务边界]
    Application --> Domain[domain\n聚合、值对象、领域服务、领域事件]
    Infrastructure[infrastructure\n持久化、缓存、消息、外部系统] --> Application
    Infrastructure --> Domain
```

```text
com.claim.platform.sign
├─ interfaces      sign（提交/查询/撤销）/ material / issue / internal（服务间入口）/ acl（租户适配：Tenant Adapter + Pipeline）/ advice（边界异常映射）
├─ application     command（命令与命令处理器）/ query（查询模型与查询服务）/ task（异步事件消费与状态推进编排）/ port（跨用例出站端口）/ result（用例结果模型）/ exception（用例层异常）
├─ domain          按聚合分包：sign / material / issue；聚合内组织 model、repository、service、exception
└─ infrastructure  persistence（dataobject / mapper / converter / repository）/ cache / mq / oss / insured / audit
```

`insured` 与 `audit` 分别承载承保系统与审核系统的调用；新增聚合（`CaseRelation`、`InteractionLog`）按同一条规则在 `domain` 下建自己的聚合包。定时补偿与技术配置类目录按实际需要落在 `infrastructure` 内，不预先建立空包。

端口与异常归属：

- 聚合自身的持久化端口放 `domain/<聚合>/repository`；跨用例的技术端口（发号与对账，以及架构基准采用的业务锁、幂等回放缓存）放 `application/port`。两者的实现都由 `infrastructure` 提供，依赖方向只允许 `infrastructure → application → domain`。
- 用例的返回值模型放 `application/result`；外部系统 DTO 按本节反腐层规则在 `acl` 或对应出站包内转换掉，不得穿透到该包。
- 异常按抛出层归属：领域异常放 `domain/<聚合>/exception`，用例编排抛出的异常放 `application/exception`。边界异常映射器放 `interfaces/advice`，只负责本服务异常到 HTTP 状态与上报值的映射。

Java 包根固定为 `com.claim.platform.sign` 与 `com.claim.platform.push`；架构蓝本中的 `com.xxx.claim.*` 只是占位写法。

用例组织规则（本项目已定，不再逐功能重新决策）：

- 写入用例以"命令 + 命令处理器"成对组织并放在 `application/command`：`SignCommand` → `SignCommandHandler`，`UploadMaterialCommand` → `MaterialCommandHandler`，`CreatePushTaskCommand` → `PushTaskCommandHandler`。
- 读取用例由 `application/query` 内的查询服务承载，按业务归属命名：`SignQueryService`、`MaterialQueryService`、`ClaimIssueQueryService`、`PushTaskQueryService`、`PushRecordQueryService`。**不引入 `GetSignQuery` / `GetSignQueryHandler` 这类读路径处理器**，也不建立独立查询服务或 query-service 模块。
- 异步事件的消费编排与状态推进放 `application/task`，不与写入、读取用例混在同一个类。
- 同一个应用层类不得同时承担写入与读取职责。
- 目录按实际需要创建，不预先建立空包；无实现的抽象层不落地。

聚合与领域行为：

- 签收域的模型固定为五个：`Sign`（业务标识 `tenantCode + externalApplyNo`，平台标识 `signNo`）、`Material`、`ClaimIssue`（平台标识 `issueNo`）、`CaseRelation`、`InteractionLog`；不在签收域新增或删除聚合，变更必须先修订本文件。
- 签收流程的业务动作固定为：影像与材料处理、有效保单查询、自动承保、审核交付、问题件下发与回复、撤销通知。不引入通用工作流引擎，也不建立独立异步任务模型。
- 聚合必须显式定义标识、一致性边界、不变量、合法状态转换、并发策略与幂等键；这些规则不分散到 Controller、Repository 或定时任务中。聚合之间通过领域事件最终一致协作。
- 撤销是 `Sign` 聚合的领域行为（`sign.revoke(...)`），不单独建立 Withdrawal 聚合；问题件建模为独立 `ClaimIssue`，MVP 不单独建立 `IssueReply`，回复内容与补充材料都记在 `ClaimIssue` 与材料关系内。
- 保司推送差异通过 `InsurerPushAdapter` 隔离，字段转换、请求组装、响应解析都收敛在对应 Adapter 与其工厂内；一个 `ClaimCase` 可以产生多个 `PushTask`。
- 外部系统协议、错误码与 DTO 必须在反腐层终止，不得成为领域对象。

## 7. 全局业务规则

以下规则长期有效，任何功能实现只能细化，不能绕过：

状态与流程：

- `Sign` 主模型只维护一个状态字段：`signStatus` ∈ `PENDING_ACCEPT` / `ACCEPT_SUCCESS` / `ACCEPT_FAILED` / `AUTO_UNDERWRITING` / `WAIT_AUDIT_ACCEPT` / `SIGN_COMPLETED` / `ISSUE_CASE` / `CLAIM_REVOKED`。`ACCEPT_FAILED`、`SIGN_COMPLETED`、`CLAIM_REVOKED` 为终态，进入条件与流转图见签收服务架构基准 §2。
- `Material`、`ClaimIssue` 各自维护处理标记，不回写进 `Sign` 主模型；`Sign` 的状态推进规则集中在 `Sign` 聚合内，只有审核系统的问题件事件可以使 `Sign` 进入 `ISSUE_CASE`。
- 失败分类固定：影像处理失败置 `ACCEPT_FAILED` 并产生可定位信号，不自动恢复；审核系统打回置 `ISSUE_CASE`，等待租户回复；自动承保未成功保持 `AUTO_UNDERWRITING`，由补偿按幂等条件重试或转人工。技术抖动只表现为事件重复投递，不产生额外的失败状态。
- 撤销只能由 `Sign` 聚合行为完成并置 `CLAIM_REVOKED`；进入 `CLAIM_REVOKED` 后，审核结果、问题件、承保与补材等迟到事实只记录交互并产生可定位信号，不得推进状态。

租户与数据隔离：

- `tenantCode` 必须来自受信任的凭证或上下文，不信任请求体、查询参数或路径中的租户字段。
- 每一条租户领域数据的查询和租户业务唯一约束都必须包含当前 `tenantCode`；不同租户的相同 `externalApplyNo` 相互隔离。
- 平台级全局业务编号（如 `SignNo`）可以使用全局唯一约束，并允许为发号读取跨租户聚合最大值；跨租户读取范围仅限发号所需的最大值，不得读取、返回或缓存租户业务明细。

异步事件与缓存边界：

- 签收异步链路固定为 `状态事实落库 → 领域事件 → RocketMQ → Consumer 状态校验 → 状态推进`；签收域不建立独立任务模型，`Sign` 状态本身即待执行意图。
- MQ 投递失败与流程中断由 XXL-Job 按当前状态执行恢复策略补偿，恢复表见签收服务架构基准 §4.3；补偿只重发事件或重试幂等外部调用，不直接改写业务状态。
- 签收域的跨系统调用一律按 §10 记录 `InteractionLog`，由它承担执行审计；不得为签收重试、超时或死信再建任务表或执行事件历史表。交互审计不得记录材料原文和个人信息。
- Consumer 必须支持重复消费，进入业务锁后先校验当前 `signStatus` 是否允许本次推进，不允许时安全结束并记录重复处理结果。
- 查询采用 Cache-Aside，MySQL 是业务状态的事实来源；缓存不得参与状态推进、并发判断与撤销判断。
- 有效保单查询、自动承保、审核交付、问题件通知等外部调用必须携带业务幂等键（`signNo`），重复调用不得产生重复业务副作用。问题件通知与回复另用 `issueNo` 定位到具体一条 `ClaimIssue`；`issueNo` 是定位标识，不是幂等键。

一致性与幂等：

- 签收提交、案件推送、回执处理和人工确认等写入必须可幂等重试，重复请求不得造成重复业务副作用。
- 签收的幂等键固定为 `tenantCode + externalApplyNo`，创建采用三层控制：事实库快查直接回放已存在签收 → `tenantCode + externalApplyNo` 业务锁串行化并发创建 → 数据库唯一键最终兜底。重复请求必须回放既有结果或返回可重试的并发错误；事实库是签收事实及其状态的唯一来源，缓存与锁不得作为终态依据。
- 唯一键冲突必须回查并发赢家并回放，回查不到时按编号碰撞处理，不得直接返回未知 500。
- 签收、材料、初始状态与交互日志必须在同一创建事务内提交或回滚；发号与外部调用不得置于该事务中。
- `SignNo` 必须全局唯一，并能从事实库恢复正确序列；数据库唯一键作最终兜底。
- 事件意图必须与状态事实原子落库：事务提交后才发布领域事件，发布失败由 XXL-Job 按状态补发；不得在数据库事务内直接调用外部系统或投递 MQ，也不得先发消息再写库。
- 消费者、补偿任务必须按业务幂等键与当前状态去重，并记录重复处理结果。
- 外部调用不得置于数据库事务中；调用失败必须转化为可查询的状态事实与交互日志，并按恢复策略重试或转人工。
- 终态不得被普通自动任务回退；人工补偿需要身份、原因、时间和前后状态的审计记录。
- 分布式业务锁只保护状态的读取、校验与更新，不得覆盖文件 IO、外部调用与事件投递；Redis 仅用于缓存、限流、短期锁或租约，不能作为签收与推送任务、回执终态的唯一事实来源。

敏感信息：

- 个人信息、材料内容、访问令牌、外部凭证不得写入日志、指标标签、Trace 属性；诊断输出必须先脱敏。
- 签收服务不建模人员明细的领域对象：申请人、出险人、主被保险人、领款人各以 `t_claim_sign` 的一个 JSON 列整体入库（`applicant_info`、`accident_person_info`、`main_insured_info`、`payee_info`），只有 `applicantName` 作为可检索列，其余扩展内容进 `ext_json`。人员明细不得进入日志与对外响应。
- 跨租户数据不得出现在错误信息中；对外响应不得暴露堆栈、SQL 或外部系统原文。

领域事件与契约：

- 领域事件必须携带 `eventId`、事件版本、`tenantCode`、聚合类型、聚合标识、发生时间和 `traceId`。
- 事件只追加兼容字段；破坏性变更必须发布新版本事件，不得静默修改旧事件含义；消费者必须把重复投递、乱序、失败重试和人工介入纳入设计。
- API 与事件只暴露调用方需要的稳定契约；对外契约（HTTP 路径、请求响应字段、错误码字符串、指标名、Redis 键结构、MQ Topic/Tag）的变更必须由专门的契约版本化 Spec 承接。

## 8. 对外契约基线

下列值是平台当前的对外与运维契约固定值，一律使用第 2 节的词汇，不设命名例外。变更必须先更新本节并经用户确认，再由契约版本化 Spec 承接。

| 对象 | 固定值 | 说明 |
| --- | --- | --- |
| HTTP 路径 | `/api/v1/signs` | 签收资源的根路径；材料、问题件、撤销作为子资源挂在其下 |
| 签收领域错误码 | `SignErrorCode` 六项常量名与上报值：`SIGN_NOT_FOUND`(404)、`INVALID_SIGN_STATUS`(409)、`SIGN_SUBMISSION_IN_PROGRESS`(409)、`SIGN_CONCURRENT_MODIFICATION`(409)、`SIGN_NO_UNAVAILABLE`(503)、`SIGN_NO_EXHAUSTED`(503) | 面向调用方的机器可读契约，常量名与上报值一体；状态模型重构只改内部状态，不改上报值 |
| 平台通用错误码 | 400 `REQUEST_VALIDATION_FAILED`、401 `UNAUTHORIZED`、500 `INTERNAL_ERROR` | 不进入领域错误码枚举，由 `interfaces/advice` 的边界异常映射器使用 |
| 签收领域事件 | `SIGN_ACCEPT`、`ISSUE_REPLY`、`CLAIM_REVOKE` | 事件载荷字段按 §7；Topic/Tag 尚未登记，确认后由契约版本化 Spec 承接 |
| 指标名 | `claim.sign.submission*`、`claim.sign.number.*` | 看板与告警按 `claim.<业务域>.*` 命名 |
| Redis 键前缀 | `claim:sign:submit:`、`claim:sign:replay:`、`claim:sign:lock:` | 提交幂等锁、幂等回放缓存与 `signNo` 业务锁的键前缀；第 7 节不强制缓存必须存在 |
| 数据库 | 库名 `claim_sign_db` | 表名与列名按第 4 节的数据表归属执行 |

领域异常与上报值同名映射：异常类名取上报值的驼峰形式加 `Exception`（`SIGN_NOT_FOUND` ↔ `SignNotFoundException`），映射表由边界异常映射器持有，不得散落到用例与领域模型中。

未列出的对外契约不得据此推断为可变更；需要变更时同样由契约版本化 Spec 承接。

## 9. 前端约束

`claim-platform-frontend/` 建立后必须遵守下列约束。

- 前端位于 `claim-platform-frontend/`，按业务能力组织为 `sign`、`material`、`issue`、`case`、`push`、`tenant` 六个功能域，而不是按后端服务名组织。
- 前端只负责页面与交互，不实现业务规则；统一通过 `claim-gateway` 暴露的版本化 API 访问后端，不直连领域服务、数据库、消息系统或外部保司系统。
- API 客户端统一处理认证、`traceId` 回传、稳定错误码与租户上下文，不得由单个页面自行拼接鉴权或租户逻辑。
- 前端不得保存外部系统明文凭证、材料原文或可跨租户复用的敏感缓存。
- 能力清单：签收查询、材料查看、问题件处理、案件查询、推送任务查询、推送记录查询、租户配置查看。

## 10. 可观测与全局需求

- 核心请求、事件消费、补偿和外部调用必须关联 `traceId` 与 `tenantCode`，并在已知时关联 `signNo`、`externalApplyNo`、`caseNo`、`issueNo`。
- Metrics、Logs、Traces 必须支持按服务、租户和签收状态定位；失败、重复投递、积压、补偿重发、限流和外部依赖错误必须产生可定位的信号。告警通道与规则尚未交付前，负责该链路的 Feature 至少输出含 `traceId`、`tenantCode`、业务标识、失败类别和恢复动作的结构化日志；跨系统场景同时记录 `InteractionLog`。不得把上述日志或审计记录表述为已配置的生产告警。
- 网关按租户、调用方和路由执行认证、限流与 Trace 透传；推送按租户、目标系统和协议适配器隔离并发与配额。
- 配置规则按环境和租户隔离，变更必须记录变更人、时间、生效范围和前后值摘要。
- 聚合查询发生局部依赖失败时，必须返回已获得数据、失败来源和 `traceId`，不得将局部失败伪装为空结果。
- Trace 透传、MDC 日志字段与指标暴露所需的依赖和配置，口径同第 3 节的建表脚本：由首个交付相应能力的 Spec 一并提供。未被任何 Spec 交付前，本节按服务已具备的能力执行，不得把"基线已写"当作"已具备"。

跨系统交互审计（`InteractionLog`）：

- 每次跨系统交互记录一条日志，字段固定为 `signNo`、`interactionType`、`direction`、`sourceSystem`、`targetSystem`、`requestId`、`traceId`、`result`、`errorCode`、`message`、`costMs`。
- `direction`：`IN` = 其他系统 → `claim-sign-service`，`OUT` = `claim-sign-service` → 其他系统；真实调用双方由 `sourceSystem + targetSystem` 表达。
- `InteractionType` 取值固定为 13 项，新增取值必须先修订本文件：`SIGN_SUBMIT`、`SIGN_QUERY`、`SIGN_REVOKE`、`MATERIAL_SUBMIT`、`MATERIAL_FETCH`、`INSURED_QUERY`、`AUTO_UNDERWRITING`、`AUDIT_SYNC`、`AUDIT_RESULT`、`AUDIT_ISSUE`、`ISSUE_NOTIFY`、`ISSUE_REPLY`、`CASE_COMPLETE`。
- 交互日志只记摘要与错误码，不记请求响应原文、材料内容与个人信息，§7 的脱敏红线同样适用。

## 11. 范围之外

- 案件审核、理算、核赔与赔付决策。
- 外部保司与核心业务系统的内部处理。
- Agent 信息收集、证据归纳与处置建议能力：目标架构未纳入，如需建设必须先修订本基线，并在"不新增微服务"约束下确定归属。
- 运营查询与异常诊断上下文：同上，在明确用例出现前不实现。
- `ClaimCase` 业务事实的创建与归属：案件业务事实由内部案件系统拥有，签收侧只维护 `Sign → ClaimCase` 关系（`CaseRelation`），案件号随审核签收结果返回，不落案件业务字段。
- 签收域的独立任务模型、通用工作流引擎与执行事件历史表：签收流程的状态补偿替代任务编排，如需重建必须先修订本文件。Push 上下文的 `PushTask`、`PushRecord`、`PushReceipt` 与 `PushStrategy` 是其独立领域事实，不受本项限制。
- 跨上下文共享数据库、共享业务表与共享业务实体。

## 12. 与功能 Spec 和架构基准的关系

- 签收服务的域内架构（分层、状态机、数据模型、时序、幂等与补偿）以 `docs/architecture/claim-sign-service.md` 为准，长期有效、不随功能交付删除；本文件只保留平台级公共约束并在必要处索引。
- 两份文档冲突时按平台级优先：命名词表、技术栈、服务边界、对外契约固定值、租户隔离与脱敏红线以本文件为准，域内实现细节以架构基准为准，冲突必须报告并修订其中一侧。
- 功能 Spec 只写本功能的增量需求与设计，公共内容引用上述两份文档而不复制。
- 功能实现只能细化本文件，不能绕过或降低其中的约束；与 §8 登记的对外契约冲突时，以本文件为准。
- 修改对外 API、领域事件、共享契约、数据库边界、租户模型或功能域划分时，必须先更新本文件并经用户确认，再动代码。
- 功能交付后其 Spec 直接删除，其中长期有效的结论（已定命名、对外契约、既定约束）必须回写本文件或签收服务架构基准的对应章节。
- 实现细节不进本文件，由架构基准与功能 Spec 设计：SQL DDL 与迁移脚本、`SignNo` 格式、Redis Key 的完整结构与恢复算法、状态机与恢复策略细则、MQ Topic 与 Tag 与 Consumer、Cache Key 与 TTL、租户 Adapter 落地、承保与审核接口、问题件规则、单元测试与集成测试与压测。第 7 节的业务不变式与第 8 节的对外契约值除外。
- 开发链路固定为：架构（本文件）→ 签收服务架构基准 → `requirements.md` → `design.md` → `tasks.md` → 实现。
- 规则冲突时的权威顺序：`docs/specs/baseline.md` → `docs/architecture/claim-sign-service.md`（签收域内）→ `docs/engineering/*.md` → 代码与测试。
