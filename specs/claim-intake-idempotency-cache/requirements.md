# 理赔申请幂等查询缓存需求

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Draft |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service` |
| 功能分支 | `feat/claim-intake-idempotency-cache` |
| 关联根需求 | `specs/requirements.md` |
| 关联 Spec | `specs/claim-intake-standard-flow/`、`specs/application-intake/` |
| 本次交付 | 幂等键 Redis 定位缓存、MySQL 降级、缓存回填、配置、指标与测试 |

## 项目背景与目标

签收提交的 `findByExternalApplyNo` 当前直接按 `tenantCode + externalApplyNo` 查询 MySQL。重复提交集中时会持续访问该业务唯一键。本功能采用 Cache-Aside 模式增加 Redis 优先、MySQL 回源的缓存链路，同时保持 MySQL 为唯一事实来源和最终幂等屏障。

Redis 仅缓存 `tenantCode + externalApplyNo -> signNo` 的定位关系。读取时优先从 Redis 获取 `signNo`，再按 `tenantCode + signNo` 查询 MySQL，以确保回放的是申请的当前状态，不会因材料上传等状态迁移而返回过期状态。缓存未命中、失效或异常时回源 MySQL，并在命中后按最佳努力回填缓存。

创建申请以 MySQL 事务结果为准。只有创建事务提交成功后才写入 Redis；Redis 不参与业务事务和最终幂等判断。

## 范围

- 将标准签收用例的 `findByExternalApplyNo` 改为 Redis 映射优先、MySQL 回源。
- 签收创建事务成功提交后写入 Redis 映射；MySQL 回源命中时回填映射。
- Redis 读、写或删除异常时降级 MySQL，不影响签收成功结果和数据库唯一键幂等。
- 缓存键摘要、TTL、配置校验、无敏感信息日志、指标与测试。

## 非范围

- 不缓存 `ClaimIntake` 聚合、申请人、人员、材料、扩展报文或其他个人信息。
- 不将 Redis 作为申请、状态或幂等的最终事实来源。
- 不变更 HTTP API、响应字段、数据库 Schema、幂等键、业务锁或状态机。
- 不新增跨上下文协作、领域事件、Outbox、MQ、缓存预热或布隆过滤器。

## 核心约束

1. 本功能采用 Cache-Aside 模式：读取优先缓存，缓存未命中、失效或异常时回源 MySQL，MySQL 命中后最佳努力回填缓存。
2. MySQL 是申请及其当前状态的唯一事实来源；`tenantCode + externalApplyNo` 唯一键仍是最终幂等屏障。
3. Redis 只保存 `tenantCode + externalApplyNo -> signNo` 映射；键和值均不得携带人员信息、材料内容或原始报文。
4. 缓存键必须包含租户且使用 SHA-256 摘要，不得暴露原始 `externalApplyNo`。
5. Redis 不可用、缓存值损坏或缓存映射失效时，必须回源 MySQL；缓存异常不得转换为业务失败。
6. 缓存命中后必须按 `tenantCode + signNo` 读取 MySQL 聚合并返回当前状态；不得直接用缓存拼装 `ClaimIntakeResult`。
7. 仅在创建事务成功提交后回填签收成功映射；创建事务回滚或失败时不得写缓存；Redis 不得参与业务事务和最终幂等判断。

## 验收需求

### FR-CIIC-001 Redis 优先定位

WHEN 标准签收用例执行 `findByExternalApplyNo` THEN THE SYSTEM SHALL 先使用 `tenantCode + externalApplyNo` 查询 Redis 签收号映射；命中后 SHALL 按 `tenantCode + signNo` 查询 MySQL 聚合并返回其当前状态，不得获取业务锁或执行按外部申请号的 MySQL 查询。

### FR-CIIC-002 MySQL 回源与回填

WHEN Redis 映射未命中、值损坏或按缓存签收号未查询到同租户聚合 THEN THE SYSTEM SHALL 按 `tenantCode + externalApplyNo` 查询 MySQL；查询命中时 SHALL 以最佳努力回填 Redis 映射，查询未命中时 SHALL 保持既有业务锁和唯一键流程。

### FR-CIIC-003 Redis 故障降级

WHEN Redis 连接、命令、读写或删除发生异常 THEN THE SYSTEM SHALL 记录无敏感信息的降级日志和指标，并继续按 `tenantCode + externalApplyNo` 查询 MySQL；SHALL NOT 因缓存故障返回业务错误、跳过 MySQL 唯一键或写入半成品申请。

### FR-CIIC-004 签收成功回填

WHEN 签收创建事务成功提交 THEN THE SYSTEM SHALL 将当前 `tenantCode + externalApplyNo -> signNo` 映射以配置 TTL 写入 Redis；WHEN 创建事务失败或回滚 THEN THE SYSTEM SHALL NOT 写入该映射；缓存写入失败 SHALL NOT 影响已提交的签收结果。

### FR-CIIC-005 租户隔离与安全

WHEN 构造缓存键、查询缓存或回源 MySQL THEN THE SYSTEM SHALL 始终包含 `tenantCode`；不同租户的相同 `externalApplyNo` SHALL 相互隔离；Redis 键、日志、指标和错误响应 SHALL NOT 包含原始 `externalApplyNo`、人员信息、材料内容、令牌或完整报文。

### FR-CIIC-006 配置与可观测性

WHEN 服务启动 THEN THE SYSTEM SHALL 绑定并校验 `claim.intake.submission.replay-cache.key-prefix` 与 `ttl`；非法空前缀或非正 TTL SHALL 阻止启动；WHEN 缓存读、写、删除或回源发生 THEN THE SYSTEM SHALL 记录稳定结果指标，且不得把租户、签收号、外部申请号或日期作为指标标签。

## 失败行为与兼容性

| 场景 | 行为 | 对外结果 |
| --- | --- | --- |
| Redis 命中且 MySQL 按签收号命中 | 返回 MySQL 当前状态 | 既有 200 回放 |
| Redis 未命中 | 按外部申请号回源 MySQL，命中则回填 | 既有行为 |
| Redis 读写异常 | 记录降级，按外部申请号回源 MySQL | 既有行为 |
| 缓存签收号无对应聚合 | 删除失效映射并按外部申请号回源 | 既有行为 |
| 创建事务失败 | 不写缓存 | 原有错误或回放语义 |

- 幂等键继续为 `tenantCode + externalApplyNo`，Redis 映射不构成新的业务幂等键。
- `ClaimIntakeResult`、HTTP 路径和数据库 Schema 保持兼容。
- 缓存过期或丢失只造成 MySQL 回源，不造成数据丢失或重复签收。
