# 架构标准流对齐设计

本设计只覆盖第一阶段：目录、模块、包、领域词汇与分层归属对齐。链路行为、幂等语义、事务边界与数据库边界保持不变。

## 1. 核心链路（改名后）

```text
租户报文 → TenantSignController → TenantContextResolver（协议键路由）
        → TenantProtocolAdapterRegistry → ReferenceProtocolAdapter（Parser → Validator → Converter）
        → SignConvertPipeline → SignCommand
        → SignCommandHandler（三级幂等 + 业务锁 + 回放）
        → SignCreationService（REQUIRES_NEW 创建事务）
        → Sign 聚合 → SignRepository / MaterialRepository / InteractionLogRepository

标准调用方 → SignController → SignCommandHandler / SignQueryService → 同上
```

链路上的判断、锁、发号与降级策略本轮全部不变，只替换参与者的名字。

## 2. 目录与 Maven 模块

| 现 | 目标 |
| --- | --- |
| `backend/` | `claim-platform-backend/`（artifactId 已是 `claim-platform-backend`，只改目录） |
| `frontend/` | `claim-platform-frontend/`（`package.json` 的 `name` 同步改为 `claim-platform-frontend`） |
| `backend/claim-intake-service/` | `claim-platform-backend/claim-sign-service/`（artifactId、`name`、`spring.application.name` 同步） |
| `backend/claim-case-delivery/` | `claim-platform-backend/claim-case-push-service/`（同上） |
| `backend/claim-common/` | 位置随父目录变化，名称不变 |
| `backend/claim-gateway/` | 位置随父目录变化，名称不变，本轮无代码变更 |

根 `pom.xml` 的 `<module>backend</module>` 改为 `claim-platform-backend`；父 pom 的 `<modules>` 顺序改为 `claim-common`、`claim-gateway`、`claim-sign-service`、`claim-case-push-service`。

## 3. 包根

| 现 | 目标 |
| --- | --- |
| `com.claim.platform.intake.**` | `com.claim.platform.sign.**` |
| `com.claim.platform.casedelivery` | `com.claim.platform.push` |
| `com.claim.platform.gateway.**` | 不变 |
| `com.claim.platform.common.**` | 不变 |

目标架构文档中的 `com.xxx.claim.sign` 按占位符理解，实际沿用 `com.claim.platform` 组根，不改 `groupId`。启动类 `ClaimIntakeApplication` → `ClaimSignApplication`，`ClaimCaseDeliveryApplication` → `ClaimCasePushApplication`；`logback-spring.xml` 的 `<contextName>`、日志文件名与 `<logger name>` 同步。

## 4. application 层目标形态

基线原有 `application/service` 同时承担写入用例与查询用例。按已确认的混合形态拆开：

```text
application
├─ command     写入用例输入，不含 Web / MyBatis / 外部协议对象
├─ handler     命令处理器：一条命令一个入口，加载领域对象、控制事务边界、返回 Result
├─ service     查询服务与应用内支撑服务（创建事务编排、发号、对账、指标）
├─ result      应用对外返回结果
├─ port        外部能力端口
└─ exception   用例层异常
```

- 写入用例入口类用 `Handler` 后缀：`SignCommandHandler`（原 `ClaimIntakeCommandService`）。
- 非用例入口的事务与支撑编排保留 `Service` 后缀：`SignCreationService`、`SignNoAllocationService`、`SignNoReconciliationService`。
- 读取用例用查询服务：`SignQueryService`（原 `ClaimIntakeQueryService`）。
- 目标文档第 5 节的 `GetSignQuery` / `GetSignQueryHandler` 不引入；文档第 7 节的 `SignQueryService` 与之冲突，按第 7 节执行。
- `MaterialQueryService`、`ClaimIssueQueryService`、`PushTaskQueryService`、`PushRecordQueryService` 属于能力轮，本轮不建空实现。

## 5. 签收服务领域词汇映射

`Intake`（受理）整体退出领域词汇，`Sign`（签收）成为唯一称呼。映射按层列出，全部为机械改名，不改变行为。

### domain

| 现 | 目标 |
| --- | --- |
| `ClaimIntake` | `Sign` |
| `ClaimIntakeState` | `SignState` |
| `ClaimIntakeDetails` | `SignDetails`（内部 `intake` 分量 → `sign`） |
| `IntakeStatus` | `SignStatus`（枚举常量名与持久化取值不变） |
| `IntakeMaterial` | `Material` |
| `IntakeMaterialType` | `MaterialType` |
| `SignNoFormat` | `SignNo` |
| `ClaimIntakeStore` | `SignRepository` |
| `IntakeMaterialStore` | `MaterialRepository` |
| `InteractionLogStore` | `InteractionLogRepository` |
| `ClaimIntakeNotFoundException` | `SignNotFoundException` |
| `InvalidIntakeStatusException` | `InvalidSignStatusException` |
| `ConcurrentIntakeModificationException` | `ConcurrentSignModificationException` |

`CertificateType`、`InteractionType`、`person/*` 与 `domain/repository` 的端口语义不变。

### application

| 现 | 目标 |
| --- | --- |
| `SubmitIntakeCommand` | `SignCommand` |
| `UploadMaterialsCommand` | `UploadMaterialCommand` |
| `ClaimIntakeCommandService` | `SignCommandHandler`（移入 `application/handler`） |
| `ClaimIntakeCreationService` | `SignCreationService` |
| `ClaimIntakeQueryService` | `SignQueryService` |
| `IntakeMetricConstants` | `SignMetricConstants` |
| `ClaimIntakeResult` / `ClaimIntakeDetailResult` | `SignResult` / `SignDetailResult` |
| `IntakeSubmissionInProgressException` | `SignSubmissionInProgressException` |
| `IntakeSubmissionLock` / `IntakeSubmissionLockHandle` / `IntakeSubmissionReplayCache` | `SignSubmissionLock` / `SignSubmissionLockHandle` / `SignSubmissionReplayCache` |
| `SignNo*`（port 与 result） | 名称不变，仅包根变化 |

### interfaces

| 现 | 目标 |
| --- | --- |
| `ClaimIntakeController` | `SignController` |
| `TenantIntakeController` | `TenantSignController` |
| `CreateIntakeRequest` | `SignRequest` |
| `UploadMaterialsRequest` | `UploadMaterialRequest` |
| `ClaimIntakeWebContract` | `SignWebContract` |
| `IntakeErrorCode` | `SignErrorCode` |
| `ClaimIntakeExceptionHandler` | `SignExceptionHandler` |
| `TenantIntakeContextResolver` | `TenantSignContextResolver` |
| `TenantProtocolHandler` | `TenantProtocolAdapter` |
| `TenantProtocolHandlerRegistry` | `TenantProtocolAdapterRegistry` |
| `ReferenceProtocolHandler` | `ReferenceProtocolAdapter` |
| `IntakeConvertPipeline` / `DefaultIntakeConvertPipeline` | `SignConvertPipeline` / `DefaultSignConvertPipeline` |
| `IntakeCodeMappingException` / `IntakeConversionException` / `IntakePayloadParseException` / `IntakeRequestValidationException` | `Sign*Exception` 对应形式 |
| `ReferenceIntakePayload` | `ReferenceSignPayload` |

### infrastructure

| 现 | 目标 |
| --- | --- |
| `ClaimIntakeDO` / `ClaimIntakeTable` / `ClaimIntakeMapper` | `SignDO` / `SignTable` / `SignMapper` |
| `ClaimIntakePersistenceConverter` | `SignPersistenceConverter` |
| `IntakePersistenceRepositoryImpl` | `SignRepositoryImpl` |
| `IntakeMaterialDO` / `IntakeMaterialTable` / `IntakeMaterialMapper` | `MaterialDO` / `MaterialTable` / `MaterialMapper` |
| `IntakeSubmissionLockConfiguration` / `IntakeSubmissionLockProperties` / `IntakeSubmissionReplayCacheProperties` | `SignSubmission*` 对应形式 |
| `RedissonIntakeSubmissionLock` / `RedisIntakeSubmissionReplayCache` | `RedissonSignSubmissionLock` / `RedisSignSubmissionReplayCache` |
| `SignNo*`（cache / config / persistence / scheduling） | 名称不变，仅包根变化 |

DO 与表名常量持有的数据库物理名（`t_claim_intake`、`intake_status` 等）保持原值。

配置与运行期契约的取舍：

| 对象 | 本轮处理 | 依据 |
| --- | --- | --- |
| 配置命名空间 `claim.intake.*` 与环境变量 `CLAIM_INTAKE_*` | 改为 `claim.sign.*` 与 `CLAIM_SIGN_*` | 配置键是装配入口，无外部消费方，Nacos 配置中心在本仓库关闭 |
| 指标名 `claim.intake.*`（8 个） | 保持不变 | `IntakeMetricConstants` 已声明指标契约属外部可观测接口，值不随重构变化 |
| Redis 键前缀取值 `claim:intake:submit:`、`claim:intake:replay:` | 保持不变 | `RedisCacheConstants` 已声明键结构属运行期契约；改值会让既有幂等回放缓存整体失效 |
| Lua 脚本路径与发号键 | 保持不变 | 同属运行期契约 |
| 错误码枚举常量名与错误码字符串（`CLAIM_INTAKE_NOT_FOUND` 等） | 都保持不变，仅类名改为 `SignErrorCode` | 错误码是面向调用方的机器可读契约，常量名与上报值一体，拆开命名会造成误解 |
| HTTP 路径与请求/响应字段名 | 保持不变 | 对外契约 |

### 测试

测试类与被测类同名后缀同步；测试方法名中的 `Intake` 按 `Sign` 改写（例：`shouldCreateIntakeWithAllocatedSignNo` → `shouldCreateSignWithAllocatedSignNo`），断言语义不变。测试替身 `SupportTenant*` 系列随领域类型改名。

## 6. 接口与契约

- HTTP 方法、路径、请求体字段、响应体字段与错误码字符串全部保持原值；`SignWebContract.Paths` 的常量名与常量值都保留 `CLAIM_INTAKES`、`TENANT_INTAKES`，因为值本身是对外契约。
- 响应信封继续是 `claim-common` 的 `ApiResult<T>`，本轮不引入与 `SignResult` 并存的 `SignResponse`，避免同一数据两种类型。
- 租户协议键、请求头名与协议版本字符串不变。

## 7. 数据模型与边界

本轮不动数据：库名 `claim_intake_db`、`push_db` 不变，`t_claim_intake*` 五张表、列名、索引、`docker/mysql/init` 与 `docker/mysql/migration` 脚本全部原样保留；`application.yml` 的数据源 URL 不变。聚合、状态机与不变量无任何定义变化。

## 8. 已知偏差登记

以下项与目标架构文档存在差距，本轮不处理，留给后续独立 Spec：

| 偏差 | 现状 | 归属后续 Spec |
| --- | --- | --- |
| 租户协议适配位于签收服务而非网关 | `interfaces/tenant/**` 在 `claim-sign-service` | Gateway Spec（需把 Servlet 管道重写为响应式过滤器） |
| 验签、加解密、`TenantResolver`、`SignatureVerifier` | 未实现 | Gateway Spec |
| HTTP 路径、领域错误码字符串、指标名、Redis 键前缀与库表列仍含 `intake` 词汇 | `/api/v1/claim-intakes`、`CLAIM_INTAKE_NOT_FOUND`、`claim.intake.submission`、`claim:intake:submit:`、`t_claim_intake*`、`intake_status` | 对外契约版本化 Spec 与能力轮的数据库重设计一并处理 |
| `MaterialStatus`、问题件、材料补充、撤销 | 仅有表，无领域模型 | 签收能力 Spec |
| `PushTask`、`PushRecord`、`PushReceipt`、`InsurerPushAdapter`、`strategy` | 服务仅有启动类，`push_db` 无表 | 案件推送 Spec |
| 前端六个功能域 | `src` 为空 | 前端 Spec |
| 网关目录不建空职责包 | 遵循 `implementation.md`「不创建无实际用途的空包」 | Gateway Spec 落地时随实现创建 |

## 9. 实施顺序

1. 文档：基线与本 Spec 三件套先行（已完成）。
2. 机械重命名，按任务分四次提交：目录与 Maven 模块 → Java 包根 → 领域词汇 → README 与文档引用。
3. 每一步之后 `mvn verify`；词汇重命名体量集中在一次改动内完成，用编译器而不是人工遍历兜底。
4. 不 push、不合并 `main`。

## 10. 测试策略

- 以现有测试为行为基线：本轮前后测试类数量、方法数量与断言内容不变，只随名称与包路径同步。若改名导致任何测试需要修改断言，即视为越界，停止并复核。
- 重点回归由既有测试覆盖：三级幂等与回放、锁超时与降级、唯一键回查、编号碰撞、创建事务原子性、租户边界与跨租户查询隔离、领域层不依赖基础设施的架构测试。
- 验证命令：`mvn verify`（本轮无前端代码变更，不跑前端）。
- 并发类集成测试依赖本地 MySQL 与 Redis；两者不可用时会被跳过，验证前须确认容器在位，否则在交付说明中写明跳过范围。
