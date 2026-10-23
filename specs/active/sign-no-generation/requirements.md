# 签收号生成需求

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Ready for Release（2026-10-23 根需求修订评审通过，实现与本地验证完成，待发布） |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service` |
| 功能分支 | 实现前创建 `feat/sign-no-generation`；本 Spec 在 `docs/sign-no-generation` |
| 关联根需求 | `specs/requirements.md` |
| 关联 Spec | `specs/active/claim-intake-standard-flow/`、`specs/archive/application-intake/` |
| 本次交付 | 全局按日签收号、Redis INCR 主发号、MySQL 降级发号、Redis 恢复校准、MySQL 全局唯一、后台校准、失败映射、数据库迁移、配置、指标与测试 |

## 背景与目标

现有 `SignNoGenerator` 生成 `SIGN` 前缀加 20 位随机十六进制字符，编号不可读、不按日期组织，也无法仅凭编号判断业务日期。本功能把平台签收号改为：

```text
S + yyyyMMdd + 8 位流水号
S2026102300000125
```

目标：

- 全局唯一，所有服务实例和所有租户共用一个日序列；
- 正常路径只依赖一次 Redis 原子自增，性能稳定；
- Redis 完全不可用时降级到 MySQL 发号，不直接中断签收；
- Redis 恢复后先按 MySQL 已落库事实校准高水位，再恢复 Redis 发号；
- 按业务日期组织，便于人工识别和对账；
- 允许跳号，不要求严格连续；
- MySQL `uk_sign_no` 是最终唯一性保证，Redis 不能成为唯一事实来源。

## 范围

- 平台签收号格式、业务日期、流水号容量和溢出处理。
- Redis 按日计数器、TTL、原子发号脚本和失效恢复。
- Redis 完全不可用时的 MySQL 降级发号、有限冲突重试和失败分类。
- 计数器缺失、计数器非法、唯一键冲突和 Redis 恢复后的序列校准。
- 单线程后台校准任务和异步人工校准能力。
- 数据库全局唯一索引 `uk_sign_no(sign_no)` 及迁移脚本。
- 发号失败的错误码、HTTP 映射、指标、日志和测试。
- `SignNoGenerator` 到应用端口和基础设施适配器的替换。
- 独立 Sequence 行方案的演进边界说明，不在本功能内实现。

## 非范围

- 不把 `signNo` 重命名为 `intakeNo`；字段名、数据库列和 HTTP 响应结构保持不变。
- 不实现租户内独立流水号；该方案仅作为根需求评审不通过时的备选，见设计文档。
- 不要求流水号严格连续、不回收跳号、不对历史 `SIGN` 编号改号或回填。
- 不在本功能内新增独立 Sequence 表、MySQL 号段或分布式 ID 生成器。
- 不新增人工校准 HTTP 接口；人工触发通过应用服务提交到后台单线程执行器，触发传输由后续运维平台接入。
- 不新增数据库业务表、领域事件、Outbox、MQ 或外部系统调用。
- 不修改申请、材料、状态机、幂等键、既有 HTTP 路径和响应字段。
- 不负责 Redis 高可用、持久化、容量和部署配置；这些是运行环境责任。
- 不扩展 8 位流水号容量，不实现跨日借号。
- 不修改前端；当前前端未使用 `signNo` 字段。

## 所属限界上下文与参与方

| 参与方 | 责任 | 本功能是否实现 |
| --- | --- | --- |
| Claim Intake 应用层 | 编排发号、降级、恢复、冲突重试和创建事务边界 | 是 |
| Redis | 保存按日共享计数器、执行发号脚本、提供恢复锁 | 是，新增适配器 |
| MySQL | 保存申请事实、提供全局唯一约束和降级/恢复高水位 | 是，新增索引和查询 |
| 后台执行器 | 启动校准、周期校准、Redis 恢复探测、异步人工校准 | 是，新增单线程执行器 |
| 运维平台 | 调用异步校准服务、配置告警、决定是否切换租户内发号 | 否，外部责任 |

## 核心约束

1. 签收号全局唯一；Redis 负责正常发号和候选值生成，MySQL `uk_sign_no` 是最终唯一性保证。
2. 正常路径固定为“快速查询/回放 → 业务锁 → 锁内复查 → Redis INCR → 生成编号 → 创建事务”；回放路径不得消耗流水号。
3. 发号必须在创建事务之外完成，不得占用数据库连接或行锁；创建失败允许留下跳号。
4. 计数器缺失、非法或回退时必须先校准再发号，不得直接从 1 重新开始。
5. 恢复必须使用按业务日期隔离的恢复锁和原子 Lua 脚本；校准值只能取 `max(Redis 当前值, MySQL 最大值)`，不得回退计数器。
6. Redis 完全不可用时降级到 MySQL 发号；只有降级也失败或显式关闭降级时才返回 `INTAKE_NUMBER_UNAVAILABLE`。
7. MySQL 降级发号使用主库当日 `MAX(sign_no) + 1` 生成候选值，依赖 `uk_sign_no` 处理并发冲突，并执行有限重试；不得假定 `MAX` 查询提供并发串行能力。
8. Redis 恢复后必须先完成高水位校准，再允许该实例使用 Redis 发号；MySQL 降级与 Redis 恢复以 MySQL 高水位和 `uk_sign_no` 为共同序列边界，不得产生两个已提交的相同签收号。
9. MySQL 恢复/降级查询只读取 Claim Intake 自有表的聚合最大值；必须读取主库、包含软删除行、排除历史 `SIGN` 编号，并只解析固定格式的 8 位流水号。
10. 当日流水号超过 99,999,999 时停止发号并告警，不得截断、回绕或生成 9 位流水号。
11. 业务日期使用注入的 `Clock` 和配置时区（默认 `Asia/Shanghai`）；每次分配只计算一次。
12. 全局流水号会暴露平台整体业务量；该风险必须在发布评审中明确接受，且不得通过接口暴露计数器值、日总量或跨租户统计。
13. 历史 `SIGN` 前缀编号继续有效；新旧编号可以在同一张表、同一 API 中共存，恢复查询必须通过日期范围、固定长度和格式排除历史编号。
14. `signNo` 字段名、HTTP 路径、响应信封、数据库列和既有状态机不变。
15. 本功能新增全局唯一约束，与根需求“唯一约束必须包含或受 tenantId 约束”存在字面冲突；必须先完成根需求修订评审。
16. Redis 键、日志、指标和错误响应不得包含人员信息、证件号、手机号、银行卡号、令牌、完整报文或外部申请编号。
17. 降级期间并发持续升高、`MAX` 查询成本增加或重试耗尽频繁发生时，必须通过新 Spec 升级为独立 Sequence 行；不得在本功能内无限扩大重试。
18. Redis 只保存按日计数器、实例就绪信号和短期锁，不保存申请、材料或状态；Redis 丢失不影响最终事实。

## 验收需求

### FR-SNG-001 编号格式

WHEN 系统为新签收申请分配平台签收号 THEN THE SYSTEM SHALL 生成 `S` + 业务日期 `yyyyMMdd` + 8 位左补零十进制流水号，长度固定 17，业务日期使用配置时区（默认 `Asia/Shanghai`）；SHALL NOT 生成小写前缀、9 位流水号或历史 `SIGN` 格式的新编号。

### FR-SNG-002 全局按日发号

WHEN 任一服务实例需要分配签收号 AND Redis 可用 THEN THE SYSTEM SHALL 使用 Redis 中按业务日期共享的单一计数器执行原子自增；所有租户共享同一计数器；SHALL NOT 使用 JVM 本地计数器、租户独立计数器或数据库自增主键。

### FR-SNG-003 正常发号路径

WHEN 创建路径已通过快速查询、业务锁和锁内二次查询且未命中既有申请 THEN THE SYSTEM SHALL 在创建事务之前完成发号，并将编号传入创建事务；WHEN 任一幂等层命中 THEN THE SYSTEM SHALL 回放既有 `signNo`，不得消耗流水号。

### FR-SNG-004 计数器生命周期

WHEN 成功分配流水号 THEN THE SYSTEM SHALL 保证当日计数器存在且 TTL 不小于 2 天（默认 3 天）；计数器键 SHALL 只包含固定前缀和业务日期，不得包含租户、人员信息或外部申请编号。

### FR-SNG-005 计数器异常恢复

WHEN 分配时发现当日计数器不存在或值非法 THEN THE SYSTEM SHALL 先获取该业务日期的恢复锁，查询 MySQL 当日已使用的最大流水号，将计数器校准为 `max(Redis 当前值, MySQL 最大值)` 后再分配；SHALL NOT 在未校准时直接从 1 开始发号。计数器已回退但值仍合法时，由 FR-SNG-007 的唯一冲突恢复兜底。

### FR-SNG-006 恢复原子性与并发

WHEN 执行恢复 THEN THE SYSTEM SHALL 使用单个 Lua 脚本原子完成“读取当前值、仅在更大时写入、刷新 TTL、分配或返回”；SHALL NOT 将计数器写回更小值；WHEN 恢复锁未获取 THEN SHALL 在限定时间内重试分配或返回稳定失败，不得无界等待；锁 SHALL 在 `finally` 中只释放当前持有者。

### FR-SNG-007 唯一冲突恢复

WHEN 创建因签收号唯一键冲突失败 AND 按 `tenantCode + externalApplyNo` 重新查询未命中 THEN THE SYSTEM SHALL 记录编号冲突，按失败编号所属业务日期执行恢复，重新分配并重试创建，默认最多 3 次；WHEN 重新查询命中 THEN SHALL 按并发赢家回放，不执行恢复；重试次数 SHALL NOT 无限增长。

### FR-SNG-008 全局唯一约束

WHEN 数据库迁移完成 THEN THE SYSTEM SHALL 在 `t_claim_intake` 上具有全局唯一键 `uk_sign_no(sign_no)`，并保留 `uk_tenant_sign_no(tenant_code, sign_no)`；WHEN 迁移前存在跨租户重复 `sign_no` THEN 迁移 SHALL 中止并要求人工处理，不得自动改号。

### FR-SNG-009 Redis 不可用时 MySQL 降级

WHEN Redis 连接失败或命令失败 THEN THE SYSTEM SHALL 在 `fallback-enabled=true` 时切换到 MySQL 降级发号：读取主库当日最大流水号、生成下一个编号并尝试创建；SHALL NOT 因 Redis 单独不可用而直接返回失败；降级成功 SHALL 返回正常创建结果并记录降级指标；WHEN 降级被关闭或降级也不可用 THEN SHALL 返回 HTTP 503 和 `INTAKE_NUMBER_UNAVAILABLE`，不得写入申请、材料或成功日志。

### FR-SNG-010 序列耗尽

WHEN 当日流水号将超过 99,999,999 THEN THE SYSTEM SHALL 返回 HTTP 503 和错误码 `INTAKE_NUMBER_EXHAUSTED`，记录告警，并停止当日发号；SHALL NOT 截断、回绕、复用或生成 9 位流水号；Redis 路径和 MySQL 降级路径 SHALL 使用同一上限。

### FR-SNG-011 Redis 恢复与后台校准

WHEN 服务启动后以及每个配置周期 THEN THE SYSTEM SHALL 在单线程后台执行器中对当日和前一业务日期执行校准：在恢复锁内查询 MySQL 最大值并将 Redis 计数器校准为 `max(当前值, MySQL 最大值)`，不消耗流水号；WHEN Redis 从不可用恢复 THEN SHALL 仅在当日校准成功后允许该实例重新使用 Redis 发号；并发运行 SHALL 跳过重叠任务；单次失败 SHALL 只记录告警，不得影响请求处理；SHALL 提供异步校准应用服务供人工触发，本功能不新增 HTTP 入口。

### FR-SNG-012 业务日期与时区

WHEN 计算业务日期 THEN THE SYSTEM SHALL 使用注入的 `Clock` 和配置时区（默认 `Asia/Shanghai`），每次分配只计算一次；跨午夜或实例时钟偏差 MAY 产生相邻日期的编号，系统不保证跨日期严格递增；编号日期与数据库创建日期无需一致。

### FR-SNG-013 兼容性

WHEN 本功能上线 THEN THE SYSTEM SHALL 保持 `signNo` 字段名、HTTP 路径、`ApiResult` 响应信封、数据库列 `sign_no` 和既有状态机不变；历史 `SIGN` 前缀编号 SHALL 继续可查询、可回放；SHALL NOT 对历史数据改号或回填；新旧编号 SHALL 通过固定长度和日期范围区分，不得把历史 `SIGN` 编号计入新序列高水位。

### FR-SNG-014 租户隔离与安全

WHEN 执行恢复或降级查询 THEN THE SYSTEM SHALL 只读取 Claim Intake 自有表中的聚合最大值，不返回租户业务数据；所有申请查询仍 SHALL 带 `tenantCode`；Redis 键、日志、指标和错误响应 SHALL NOT 包含姓名、证件号、手机号、银行卡号、令牌、完整报文或外部申请编号。全局流水号会暴露平台整体业务量，该风险 SHALL 在发布评审中明确接受。

### FR-SNG-015 可观测

WHEN 发号、降级、恢复、冲突、不可用或耗尽发生 THEN THE SYSTEM SHALL 记录稳定指标和日志，携带 `tenantCode`（已知时）、`signNo`（已知时）、`traceId`、业务日期、发号来源和结果类别；恢复/校准日志 SHALL 包含校准前后值、MySQL 高水位和正序列差值；SHALL 为降级持续发生、序列差值持续为正、唯一冲突激增、不可用、耗尽和重试耗尽提供告警依据；SHALL NOT 把日期、租户或签收号作为指标标签。

### FR-SNG-016 配置与启动校验

WHEN 服务启动 THEN THE SYSTEM SHALL 绑定并校验 `claim.intake.number.*`；非法时区、TTL、重试次数、恢复锁等待时间或降级开关 SHALL 阻止启动；SHALL 校验 `t_claim_intake` 上存在 `NON_UNIQUE=0` 且首列为 `sign_no` 的 `uk_sign_no` 索引，缺失时 SHALL 阻止启动。

### FR-SNG-017 幂等与失败分类

WHEN 同一 `tenantCode + externalApplyNo` 重复提交 THEN THE SYSTEM SHALL 回放既有编号且不消耗流水号；WHEN 发号、降级或恢复失败 THEN SHALL 不产生半成品申请；WHEN 签收号冲突重试耗尽 THEN SHALL 返回 503 `INTAKE_NUMBER_UNAVAILABLE` 并告警，不得无限重试或改用其他业务键；WHEN 发生非唯一键数据库异常 THEN SHALL 按 500 `INTERNAL_ERROR` 处理，不得伪装成编号冲突。

### FR-SNG-018 根需求前置评审

WHEN 本功能进入实现 THEN `specs/requirements.md` 中“唯一约束必须包含或受 tenantId 约束”的条款 SHALL 先按本 Spec 的修订建议完成项目级评审；评审未通过 SHALL NOT 开始生产代码实现，并 SHALL 改用租户内发号方案重新修订本 Spec。

### FR-SNG-019 MySQL 降级并发与重试上限

WHEN 使用 MySQL 降级发号 THEN THE SYSTEM SHALL 以主库当日 `MAX(sign_no) + 1` 生成候选编号，依赖 `uk_sign_no` 处理并发冲突，并在冲突后重新查询最大值、有限重试（默认 3 次，允许 1..5 次）；SHALL NOT 假定 `MAX` 查询提供并发串行能力；WHEN 重试耗尽 THEN SHALL 返回 503 `INTAKE_NUMBER_UNAVAILABLE` 并告警；当降级并发持续升高、`MAX` 查询成本增加或重试耗尽频繁发生时，SHALL 通过新 Spec 升级为独立 Sequence 行，不在本功能内临时实现。

### FR-SNG-020 降级与 Redis 恢复的序列空间协调

WHEN Redis 从不可用恢复 THEN THE SYSTEM SHALL 在恢复使用 Redis 发号前获取按日期恢复锁、读取主库高水位并执行原子校准，使 Redis 计数器不小于 `max(Redis 当前值, MySQL 最大值)`；在实例完成当日校准前 SHALL 继续使用 MySQL 降级；MySQL 降级与 Redis 恢复 SHALL 以 MySQL 高水位和 `uk_sign_no` 为共同序列边界，不得产生两个已提交的相同签收号；部分实例未观察到故障时，SHALL 通过唯一冲突恢复和有限重试收敛，不得无界冲突。

## 失败行为与错误码

| 场景 | 内部异常 | 对外错误码 | HTTP 状态 | 调用方行为 |
| --- | --- | --- | --- | --- |
| Redis 不可用，MySQL 降级成功 | 无（降级） | `OK` | 200 | 使用返回的 `signNo`；平台关注降级告警 |
| Redis 不可用，降级被关闭 | `SignNoUnavailableException` | `INTAKE_NUMBER_UNAVAILABLE` | 503 | 使用相同幂等键退避重试；关注平台告警 |
| Redis 不可用且 MySQL 降级查询/写入不可用 | `SignNoUnavailableException` | `INTAKE_NUMBER_UNAVAILABLE` | 503 | 使用相同幂等键退避重试；携带 `traceId` 联系平台 |
| 降级或 Redis 路径冲突重试耗尽 | `SignNoUnavailableException` | `INTAKE_NUMBER_UNAVAILABLE` | 503 | 使用相同幂等键退避重试；关注平台告警 |
| MySQL 恢复查询失败或返回非法高水位 | `SignNoUnavailableException` | `INTAKE_NUMBER_UNAVAILABLE` | 503 | 使用相同幂等键退避重试；携带 `traceId` 联系平台 |
| 当日流水号耗尽 | `SignNoSequenceExhaustedException` | `INTAKE_NUMBER_EXHAUSTED` | 503 | 当日停止提交；携带 `traceId` 联系平台 |
| `tenantCode + externalApplyNo` 唯一冲突 | 无（回放） | `OK` | 200 | 使用返回的 `signNo` |
| 第一、二、三层命中既有申请 | 无（回放） | `OK` | 200 | 使用返回的 `signNo` |
| 非唯一键数据库异常 | 原始数据访问异常 | `INTERNAL_ERROR` | 500 | 使用相同幂等键重试；携带 `traceId` 联系平台 |
| 业务锁等待超时 | `IntakeSubmissionInProgressException` | `INTAKE_SUBMISSION_IN_PROGRESS` | 409 | 使用相同幂等键退避重试 |
| 配置非法或数据库缺少 `uk_sign_no` | 启动校验异常 | 无 | 不启动 | 修正配置或先执行迁移 |
| 迁移前存在重复 `sign_no` | 迁移失败 | 无 | 不迁移 | 人工确认并处理重复数据 |

`INTAKE_NUMBER_UNAVAILABLE` 表示平台暂时无法分配签收号，不代表申请已创建；调用方使用相同幂等键重试不会产生重复申请。`INTAKE_NUMBER_EXHAUSTED` 是容量耗尽，不是客户端错误。降级成功不是错误，调用方无需感知。

## 幂等与兼容性影响

- 幂等键保持 `tenantCode + externalApplyNo`；签收号不是幂等键。
- 回放不消耗流水号，不生成新编号，不写入材料或成功日志。
- 发号在创建事务之外；创建失败、回放、唯一冲突和进程崩溃都可能产生跳号，这是允许的。
- `signNo` 字段、HTTP 路径、`ApiResult`、`ClaimIntakeResult`、数据库列和状态机不变。
- 新编号 `S...` 与历史 `SIGN...` 编号共存；历史编号不迁移、不改号。
- 新增 `uk_sign_no(sign_no)` 全局唯一索引；`uk_tenant_sign_no` 保留用于租户范围查询和既有约束。
- 新增配置项 `claim.intake.number.*`，全部有安全默认值；不配置也可启动。
- 新增错误码 `INTAKE_NUMBER_UNAVAILABLE`、`INTAKE_NUMBER_EXHAUSTED`；既有错误码不变。
- 新增指标和日志；不得新增敏感字段。
- 部署顺序：先执行数据库迁移并确认 `uk_sign_no` 存在，再部署新版本；旧实例生成的 `SIGN...` 编号与新实例的 `S...` 编号互不冲突。
- 滚动发布期间新旧实例共享同一 Redis 计数器；新旧实例必须使用相同的 `key-prefix`，否则会出现两套序列，最终由 `uk_sign_no` 拒绝重复并触发恢复。
- 回滚：回退应用代码即可；新编号仍可查询和回放；`uk_sign_no` 可以保留，不影响旧代码。
- 本功能不发布领域事件，不需要 Outbox；Redis 只保存计数器、实例就绪信号和短期恢复锁。

## 与根需求的冲突与修订建议

现有根需求规定：

> 每一条领域数据、唯一约束、查询和事件都必须包含或受当前 `tenantId` 约束。

本功能需要全局唯一索引 `uk_sign_no(sign_no)`，并且降级/恢复需要跨租户读取当日 `MAX(sign_no)`，与上述条款存在字面冲突。建议在项目级评审中把条款细化为：

> 每一条租户领域数据的查询和租户业务唯一约束都必须包含或受当前 `tenantId` 约束。平台级全局业务编号可以使用全局唯一约束，但不得放宽任何租户数据访问、缓存、事件和错误信息的隔离要求；跨租户只允许读取发号所需的聚合最大值，不得读取或返回租户业务明细。

在修订评审通过前，本功能只能保持 Draft。若评审不通过，应改用租户内发号：Redis 键和恢复锁包含 `tenantCode`，MySQL 保留 `uk_tenant_sign_no`，降级和恢复按租户和日期查询；该备选设计见 `design.md` 的 DS-SNG-013。

## 假设与待确认事项

### A-1 根需求修订

全局唯一索引和跨租户聚合最大值读取必须先完成根需求修订和项目级评审。没有评审结论，不开始生产代码实现。

### A-2 全局业务量可见性

全局连续流水号会让租户根据自己获得的编号推断平台整体签收量。已确认接受该风险；接口不得返回计数器值、日总量或跨租户统计。若业务认为该信息敏感，应切换到租户内发号方案。

### A-3 人工校准入口

按当前决策，人工触发不新增同步 HTTP 接口，而是提交到单线程后台执行器异步拉取 MySQL 最大值；触发传输由后续运维平台或 `Operations & Diagnosis` 上下文接入。当前运维可通过重启实例触发启动校准。

### A-4 流水号容量

8 位流水号每天最多 99,999,999 个。当前签收量远低于该上限；超过时按 FR-SNG-010 失败并告警，不自动扩展位数。容量规划需要在发布评审中确认。

### A-5 Redis 可用性与降级

Redis 是正常发号路径的关键依赖，但不是签收事实来源。Redis 完全不可用时按 FR-SNG-009 降级到 MySQL；Redis 主从切换、故障转移或数据丢失后，计数器可能缺失、非法或回退：缺失/非法由 FR-SNG-005 恢复，回退由 FR-SNG-007 的唯一冲突恢复兜底。Redis 的持久化、Sentinel/Cluster 和高可用策略由运行环境负责，不在本 Spec 实现。

### A-6 在途分配与恢复碰撞

Redis 数据丢失或降级切换时，MySQL 最大值不包含尚未提交的在途分配。恢复后可能重新发出与在途编号相同的候选值，由 `uk_sign_no` 拒绝并触发再次恢复和重试。重试次数有上限，持续冲突会告警，不能无限重试。

### A-7 数据库迁移与本地环境

当前项目没有 Flyway/Liquibase，数据库由 `docker/mysql/init/` 初始化。迁移需要同时更新初始化脚本和提供存量库迁移脚本；已有本地数据卷不会自动执行新初始化脚本，必须手工执行迁移或重建数据卷（重建前确认数据可丢弃）。生产环境必须在发布新版本前完成迁移。

### A-8 恢复查询读取主库

恢复和降级高水位必须读取主库，不能读取延迟副本；查询必须在创建事务之外执行，确保每次重试都能看到最新已提交的最大值。当前数据源为单库，未来引入读写分离时必须显式指定主库。

### A-9 时钟与 NTP

业务日期依赖实例时钟。所有实例必须启用 NTP；时钟偏差可能产生相邻日期或乱序编号，但不影响唯一性。时钟回拨超过计数器 TTL 时按缺失计数器恢复。

### A-10 `S` 前缀与历史 `SIGN` 编号

`S` 前缀与历史 `SIGN` 前缀共享首字母。新编号固定 17 字符，历史编号为 `SIGN` + 20 位十六进制（24 字符）；高水位查询必须同时使用日期范围 `[SyyyyMMdd, SyyyyMMdd+1)` 和 `CHAR_LENGTH(sign_no)=17`，不得只用 `LIKE 'S%'`。若后续编号体系扩张，应保证前缀空间不重叠，并先更新本 Spec。

### A-11 降级并发与 Sequence 演进

MySQL 降级发号依赖 `MAX + 1 + uk_sign_no + 有限重试`，不提供严格并发串行能力。当前单库、中低并发和 Redis 故障属于少量异常场景，可以接受有限冲突。出现以下任一情况时，应新建 Spec 升级为独立 Sequence 行：

- MySQL 分库分表，无法直接得到全局最大 `sign_no`；
- Redis 故障期间降级发号并发明显提高，重试耗尽频繁；
- 主表 `MAX(sign_no)` 查询成本明显增加；
- 多个业务开始共享统一发号能力。

### A-12 关闭降级

`fallback-enabled=false` 用于需要快速失败或隔离数据库压力的场景。关闭后 Redis 不可用将直接返回 503 `INTAKE_NUMBER_UNAVAILABLE`，不再尝试 MySQL 降级；该配置必须在发布评审中确认，不得作为常规运行配置。

### A-13 Redis 键前缀与滚动发布

计数器键默认 `sign:no:{yyyyMMdd}`，恢复锁默认 `sign:no:reconcile-lock:{yyyyMMdd}`。滚动发布期间所有实例必须使用相同 `key-prefix`；修改前缀会形成两套计数器，最终由 `uk_sign_no` 拒绝重复并触发恢复，但会增加冲突和告警。
