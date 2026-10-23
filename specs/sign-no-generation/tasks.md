# 签收号生成任务

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Draft（任务已按新方案拆解，阻塞于根需求修订评审） |
| 关联需求 | `specs/sign-no-generation/requirements.md` |
| 关联设计 | `specs/sign-no-generation/design.md` |
| 实现分支 | `feat/sign-no-generation`（根需求评审通过后创建） |

## 任务清单

| ID | 任务 | 依赖 | 需求/设计回链 | 状态 | 证据 |
| --- | --- | --- | --- | --- | --- |
| TK-SNG-001 | 根需求修订与项目级评审：允许平台级全局业务编号，并限定跨租户只读取发号聚合最大值 | 无 | FR-SNG-018、DS-SNG-013 | Pending | 评审记录、修订后的 `specs/requirements.md` |
| TK-SNG-002 | 数据库迁移：新增 `uk_sign_no`、更新初始化脚本、重复检查脚本、启动索引校验 | TK-SNG-001 | FR-SNG-008、FR-SNG-016、DS-SNG-009 | Pending | 迁移脚本、`SHOW INDEX` 结果、启动失败用例 |
| TK-SNG-003 | 领域 `SignNoFormat` 与 `Clock` 配置：`S` 格式、解析、日期范围、容量校验 | TK-SNG-001 | FR-SNG-001、FR-SNG-012、DS-SNG-002 | Pending | 单元测试 TEST-SNG-001/002 |
| TK-SNG-004 | 应用端口、结果与异常：`SignNoSequenceCounter`、`SignNoHighWaterMark`、`SignNoFallbackAllocator`、恢复锁、就绪协调器、503/耗尽异常 | TK-SNG-003 | FR-SNG-005、FR-SNG-009、FR-SNG-010、FR-SNG-017、DS-SNG-005/007 | Pending | 端口定义、异常映射测试 |
| TK-SNG-005 | Redis 计数器适配器：Key、TTL、三个 Lua 脚本、`-1/-2` 区分、非法值修复、异常转换 | TK-SNG-004 | FR-SNG-002、FR-SNG-004、FR-SNG-005、DS-SNG-004 | Pending | Lua 脚本、Redis 集成测试 |
| TK-SNG-006 | MySQL 高水位适配器：主库范围查询、软删除、历史 `SIGN` 排除、事务外读取、非法值处理 | TK-SNG-002、TK-SNG-004 | FR-SNG-005、FR-SNG-006、FR-SNG-013、FR-SNG-014、DS-SNG-005 | Pending | SQL、集成测试 TEST-SNG-020/022/023 |
| TK-SNG-007 | 恢复锁与校准服务：Redisson 锁、`reconcileAndAllocate`/`reconcile`、`max(Redis, MySQL)`、Lua 原子校准、失败分类 | TK-SNG-005、TK-SNG-006 | FR-SNG-005、FR-SNG-006、FR-SNG-011、DS-SNG-005 | Pending | 锁竞争测试、校准结果日志 |
| TK-SNG-008 | 就绪协调器与发号服务：实例级 `redisReady`、Redis/降级来源选择、冲突后恢复、失败降级 | TK-SNG-005、TK-SNG-007、TK-SNG-009 | FR-SNG-003、FR-SNG-009、FR-SNG-020、DS-SNG-003/007/008 | Pending | 单元测试 TEST-SNG-008/017 |
| TK-SNG-009 | MySQL 降级发号适配器：主库 `MAX + 1`、上限检查、不落库、失败分类 | TK-SNG-006 | FR-SNG-009、FR-SNG-010、FR-SNG-019、DS-SNG-007 | Pending | 单元测试 TEST-SNG-010/011/016 |
| TK-SNG-010 | 创建流程集成与重试编排：替换 `SignNoGenerator`、区分外部申请号/签收号冲突、默认 3 次、随机退避、失败不写库 | TK-SNG-008、TK-SNG-009 | FR-SNG-003、FR-SNG-007、FR-SNG-017、FR-SNG-019、DS-SNG-003/006 | Pending | 应用层单元测试、并发测试 TEST-SNG-019/021/025 |
| TK-SNG-011 | 错误码与入口映射：`INTAKE_NUMBER_UNAVAILABLE`、`INTAKE_NUMBER_EXHAUSTED`、`Retry-After`、500 分类 | TK-SNG-004、TK-SNG-010 | FR-SNG-009、FR-SNG-010、FR-SNG-017、DS-SNG-007 | Pending | 控制器测试、错误响应样例 |
| TK-SNG-012 | 后台校准调度器与异步人工触发：单线程、启动/周期校准、重叠跳过、就绪切换、失败隔离 | TK-SNG-007、TK-SNG-008 | FR-SNG-011、FR-SNG-020、DS-SNG-008 | Pending | 调度测试 TEST-SNG-015/030 |
| TK-SNG-013 | 配置属性：`claim.intake.number.*`、默认值、启动校验、配置文档 | TK-SNG-005、TK-SNG-007、TK-SNG-009、TK-SNG-012 | FR-SNG-016、DS-SNG-011 | Pending | `application.yml`、配置绑定测试 |
| TK-SNG-014 | 指标、日志与告警依据：发号/降级/恢复/冲突/不可用/耗尽/序列差值指标，敏感信息检查 | TK-SNG-008、TK-SNG-012 | FR-SNG-015、DS-SNG-012 | Pending | 指标测试、日志脱敏测试 |
| TK-SNG-015 | 单元测试：格式、回放、缺失/非法 Key、锁失败、冲突、降级、耗尽、配置、就绪切换、边界 | TK-SNG-003 至 TK-SNG-014 | TEST-SNG-001 至 TEST-SNG-018 | Pending | 测试报告 |
| TK-SNG-016 | 集成与并发测试：真实 MySQL/Redis、计数器删除/回退、软删除、历史编号、Redis 不可用降级、Redis 恢复校准、降级关闭、跨租户唯一、后台校准、在途碰撞 | TK-SNG-002 至 TK-SNG-015 | TEST-SNG-019 至 TEST-SNG-036 | Pending | 实际执行记录，不得以跳过代替通过 |
| TK-SNG-017 | 关联 Spec 回写：标注 `claim-intake-standard-flow` 中编号条款被取代 | 无 | FR-SNG-013、DS-SNG-013 | Done | 已在 `docs/sign-no-generation` 分支为 `specs/claim-intake-standard-flow/requirements.md`、`design.md` 和 `tasks.md` 添加取代说明 |
| TK-SNG-018 | 发布验证：`mvn verify`、迁移演练、降级演练、恢复演练、回滚演练、告警配置、已知限制确认 | TK-SNG-001 至 TK-SNG-017 | 全部 | Pending | 构建日志、迁移记录、降级/恢复记录、回滚记录 |

## 需求追踪矩阵

| 需求 | 设计 | 任务 | 测试 |
| --- | --- | --- | --- |
| FR-SNG-001 编号格式 | DS-SNG-002 | TK-SNG-003、TK-SNG-010 | TEST-SNG-001、TEST-SNG-032 |
| FR-SNG-002 全局按日发号 | DS-SNG-004 | TK-SNG-005、TK-SNG-008 | TEST-SNG-019 |
| FR-SNG-003 正常发号路径 | DS-SNG-003 | TK-SNG-008、TK-SNG-010 | TEST-SNG-003、TEST-SNG-019 |
| FR-SNG-004 计数器生命周期 | DS-SNG-004 | TK-SNG-005、TK-SNG-013 | TEST-SNG-033 |
| FR-SNG-005 计数器异常恢复 | DS-SNG-005 | TK-SNG-006、TK-SNG-007 | TEST-SNG-004、TEST-SNG-020 |
| FR-SNG-006 恢复原子性与并发 | DS-SNG-005 | TK-SNG-007 | TEST-SNG-005、TEST-SNG-021 |
| FR-SNG-007 唯一冲突恢复 | DS-SNG-006 | TK-SNG-010 | TEST-SNG-006、TEST-SNG-007、TEST-SNG-021 |
| FR-SNG-008 全局唯一约束 | DS-SNG-009 | TK-SNG-002 | TEST-SNG-029、TEST-SNG-031 |
| FR-SNG-009 Redis 不可用降级 | DS-SNG-007 | TK-SNG-008、TK-SNG-009 | TEST-SNG-008、TEST-SNG-024 |
| FR-SNG-010 序列耗尽 | DS-SNG-002、DS-SNG-007 | TK-SNG-003、TK-SNG-009 | TEST-SNG-011 |
| FR-SNG-011 Redis 恢复与后台校准 | DS-SNG-008 | TK-SNG-012 | TEST-SNG-015、TEST-SNG-030 |
| FR-SNG-012 业务日期与时区 | DS-SNG-002 | TK-SNG-003、TK-SNG-013 | TEST-SNG-002、TEST-SNG-034 |
| FR-SNG-013 兼容性 | DS-SNG-013 | TK-SNG-006、TK-SNG-017、TK-SNG-018 | TEST-SNG-023、TEST-SNG-032 |
| FR-SNG-014 租户隔离与安全 | DS-SNG-012 | TK-SNG-006、TK-SNG-014 | TEST-SNG-029 |
| FR-SNG-015 可观测 | DS-SNG-012 | TK-SNG-014 | TEST-SNG-035、指标/日志测试 |
| FR-SNG-016 配置与启动校验 | DS-SNG-009、DS-SNG-011 | TK-SNG-002、TK-SNG-013 | TEST-SNG-012、TEST-SNG-013 |
| FR-SNG-017 幂等与失败分类 | DS-SNG-003、DS-SNG-006、DS-SNG-007 | TK-SNG-010、TK-SNG-011 | TEST-SNG-003、TEST-SNG-007、TEST-SNG-010 |
| FR-SNG-018 根需求前置评审 | DS-SNG-013 | TK-SNG-001 | 评审记录 |
| FR-SNG-019 降级并发与重试上限 | DS-SNG-007、DS-SNG-018 | TK-SNG-009、TK-SNG-010 | TEST-SNG-016、TEST-SNG-025 |
| FR-SNG-020 降级与 Redis 恢复协调 | DS-SNG-008 | TK-SNG-008、TK-SNG-012 | TEST-SNG-017、TEST-SNG-026、TEST-SNG-036 |

## 测试证据要求

### 单元测试

| 测试 | 覆盖点 | 状态 | 证据 |
| --- | --- | --- | --- |
| TEST-SNG-001 | 编号格式、补零、长度、正则 | Pending | 测试类与断言 |
| TEST-SNG-002 | 每日重置、不同日期 Key | Pending | 测试类与断言 |
| TEST-SNG-003 | 回放不消耗流水号 | Pending | Mock 验证 |
| TEST-SNG-004 | 缺失/非法 Key 恢复 | Pending | 假计数器/高水位 |
| TEST-SNG-005 | 恢复锁超时/不可用 | Pending | 状态与降级断言 |
| TEST-SNG-006 | 外部申请号冲突回放 | Pending | 不触发恢复 |
| TEST-SNG-007 | 签收号冲突重试与耗尽 | Pending | 次数与 503 异常 |
| TEST-SNG-008 | Redis 不可用降级成功 | Pending | 降级端口被调用 |
| TEST-SNG-009 | 降级关闭 | Pending | 503、创建服务未调用 |
| TEST-SNG-010 | 两条路径均不可用 | Pending | 503、不写库 |
| TEST-SNG-011 | 流水号耗尽 | Pending | 两条路径异常 |
| TEST-SNG-012 | 配置校验 | Pending | 启动失败 |
| TEST-SNG-013 | 启动索引校验 | Pending | 缺少索引启动失败 |
| TEST-SNG-014 | 错误映射 | Pending | 503/500 与错误码 |
| TEST-SNG-015 | 后台校准重叠/失败隔离/就绪切换 | Pending | 调度测试 |
| TEST-SNG-016 | 降级重试与高水位变化 | Pending | 次数与结果 |
| TEST-SNG-017 | 部分 Redis 故障收敛 | Pending | 来源选择与冲突恢复 |
| TEST-SNG-018 | 分层边界 | Pending | 架构测试 |

### 集成测试

| 测试 | 覆盖点 | 状态 | 证据 |
| --- | --- | --- | --- |
| TEST-SNG-019 | 100 个并发/不同幂等键全部唯一 | Pending | 真实 Redis/MySQL 执行记录 |
| TEST-SNG-020 | 计数器删除/非法后从 MySQL 恢复 | Pending | `dbMax + 1`；高水位为 0 时非法值被修复 |
| TEST-SNG-021 | 计数器回退后冲突恢复 | Pending | 无 500，最终成功 |
| TEST-SNG-022 | 软删除行计入高水位 | Pending | 不复用编号 |
| TEST-SNG-023 | 历史 `SIGN` 编号忽略且可查询 | Pending | 高水位和 API 断言 |
| TEST-SNG-024 | Redis 不可用降级 | Pending | 200、编号唯一、降级指标 |
| TEST-SNG-025 | 降级并发 | Pending | 无重复、无 500、503 有界 |
| TEST-SNG-026 | Redis 恢复校准 | Pending | 下一编号大于 MySQL 最大值 |
| TEST-SNG-027 | 降级关闭 | Pending | 503 且无写入 |
| TEST-SNG-028 | 两条路径不可用 | Pending | 503 且无写入 |
| TEST-SNG-029 | 跨租户重复被唯一键拒绝 | Pending | 数据库异常、无 500 |
| TEST-SNG-030 | 后台校准恢复 Key 且不消耗流水号 | Pending | 校准前后值 |
| TEST-SNG-031 | 迁移重复检查中止 | Pending | 迁移失败记录 |
| TEST-SNG-032 | 两个入口端到端返回新格式 | Pending | API 响应 |
| TEST-SNG-033 | TTL 生效 | Pending | `PTTL` |
| TEST-SNG-034 | 时区/跨午夜 | Pending | 固定 `Clock` |
| TEST-SNG-035 | 序列差值指标 | Pending | 指标断言 |
| TEST-SNG-036 | 在途碰撞 | Pending | 无重复提交、最终成功或稳定 503 |

## 完成门禁

功能只能标记为 Done 当且仅当：

- TK-SNG-001 根需求修订评审通过，且 `specs/requirements.md` 已更新；
- TK-SNG-002 迁移在测试库和生产预演环境执行成功，`uk_sign_no` 校验通过；
- 所有 FR 均有对应实现和测试证据，`mvn verify` 通过；
- 集成测试在真实 MySQL/Redis 上实际执行，测试报告不得显示为跳过；
- 100 个不同幂等键并发 Redis 发号全部唯一，无 500；
- Redis 不可用时 100% 走降级或稳定 503，不出现直接失败；降级并发测试无重复编号、无 500；
- Redis 恢复后先校准再发号，下一编号大于 MySQL 最大值，不复用降级期间编号；
- 降级关闭、两条路径不可用、计数器删除/回退、软删除、历史 `SIGN` 编号、跨租户重复和在途碰撞场景均有证据；
- 后台校准在启动和周期场景生效，成功切换就绪状态，失败不影响请求处理；
- 指标、日志和告警项已配置，敏感信息检查通过；
- 旧 `SIGN` 编号查询和回放回归通过；
- 降级并发边界和 Sequence 演进触发条件已写入发布说明；
- 回滚演练通过，已知限制已写入发布说明。

## 已知阻塞与待确认

| 项 | 说明 | 处理 |
| --- | --- | --- |
| 根需求修订 | 全局唯一索引和跨租户最大序列查询与“唯一约束/查询含 tenantId”条款冲突 | TK-SNG-001，评审通过前不实现 |
| 全局业务量可见性 | 全局流水号会暴露平台整体签收量 | 发布评审显式接受；否则切换租户内备选 |
| 人工触发传输 | 本功能不新增 HTTP 入口，仅提供异步服务 | 后续运维平台或 Operations & Diagnosis 接入 |
| 8 位容量 | 每日上限 99,999,999 | 发布评审确认容量规划 |
| Redis 高可用 | 主从切换可能丢失/回退计数器 | 运行环境确认 Sentinel/Cluster 与持久化策略 |
| 降级并发上限 | `MAX + 重试` 不适合高并发 | 达到 FR-SNG-019 触发条件后新建 Sequence Spec |
| 本地数据卷 | 已有卷不会自动执行新 init 脚本 | 手工迁移或在确认数据可丢弃后重建 |
| 集成测试跳过 | 端口不可用时现有测试会静默跳过 | 发布证据必须显式记录实际执行，不接受跳过 |
| `S` 与 `SIGN` 共存 | 共享首字母 | 高水位查询必须同时限制日期范围、长度 17 和格式 |
