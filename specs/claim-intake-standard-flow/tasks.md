# 理赔申请签收标准流程任务

## 状态说明

| 状态 | 含义 |
| --- | --- |
| Done | 已完成并有可核对证据 |
| In Progress | 部分完成，剩余工作已有明确依赖 |
| Planned | 已拆分，等待实现条件 |
| Blocked | 缺少外部契约、基础设施或前置决策，暂不能完成 |

当前进度：TK-CISF-002 至 TK-CISF-009、TK-CISF-011 已完成；Redisson watchdog 续期和代码规范复核已通过全量构建。Gateway 鉴权、租户识别、限流和协议版本透传仍未实现，TK-CISF-010 保持 Blocked，端到端验证缺口记录在已知限制中。

> 编号生成变更：本文件 TK-CISF-005、TK-CISF-007、TK-CISF-008 中关于 `SignNoGenerator`、`SIGN` 格式和单次编号重试的完成证据，仅记录当时实现；现行发号规则以 `specs/sign-no-generation/` 为准。

## 任务清单

| 任务 | 关联需求与设计 | 状态 | 依赖 | 完成证据或阻塞原因 |
| --- | --- | --- | --- | --- |
| TK-CISF-001 建立标准签收流程 Spec | FR-CISF-001 至 FR-CISF-018；DS-CISF-001 至 DS-CISF-012 | Done | 无 | 本目录 `requirements.md`、`design.md`、`tasks.md` 已建立 |
| TK-CISF-002 扩展标准命令与材料模型 | FR-CISF-005、FR-CISF-006、FR-CISF-007、FR-CISF-017；DS-CISF-002 | Done | TK-CISF-001 | `SubmitIntakeCommand` 新增不可变 `materials`；`ClaimIntakeController`、`ReferenceRequestConverter`、`SupportTenantRequestConverter` 传空列表；`SubmitIntakeCommandTest` 2 个测试通过 |
| TK-CISF-003 定义锁端口、并发异常与配置属性 | FR-CISF-010、FR-CISF-011、FR-CISF-012、FR-CISF-016；DS-CISF-003、DS-CISF-005、DS-CISF-006、DS-CISF-008 | Done | TK-CISF-001 | 新增 `IntakeSubmissionLock`、`IntakeSubmissionLockHandle`、`LockAcquireStatus`、`IntakeSubmissionInProgressException`、`IntakeSubmissionLockProperties`；`IntakeSubmissionLockPropertiesTest` 5 个测试通过 |
| TK-CISF-004 实现 Redisson 业务锁适配器 | FR-CISF-003、FR-CISF-010、FR-CISF-011、FR-CISF-012、FR-CISF-014、FR-CISF-016、FR-CISF-019；DS-CISF-003、DS-CISF-006、DS-CISF-007、DS-CISF-013 | Done | TK-CISF-003 | `RedissonIntakeSubmissionLock` 实现 SHA-256 租户范围锁键、有限等待、watchdog 获取方式、按持有者释放、Redis 异常降级和 `claim.intake.submission.lock` 指标；`RedissonIntakeSubmissionLockTest` 8 个测试通过 |
| TK-CISF-005 拆分提交编排与创建事务 | FR-CISF-001 至 FR-CISF-009、FR-CISF-013、FR-CISF-018；DS-CISF-003、DS-CISF-004、DS-CISF-008 | Done | TK-CISF-002、TK-CISF-003、TK-CISF-004 | `ClaimIntakeCommandService` 改为非事务编排；`ClaimIntakeCreationService` 使用 `REQUIRES_NEW`；实现锁内二次查询、`SignNoGenerator`、唯一键回查和一次编号重试；`ClaimIntakeCommandServiceTest` 11 个、`ClaimIntakeCreationServiceTest` 2 个测试通过 |
| TK-CISF-006 补充错误映射、日志与指标 | FR-CISF-015、FR-CISF-018；DS-CISF-005、DS-CISF-007 | Done | TK-CISF-003、TK-CISF-004、TK-CISF-005 | `ClaimIntakeExceptionHandler` 映射 409 `INTAKE_SUBMISSION_IN_PROGRESS`；新增 `claim.intake.submission`、`claim.intake.submission.conflict` 指标；日志仅含租户、`traceId`、`signNo` 和结果标签；`TenantIntakeControllerTest.shouldMapSubmissionInProgressConflict` 通过 |
| TK-CISF-007 补充单元测试 | TEST-CISF-001 至 TEST-CISF-013、TEST-CISF-015、TEST-CISF-016；DS-CISF-009 | Done | TK-CISF-002 至 TK-CISF-006 | 本 Spec 相关测试类共 34 个测试全部通过，覆盖编排、锁超时、降级、释放、材料状态、唯一键回查、编号重试、配置和脱敏 |
| TK-CISF-008 增加真实 MySQL/Redis 并发测试 | TEST-CISF-014；DS-CISF-009 | Done | TK-CISF-005、TK-CISF-006；MySQL/Redis 测试环境 | `ClaimIntakeSubmissionIntegrationTest` 使用本地 MySQL 和 Redis，6 个并发提交同一幂等键，1 个测试通过，断言单申请、单材料、单日志和相同 `signNo` |
| TK-CISF-009 回写证据并执行构建验证 | 全部需求；DS-CISF-009、DS-CISF-010 | Done | TK-CISF-007、TK-CISF-008 | 本文件证据已回写；`tenant-adapter-intake` A-1 已同步；`mvn -f backend/pom.xml verify` BUILD SUCCESS，`claim-intake-service` 104 个测试通过 |
| TK-CISF-010 Gateway 透传契约验证 | FR-CISF-001、FR-CISF-014、FR-CISF-017；DS-CISF-001 | Blocked | Gateway Spec 和实现 | 当前 `claim-gateway` 仅为启动骨架，尚未实现鉴权、租户识别、限流和协议版本头透传；本任务只验证契约，不扩大本 Spec 范围 |
| TK-CISF-011 增加 Redisson watchdog 续期 | FR-CISF-003、FR-CISF-012、FR-CISF-016、FR-CISF-019；DS-CISF-003、DS-CISF-006、DS-CISF-013 | Done | TK-CISF-004 | `lease-time` 经 `IntakeSubmissionLockConfiguration` 映射为 `lockWatchdogTimeout`；适配器改用 `tryLock(waitTime, TimeUnit)`；`IntakeSubmissionLockConfigurationTest` 2 个、`RedissonIntakeSubmissionLockTest.shouldAcquireWithWatchdogLease`、`RedissonIntakeSubmissionLockRenewalTest` 1 个真实 Redis 测试通过 |

## 实现前门禁

- [x] 已建立 `specs/claim-intake-standard-flow/` 三份 Spec。
- [x] 已确认 `SubmitIntakeCommand` 承载材料元数据（A-3），并接受 `reference`/既有入口传空列表。
- [x] 已确认材料非空时初始状态为 `IMAGE_UPLOADED`、材料为空时为 `IMAGE_NOT_UPLOAD`（A-4）。
- [x] 已确认 Redis 不可用时降级到唯一键路径（A-5）。
- [x] 已确认锁等待 3 秒、租期 10 秒、摘要锁键和配置前缀（A-6）。
- [x] 已创建功能分支 `feat/claim-intake-standard-flow`。
- [x] 已确认真实 MySQL/Redis 并发测试环境可用（A-8）。
- [x] Gateway 尚未实现，服务入口已具备可信头模拟和单元测试条件；端到端透传验证保留为已知限制（TK-CISF-010）。

## 追踪矩阵

| 需求 | 设计 | 任务 | 测试 |
| --- | --- | --- | --- |
| FR-CISF-001 | DS-CISF-001、DS-CISF-008 | TK-CISF-005 | TEST-CISF-002、TEST-CISF-014、TEST-CISF-016 |
| FR-CISF-002 | DS-CISF-003 | TK-CISF-005 | TEST-CISF-001 |
| FR-CISF-003 | DS-CISF-003 | TK-CISF-003、TK-CISF-004、TK-CISF-005 | TEST-CISF-009、TEST-CISF-010、TEST-CISF-014 |
| FR-CISF-004 | DS-CISF-003 | TK-CISF-005 | TEST-CISF-002 |
| FR-CISF-005 | DS-CISF-002、DS-CISF-003 | TK-CISF-005 | TEST-CISF-003、TEST-CISF-004、TEST-CISF-007 |
| FR-CISF-006 | DS-CISF-002、DS-CISF-004 | TK-CISF-002、TK-CISF-005 | TEST-CISF-003、TEST-CISF-004、TEST-CISF-014 |
| FR-CISF-007 | DS-CISF-002、DS-CISF-004 | TK-CISF-002、TK-CISF-005 | TEST-CISF-003、TEST-CISF-004 |
| FR-CISF-008 | DS-CISF-004 | TK-CISF-005 | TEST-CISF-006、TEST-CISF-014 |
| FR-CISF-009 | DS-CISF-004 | TK-CISF-005 | TEST-CISF-007 |
| FR-CISF-010 | DS-CISF-003、DS-CISF-005 | TK-CISF-003、TK-CISF-004、TK-CISF-005、TK-CISF-006 | TEST-CISF-008 |
| FR-CISF-011 | DS-CISF-003、DS-CISF-005 | TK-CISF-003、TK-CISF-004、TK-CISF-005、TK-CISF-006 | TEST-CISF-009 |
| FR-CISF-012 | DS-CISF-003 | TK-CISF-003、TK-CISF-004、TK-CISF-005 | TEST-CISF-010 |
| FR-CISF-013 | DS-CISF-003 | TK-CISF-005 | TEST-CISF-001、TEST-CISF-002、TEST-CISF-005、TEST-CISF-014 |
| FR-CISF-014 | DS-CISF-003、DS-CISF-007 | TK-CISF-004、TK-CISF-005 | TEST-CISF-011 |
| FR-CISF-015 | DS-CISF-007 | TK-CISF-006 | TEST-CISF-009、TEST-CISF-015 |
| FR-CISF-016 | DS-CISF-006 | TK-CISF-003、TK-CISF-004 | TEST-CISF-013 |
| FR-CISF-017 | DS-CISF-010 | TK-CISF-002、TK-CISF-009 | TEST-CISF-016 |
| FR-CISF-018 | DS-CISF-005 | TK-CISF-006 | TEST-CISF-006、TEST-CISF-007、TEST-CISF-008、TEST-CISF-012 |
| FR-CISF-019 | DS-CISF-003、DS-CISF-013 | TK-CISF-004、TK-CISF-011 | TEST-CISF-017 |

## 测试证据

| 测试 | 覆盖需求 | 状态 | 证据 |
| --- | --- | --- | --- |
| TEST-CISF-001 | FR-CISF-002、FR-CISF-013 | Pass | `ClaimIntakeCommandServiceTest.shouldReplayQuickQueryWithoutAcquiringLock`：回放且不获取锁、不写库 |
| TEST-CISF-002 | FR-CISF-001、FR-CISF-004、FR-CISF-013 | Pass | `ClaimIntakeCommandServiceTest.shouldReplayLockedDoubleCheckWithoutCreating`：锁内回放且不创建 |
| TEST-CISF-003 | FR-CISF-005、FR-CISF-006、FR-CISF-007 | Pass | `ClaimIntakeCreationServiceTest.shouldCreateIntakeAsImageNotUploadWhenMaterialsEmpty`：`IMAGE_NOT_UPLOAD` |
| TEST-CISF-004 | FR-CISF-005、FR-CISF-006、FR-CISF-007 | Pass | `ClaimIntakeCreationServiceTest.shouldCreateIntakeAsImageUploadedAndSaveMaterialsWhenMaterialsPresent`：`IMAGE_UPLOADED` 且材料和日志同事务写入 |
| TEST-CISF-005 | FR-CISF-013 | Pass | `ClaimIntakeCommandServiceTest.shouldReplayLockedDoubleCheckWithoutCreating`、`shouldReplayConcurrentWinnerAfterIdempotencyKeyConflict`；并发测试断言单材料、单日志 |
| TEST-CISF-006 | FR-CISF-008、FR-CISF-018 | Pass | `ClaimIntakeCommandServiceTest.shouldReplayConcurrentWinnerAfterIdempotencyKeyConflict`：唯一键冲突回查回放 |
| TEST-CISF-007 | FR-CISF-009、FR-CISF-018 | Pass | `ClaimIntakeCommandServiceTest.shouldRegenerateSignNoOnceWhenConflictIsNotIdempotencyKey`、`shouldFailAfterSecondSignNoCollision` |
| TEST-CISF-008 | FR-CISF-010、FR-CISF-018 | Pass | `ClaimIntakeCommandServiceTest.shouldThrowSubmissionInProgressWhenLockTimeout`；`TenantIntakeControllerTest.shouldMapSubmissionInProgressConflict` 验证 409 |
| TEST-CISF-009 | FR-CISF-003、FR-CISF-011、FR-CISF-015 | Pass | `ClaimIntakeCommandServiceTest.shouldDegradeWhenRedisUnavailable`：降级继续并记录 `lock_degraded` |
| TEST-CISF-010 | FR-CISF-003、FR-CISF-012 | Pass | `ClaimIntakeCommandServiceTest.shouldReleaseLockWhenCreationFails`；`RedissonIntakeSubmissionLockTest.shouldReleaseLockOnlyWhenHeldByCurrentThread`、`shouldNotForceUnlockWhenOwnershipLost` |
| TEST-CISF-011 | FR-CISF-014 | Pass | `RedissonIntakeSubmissionLockTest.shouldUseTenantScopedHashedKey`：租户参与摘要，相同外部编号不共享锁键 |
| TEST-CISF-012 | FR-CISF-018 | Pass | `ClaimIntakeCommandServiceTest.shouldPropagateNonUniqueIntegrityFailureWithoutReplay` |
| TEST-CISF-013 | FR-CISF-016 | Pass | `IntakeSubmissionLockPropertiesTest` 5 个测试：默认值、覆盖值、负等待、非正租期、空前缀 |
| TEST-CISF-014 | FR-CISF-001、FR-CISF-003、FR-CISF-006、FR-CISF-008、FR-CISF-013 | Pass | `ClaimIntakeSubmissionIntegrationTest.shouldCreateSingleIntakeWhenConcurrentSubmissionsShareIdempotencyKey`：真实 MySQL/Redis，6 并发，单申请、单材料、单日志、相同 `signNo`；本地 3306/6379 不可达时自动跳过 |
| TEST-CISF-015 | FR-CISF-015 | Pass | `ClaimIntakeCommandServiceTest.shouldNotLogSensitiveCommandValues`：日志不含证件号、手机号、材料路径；指标仅含 `result` 标签 |
| TEST-CISF-016 | FR-CISF-001、FR-CISF-017 | Pass | `ClaimIntakeControllerTest` 2 个、`TenantIntakeControllerTest` 10 个、`ReferenceTenantAdapterTest` 6 个、`DefaultIntakeConvertPipelineTest` 11 个测试；既有入口命令材料为空列表 |
| TEST-CISF-017 | FR-CISF-003、FR-CISF-016、FR-CISF-019 | Pass | `IntakeSubmissionLockConfigurationTest` 2 个测试验证 `lease-time`（含默认值）映射为 `lockWatchdogTimeout`；`RedissonIntakeSubmissionLockRenewalTest.shouldKeepMutexBeyondBaseLeaseAndAllowAcquireAfterRelease` 使用真实 Redis，watchdog 基础租期 1 秒，持有 2.5 秒后在 1.5 秒时另一线程获取得到 `TIMEOUT`，释放后可重新获取；本地 6379 不可达时自动跳过 |

## 新增内容与兼容性

- 新增应用端口：`IntakeSubmissionLock`、`IntakeSubmissionLockHandle`。
- 新增应用异常：`IntakeSubmissionInProgressException`。
- 新增应用服务：`ClaimIntakeCreationService`、`SignNoGenerator`。
- 新增基础设施：`RedissonIntakeSubmissionLock`、`IntakeSubmissionLockProperties`、`IntakeSubmissionLockConfiguration`。
- 修改 `SubmitIntakeCommand`：新增材料元数据列表；同步更新两个 Controller、`reference` Converter 和相关测试。
- 新增配置：`claim.intake.submission.lock.wait-time`、`lease-time`、`key-prefix`；`lease-time` 作为 Redisson watchdog 基础租期，不再作为不可续期的固定租期。
- 新增错误码：`INTAKE_SUBMISSION_IN_PROGRESS`（409）。
- 新增指标：`claim.intake.submission`、`claim.intake.submission.lock`、`claim.intake.submission.conflict`。
- 不修改数据库 Schema、HTTP 路径、成功响应结构、`signNo` 语义和材料上传状态机。
- Redis 仅新增短期锁键，不保存业务事实；键在租期后自动清理。
- 本功能完成后需回写 `specs/tenant-adapter-intake/` 的 A-1，说明标准命令已承载材料元数据，`reference` 仍传空列表。

## 完成门禁

功能只能标记为 Done 当且仅当：

- TK-CISF-002 至 TK-CISF-009、TK-CISF-011 全部 Done；
- TEST-CISF-001 至 TEST-CISF-016 有可核对证据，或明确记录无法执行的原因和风险；
- 真实 MySQL/Redis 并发测试证明同一幂等键只创建一条申请；
- Redisson watchdog 续期在真实 Redis 下验证通过，且 `lease-time` 语义与文档一致；
- `mvn verify` BUILD SUCCESS；
- `tenant-adapter-intake` 的 A-1 和本功能 Spec 已同步；
- Gateway 透传契约若仍未实现，已在已知限制中明确端到端验证缺口。

## 回写规则

实现阶段完成后，必须更新本文件：

- 将对应任务状态改为 Done；
- 填写实际测试名称、测试数量和 `mvn verify` 结果；
- 记录新增的包、类、配置项、错误码和指标；
- 若锁参数、材料初始状态或 Redis 降级策略与当前设计不一致，先更新 `requirements.md` 和 `design.md`，再修改代码；
- 若 Gateway 或验签/解密仍未实现，必须保留为外部依赖和已知限制，不得用服务内模拟冒充端到端完成。

## 实现与验证记录

### 2026-10-23

- 功能分支：`feat/claim-intake-standard-flow`，基于 `feat/tenant-adapter-intake` HEAD 建立；`main` 尚未包含租户适配器前置提交，因此是堆叠分支。
- 单元与控制器测试：本 Spec 相关 9 个测试类共 56 个测试通过，覆盖命令材料归一化、创建事务、锁适配器、配置绑定、编排、异常映射和两个既有入口。
- 并发集成测试：`ClaimIntakeSubmissionIntegrationTest` 在本地 Docker MySQL 8.4 与 Redis 7 上执行，6 个线程提交同一 `tenantCode + externalApplyNo`，结果为 1 个 `created`、5 个 `replayed_locked`，单申请、单材料、单 `SIGN_RECEIVE` 日志、相同 `signNo`，测试通过。
- 全量构建：`mvn -f backend/pom.xml verify` BUILD SUCCESS；`claim-common`、`claim-gateway`、`claim-intake-service`、`claim-case-delivery` 均 SUCCESS，`claim-intake-service` 共 104 个测试通过。
- 本地环境：MySQL 容器数据卷早于初始化脚本，已手工创建 `claim_intake_db` 并执行仓库内 `02-create-claim-intake-tables.sql`；Redis 正常响应 `PONG`。

### 2026-10-23 锁续期与规范复核

- Spec 先行更新：FR-CISF-019、DS-CISF-003、DS-CISF-006、DS-CISF-009、DS-CISF-013 明确 `lease-time` 为 watchdog 基础租期、自动续期规则和停止续期的边界。
- 实现：新增 `IntakeSubmissionLockConfiguration`，将 `lease-time` 映射为 Redisson 客户端级 `lockWatchdogTimeout`；`RedissonIntakeSubmissionLock` 改用 `tryLock(waitTime, TimeUnit)`，不传固定租期，由 watchdog 每三分之一基础租期续期。
- 规范复核：`ClaimIntakeCommandService`、`ClaimIntakeCreationService`、`SignNoGenerator`、`IntakeSubmissionLockProperties`、`RedissonIntakeSubmissionLock` 统一使用构造器注入、SLF4J 占位符、固定编码常量化、异常保留堆栈和类级 Javadoc；锁配置校验改用 Hibernate Validator `@DurationMin`。
- 续期测试：`RedissonIntakeSubmissionLockRenewalTest` 在本地 Redis 7 上通过，证明锁跨越 1 秒基础租期后仍保持互斥，释放后可重新获取；若退化为固定租期该测试会失败。
- 配置测试：`IntakeSubmissionLockConfigurationTest` 2 个测试通过，验证默认 10 秒和覆盖 7 秒均正确映射到 `lockWatchdogTimeout`。
- 全量构建：`mvn -f backend/pom.xml verify` BUILD SUCCESS，`claim-intake-service` 108 个测试通过，0 失败、0 跳过。
- 已知限制：Gateway 仍为启动骨架，未实现鉴权、租户识别、限流和 `X-Claim-*` 可信头透传；`TK-CISF-010` 保持 Blocked，真实租户协议、验签和解密同样未实现。本功能验证的是服务内入口到持久化的完整路径，不等同于端到端验收。
