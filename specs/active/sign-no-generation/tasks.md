# 签收号生成任务

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Ready for Release（2026-10-23 本地实现与验证完成，待生产发布验证） |
| 关联需求 | `specs/active/sign-no-generation/requirements.md` |
| 关联设计 | `specs/active/sign-no-generation/design.md` |
| 实现分支 | `feat/sign-no-generation` |
| 验证环境 | 本地 MySQL 8.4 + Redis 7.4；`mvn -pl backend/claim-intake-service -am clean verify` |

## 任务清单

| ID | 任务 | 依赖 | 需求/设计回链 | 状态 | 证据 |
| --- | --- | --- | --- | --- | --- |
| TK-SNG-001 | 根需求修订与项目级评审：允许平台级全局业务编号，并限定跨租户只读取发号聚合最大值 | 无 | FR-SNG-018、DS-SNG-013 | Done | `specs/requirements.md` 第 84 行已细化条款，第 135 行记录 2026-10-23 项目级评审通过 |
| TK-SNG-002 | 数据库迁移：新增 `uk_sign_no`、更新初始化脚本、重复检查脚本、启动索引校验 | TK-SNG-001 | FR-SNG-008、FR-SNG-016、DS-SNG-009 | Done | 初始化脚本含 `uk_sign_no`；`docker/mysql/migration/20261023-add-uk-sign-no.sql`；本地库 `SHOW INDEX` 为 `Non_unique=0, Column_name=sign_no`；scratch 库演练见下方发布验证记录 |
| TK-SNG-003 | 领域 `SignNoFormat` 与 `Clock` 配置：`S` 格式、解析、日期范围、容量校验 | TK-SNG-001 | FR-SNG-001、FR-SNG-012、DS-SNG-002 | Done | `SignNoFormatTest` 6 项通过；`SignNoClockConfiguration` 使用配置时区 |
| TK-SNG-004 | 应用端口、结果与异常：`SignNoSequenceCounter`、`SignNoHighWaterMark`、`SignNoFallbackAllocator`、恢复锁、就绪协调器、503/耗尽异常 | TK-SNG-003 | FR-SNG-005、FR-SNG-009、FR-SNG-010、FR-SNG-017、DS-SNG-005/007 | Done | 端口与异常类已落地；`SignNoModeCoordinatorTest` 1 项、`SignNoExceptionHandlerTest` 4 项通过；新增应用层 `SignNoPolicy` 隔离配置 |
| TK-SNG-005 | Redis 计数器适配器：Key、TTL、三个 Lua 脚本、`-1/-2` 区分、非法值修复、异常转换 | TK-SNG-004 | FR-SNG-002、FR-SNG-004、FR-SNG-005、DS-SNG-004 | Done | `lua/sign-no/allocate.lua`、`reconcile-only.lua`、`reconcile-and-allocate.lua`；`RedisSignNoSequenceCounter`；TEST-SNG-020/021/030/033 真实 Redis 通过 |
| TK-SNG-006 | MySQL 高水位适配器：主库范围查询、软删除、历史 `SIGN` 排除、事务外读取、非法值处理 | TK-SNG-002、TK-SNG-004 | FR-SNG-005、FR-SNG-006、FR-SNG-013、FR-SNG-014、DS-SNG-005 | Done | `SignNoHighWaterMarkImplTest` 5 项通过；TEST-SNG-022/023 验证软删除计入、历史 `SIGN` 忽略且可查询 |
| TK-SNG-007 | 恢复锁与校准服务：Redisson 锁、`reconcileAndAllocate`/`reconcile`、`max(Redis, MySQL)`、Lua 原子校准、失败分类 | TK-SNG-005、TK-SNG-006 | FR-SNG-005、FR-SNG-006、FR-SNG-011、DS-SNG-005 | Done | `SignNoReconciliationServiceTest` 11 项通过；TEST-SNG-020/021/026/030 通过；三个 Lua 返回校准前后值 |
| TK-SNG-008 | 就绪协调器与发号服务：实例级 `redisReady`、Redis/降级来源选择、冲突后恢复、失败降级 | TK-SNG-005、TK-SNG-007、TK-SNG-009 | FR-SNG-003、FR-SNG-009、FR-SNG-020、DS-SNG-003/007/008 | Done | `SignNoAllocationServiceTest` 19 项通过；TEST-SNG-024/027/028 通过 |
| TK-SNG-009 | MySQL 降级发号适配器：主库 `MAX + 1`、上限检查、不落库、失败分类 | TK-SNG-006 | FR-SNG-009、FR-SNG-010、FR-SNG-019、DS-SNG-007 | Done | `MysqlSignNoFallbackAllocatorTest` 4 项通过；TEST-SNG-025 并发降级通过 |
| TK-SNG-010 | 创建流程集成与重试编排：替换 `SignNoGenerator`、区分外部申请号/签收号冲突、默认 3 次、随机退避、失败不写库 | TK-SNG-008、TK-SNG-009 | FR-SNG-003、FR-SNG-007、FR-SNG-017、FR-SNG-019、DS-SNG-003/006 | Done | `SignNoGenerator` 已删除；`ClaimIntakeCommandServiceTest` 13 项通过；TEST-SNG-019/021/025/036 通过；创建改为严格 `insert`，冲突可被唯一键暴露 |
| TK-SNG-011 | 错误码与入口映射：`INTAKE_NUMBER_UNAVAILABLE`、`INTAKE_NUMBER_EXHAUSTED`、`Retry-After`、500 分类 | TK-SNG-004、TK-SNG-010 | FR-SNG-009、FR-SNG-010、FR-SNG-017、DS-SNG-007 | Done | `SignNoExceptionHandlerTest` 4 项通过：503 + `Retry-After: 5`、耗尽无 `Retry-After`、非唯一键保持 500 |
| TK-SNG-012 | 后台校准调度器与异步人工触发：单线程、启动/周期校准、重叠跳过、就绪切换、失败隔离 | TK-SNG-007、TK-SNG-008 | FR-SNG-011、FR-SNG-020、DS-SNG-008 | Done | `SignNoReconciliationSchedulerTest` 8 项通过；TEST-SNG-030/035 通过 |
| TK-SNG-013 | 配置属性：`claim.intake.number.*`、默认值、启动校验、配置文档 | TK-SNG-005、TK-SNG-007、TK-SNG-009、TK-SNG-012 | FR-SNG-016、DS-SNG-011 | Done | `application.yml` 已加入配置；`SignNoPropertiesTest` 10 项通过 |
| TK-SNG-014 | 指标、日志与告警依据：发号/降级/恢复/冲突/不可用/耗尽/序列差值指标，敏感信息检查 | TK-SNG-008、TK-SNG-012 | FR-SNG-015、DS-SNG-012 | Done | `SignNoMetrics` 五个指标；TEST-SNG-035 校验 `sequence_gap{scope=current}`；`shouldNotLogSensitiveCommandValues` 通过 |
| TK-SNG-015 | 单元测试：格式、回放、缺失/非法 Key、锁失败、冲突、降级、耗尽、配置、就绪切换、边界 | TK-SNG-003 至 TK-SNG-014 | TEST-SNG-001 至 TEST-SNG-018 | Done | `mvn verify` 共 200 项，Failures 0、Errors 0、Skipped 0 |
| TK-SNG-016 | 集成与并发测试：真实 MySQL/Redis、计数器删除/回退、软删除、历史编号、Redis 不可用降级、Redis 恢复校准、降级关闭、跨租户唯一、后台校准、在途碰撞 | TK-SNG-002 至 TK-SNG-015 | TEST-SNG-019 至 TEST-SNG-036 | Done | `SignNoGenerationIntegrationTest` 17 项真实 MySQL/Redis 全部通过，无跳过；另含 `ClaimIntakeSubmissionIntegrationTest` 1 项通过 |
| TK-SNG-017 | 关联 Spec 回写：标注 `claim-intake-standard-flow` 中编号条款被取代 | 无 | FR-SNG-013、DS-SNG-013 | Done | 已在 `docs/sign-no-generation` 分支为 `specs/active/claim-intake-standard-flow/requirements.md`、`design.md` 和 `tasks.md` 添加取代说明 |
| TK-SNG-018 | 发布验证：`mvn verify`、迁移演练、降级演练、恢复演练、回滚演练、告警配置、已知限制确认 | TK-SNG-001 至 TK-SNG-017 | 全部 | Done | 本地验证全部通过，记录见“发布验证记录”；生产迁移预演、告警规则落地和发布说明仍待发布流程执行 |

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
| TEST-SNG-001 | 编号格式、补零、长度、正则 | Done | `SignNoFormatTest.shouldFormatDateAndLeftPaddedSequence`、`shouldRejectNullDateAndOutOfRangeSequence`、`shouldRecognizeOnlyCurrentFormat` |
| TEST-SNG-002 | 每日重置、不同日期 Key | Done | `SignNoFormatTest.shouldBuildHalfOpenDateRangeAcrossBoundaries`；`SignNoGenerationIntegrationTest.shouldIsolateCountersByBusinessDateAndZone` |
| TEST-SNG-003 | 回放不消耗流水号 | Done | `ClaimIntakeCommandServiceTest.shouldReplayQuickQueryWithoutAcquiringLock`、`shouldReplayLockedDoubleCheckWithoutCreating`、`shouldCreateIntakeWithAllocatedSignNo` |
| TEST-SNG-004 | 缺失/非法 Key 恢复 | Done | `SignNoAllocationServiceTest.shouldRecoverAndAllocateWhenCounterMissing/Invalid`；`SignNoReconciliationServiceTest.shouldReconcileAndAllocateInsideRecoveryLock`；TEST-SNG-020 |
| TEST-SNG-005 | 恢复锁超时/不可用 | Done | `SignNoAllocationServiceTest.shouldRetryCounterOnceAfterRecoveryLockTimeout`、`shouldFallbackWhenRecoveryLockTimeoutAndRetryFails`、`shouldMarkUnavailableAndFallbackWhenRedisFails`；`SignNoReconciliationServiceTest.shouldFailRecoveryAllocationWhenLockTimeout/Unavailable` |
| TEST-SNG-006 | 外部申请号冲突回放 | Done | `ClaimIntakeCommandServiceTest.shouldReplayConcurrentWinnerAfterIdempotencyKeyConflict` |
| TEST-SNG-007 | 签收号冲突重试与耗尽 | Done | `ClaimIntakeCommandServiceTest.shouldRecoverAndRetryWhenSignNoCollides`、`shouldFailAfterConfiguredAttemptsWithoutRecoveringOnLastConflict`；`SignNoExceptionHandlerTest.shouldMapUnavailableToServiceUnavailableWithRetryAfter` |
| TEST-SNG-008 | Redis 不可用降级成功 | Done | `SignNoAllocationServiceTest.shouldMarkUnavailableAndFallbackWhenRedisFails`、`shouldUseFallbackWhenInstanceNotReady` |
| TEST-SNG-009 | 降级关闭 | Done | `SignNoAllocationServiceTest.shouldRejectFallbackWhenDisabled`；TEST-SNG-027 |
| TEST-SNG-010 | 两条路径均不可用 | Done | `SignNoAllocationServiceTest.shouldPropagateMysqlSideRecoveryFailureWithoutFallback`；TEST-SNG-028 |
| TEST-SNG-011 | 流水号耗尽 | Done | `MysqlSignNoFallbackAllocatorTest.shouldRejectWhenHighWaterMarkReachesUpperBound`；`SignNoAllocationServiceTest.shouldPropagateExhaustedWithoutFallback`、`shouldPropagateExhaustedFromRecoveryWithoutFallback` |
| TEST-SNG-012 | 配置校验 | Done | `SignNoPropertiesTest` 10 项：默认值、覆盖值、空白前缀、缺少冒号、TTL、重试次数、锁等待、校准周期、初始延迟、校准天数 |
| TEST-SNG-013 | 启动索引校验 | Done | `SignNoSchemaValidatorTest` 3 项；缺少有效索引时 `afterPropertiesSet` 抛 `IllegalStateException` 阻止启动 |
| TEST-SNG-014 | 错误映射 | Done | `SignNoExceptionHandlerTest` 4 项：503/`Retry-After`、耗尽、traceId 兜底、非唯一键 500 |
| TEST-SNG-015 | 后台校准重叠/失败隔离/就绪切换 | Done | `SignNoReconciliationSchedulerTest` 8 项 |
| TEST-SNG-016 | 降级重试与高水位变化 | Done | `MysqlSignNoFallbackAllocatorTest` 4 项；`SignNoAllocationServiceTest` 降级分支；TEST-SNG-025 |
| TEST-SNG-017 | 部分 Redis 故障收敛 | Done | `SignNoAllocationServiceTest` 冲突后恢复 5 项（来源选择、成功/跳过/MySQL 侧失败/Redis 侧失败）；TEST-SNG-024/026/028 |
| TEST-SNG-018 | 分层边界 | Done | `SignNoArchitectureTest` 2 项 |

### 集成测试

| 测试 | 覆盖点 | 状态 | 证据 |
| --- | --- | --- | --- |
| TEST-SNG-019 | 100 个并发/不同幂等键全部唯一 | Done | `shouldAllocateUniqueSignNumbersForHundredConcurrentSubmissions`：100 并发全部成功、100 个唯一编号、100 行 |
| TEST-SNG-020 | 计数器删除/非法后从 MySQL 恢复 | Done | `shouldRecoverFromDeletedAndInvalidCounterUsingDatabaseHighWater`：删除后按 `dbMax+1` 恢复；高水位为 0 时非法值修复为 1 |
| TEST-SNG-021 | 计数器回退后冲突恢复 | Done | `shouldRecoverFromCounterRollbackAndSignNoCollision`：冲突后重试返回 `base+2`，conflict=1、recovery=1 |
| TEST-SNG-022 | 软删除行计入高水位 | Done | `shouldCountSoftDeletedRowsInHighWaterMark`：软删除 `base+9` 后下一号为 `base+10` |
| TEST-SNG-023 | 历史 `SIGN` 编号忽略且可查询 | Done | `shouldIgnoreHistoricalSignNumbersAndKeepThemQueryable`：高水位忽略 `SIGN...`；MockMvc GET 返回该记录 |
| TEST-SNG-024 | Redis 不可用降级 | Done | `shouldDegradeToFallbackWhenRedisUnavailable`：200、编号唯一、降级指标 +1、计数器未推进、就绪为 false |
| TEST-SNG-025 | 降级并发 | Done | `shouldHandleConcurrentFallbackWithBoundedFailures`：20 并发成功编号唯一；失败仅为 `SignNoUnavailableException` |
| TEST-SNG-026 | Redis 恢复校准 | Done | `shouldCalibrateRedisAndContinueAboveDatabaseMaxAfterRecovery`：DB max `base+4` → 校准后下一号 `base+5`，gap=3 |
| TEST-SNG-027 | 降级关闭 | Done | `shouldReturnUnavailableAndWriteNothingWhenFallbackDisabled`：503、零写入 |
| TEST-SNG-028 | 两条路径不可用 | Done | `shouldReturnUnavailableAndWriteNothingWhenBothPathsUnavailable`：503、零写入 |
| TEST-SNG-029 | 跨租户重复被唯一键拒绝 | Done | `shouldRejectCrossTenantDuplicateSignNoByGlobalUniqueIndex`：抛出 `DuplicateKeyException`，第二租户零行 |
| TEST-SNG-030 | 后台校准恢复 Key 且不消耗流水号 | Done | `shouldReconcileMissingCounterWithoutConsumingSequence`：重建为 `base+4`，下一号为 `base+5` |
| TEST-SNG-031 | 迁移重复检查中止 | Done | scratch 库演练：重复数据 `ERROR 1644 ... duplicate sign_no values exist` 且未创建索引；去重后添加成功；重复执行跳过；伪唯一索引中止 |
| TEST-SNG-032 | 两个入口端到端返回新格式 | Done | `shouldServeNewFormatFromBothHttpEntryPoints`：标准入口和租户入口均返回 `^S\d{16}$`，日期前缀正确且编号互异 |
| TEST-SNG-033 | TTL 生效 | Done | `shouldExpireCounterWithConfiguredTtl`：`PTTL` 位于 2–3 天 |
| TEST-SNG-034 | 时区/跨午夜 | Done | `shouldIsolateCountersByBusinessDateAndZone`：不同业务日期使用独立 Key 和序列；时区为 `Asia/Shanghai` |
| TEST-SNG-035 | 序列差值指标 | Done | `shouldRecordSequenceGapMetricDuringCalibration`：DB max `base+5`、counter `base+2`、gap=3，Gauge `claim.intake.number.sequence_gap{scope=current}=3.0` |
| TEST-SNG-036 | 在途碰撞 | Done | `shouldKeepSingleSignNoForConcurrentDuplicateSubmissions`：8 并发同幂等键只产生 1 个编号、1 行 |

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

## 发布验证记录（2026-10-23）

### 构建与测试

- 命令：`mvn -pl backend/claim-intake-service -am clean verify`
- 结果：`Tests run: 200, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
- 集成测试：`SignNoGenerationIntegrationTest` 17 项在真实 MySQL 8.4、Redis 7.4 上执行，0 跳过；`ClaimIntakeSubmissionIntegrationTest` 1 项通过
- 说明：一次全量运行曾出现 27 个上下文错误，定位为 VS Code Java 语言服务器向 `target/classes` 写入 ECJ 错误桩；在仓库副本中 clean build 后全部通过，非代码缺陷

### 迁移演练（scratch 库 `it_uk_sign_no_drill`）

1. 存在重复 `sign_no`：迁移中止，`ERROR 1644 (45000) ... duplicate sign_no values exist`，`uk_sign_no` 未创建
2. 去重后执行：`uk_sign_no added`；索引为 `Non_unique=0, Seq_in_index=1, Column_name=sign_no`
3. 重复执行：`uk_sign_no already exists, migration skipped`
4. 存在同名非唯一索引：中止，`uk_sign_no exists but is not a unique index on sign_no`
5. scratch 库已删除，未修改 `claim_intake_db`

### 降级、恢复与在途演练

- 降级：TEST-SNG-024/025/027/028 覆盖 Redis 不可用、降级并发、降级关闭、双路径不可用；无重复编号，失败稳定为 503
- 恢复：TEST-SNG-020/021/026/030/035 覆盖计数器删除/非法/回退、在途碰撞和后台校准；恢复后下一编号大于 MySQL 最大值且不复用降级编号
- 在途：TEST-SNG-021/036 由唯一键拒绝后重试或回放，无重复提交

### 回滚演练

- 使用改动前提交 `9a5907e` 在临时 worktree 运行 `RollbackCompatibilityDrillTest`
- scratch 库保留 `uk_sign_no` 并预置 `S2026102300000042`
- 旧版本成功读取该 `S` 格式记录，并创建旧格式 `SIGN7A94F77714B04AA58CEF`，随后可按编号查询
- 结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- 结论：回滚只需回退应用代码，`uk_sign_no` 可以保留；新旧编号继续共存

### 告警与已知限制

- 指标：`allocation{source,result}`、`fallback{result}`、`recovery{trigger,result}`、`conflict{type}`、`sequence_gap{scope}`
- 告警规则由运维平台按 DS-SNG-012 配置；仓库不包含 Prometheus 规则文件
- 生产迁移预演、生产告警规则、发布说明和容量确认属于发布流程，本地验证不替代

## 已知阻塞与待确认

| 项 | 说明 | 处理 |
| --- | --- | --- |
| 根需求修订 | 全局唯一索引和跨租户最大序列查询与“唯一约束/查询含 tenantId”条款冲突 | 已解决：2026-10-23 评审通过，`specs/requirements.md` 已修订 |
| 全局业务量可见性 | 全局流水号会暴露平台整体签收量 | 发布评审显式接受；否则切换租户内备选 |
| 人工触发传输 | 本功能不新增 HTTP 入口，仅提供异步服务 | 后续运维平台或 Operations & Diagnosis 接入 |
| 8 位容量 | 每日上限 99,999,999 | 发布评审确认容量规划 |
| Redis 高可用 | 主从切换可能丢失/回退计数器 | 运行环境确认 Sentinel/Cluster 与持久化策略 |
| 降级并发上限 | `MAX + 重试` 不适合高并发 | 达到 FR-SNG-019 触发条件后新建 Sequence Spec |
| 本地数据卷 | 已有卷不会自动执行新 init 脚本 | 本地库已手工确认索引；其他环境按迁移脚本执行 |
| 集成测试跳过 | 端口不可用时现有测试会静默跳过 | 本次已显式执行 17 项、0 跳过；发布环境仍需确认基础设施可用 |
| `S` 与 `SIGN` 共存 | 共享首字母 | 高水位查询同时限制日期范围、长度 17 和格式；TEST-SNG-023 已覆盖 |
| 生产发布动作 | 生产迁移预演、告警规则和发布说明未在本地执行 | 发布流程执行并回填证据 |
