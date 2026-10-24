# 签收服务精简设计

## 1. 设计结论

删除两处蓝本没有要求、且与基线归属冲突的实现：租户协议适配整簇（基线 §4 归网关）与人员信息强类型建模（蓝本 §5.1 的 `Sign` 聚合不含人员对象）。签收主链路、发号机制与三级幂等一行逻辑都不改。

删除只做减法：不新建包、不新建类、不引入兼容壳。人员信息退化为"不再结构化"，`applicantName` 与 `extJson` 两个字段承担全部入口。

删除后的链路仍是 `SignRequest → SignCommand → SignCommandHandler → SignCreationService → Sign → SignRepository`，与蓝本 §5.1 一致；`Sign` 的不变量（`tenantCode + signNo` 标识、`tenantCode + externalApplyNo` 幂等键、`version` 乐观锁、材料状态转换）与异常分类均不变，只减少租户分支。

## 2. 基线核对

| 基线条款 | 本轮动作 |
| --- | --- |
| §4 服务边界：租户协议适配属网关 | 删除签收服务内的适配实现，偏差登记改为"暂缓" |
| §7 `tenantCode` 来自受信任上下文 | 不变，`GatewayRequestContextResolver` 保留 |
| §7 三级幂等 + 发号三条路径 | 全量保留，配置与键不变 |
| §8 保留契约 HTTP 路径 | `/api/v1/tenant-intakes` 移出保留契约（见第 6 节） |
| §8 保留契约 `SignErrorCode` 6 项 | 全部保留，不触碰常量名与上报值 |
| §8 保留契约指标名与 Redis 键 | 不变 |

## 3. 删除清单

### 3.1 主代码（39 文件 / 1290 行）

簇 A —— 租户协议适配（26 文件 / 890 行）：

| 路径 | 文件数 | 行数 | 删除理由 |
| --- | --- | --- | --- |
| `interfaces/tenant/core/context/`（`TenantContext`、`TenantContextHolder`） | 2 | 70 | 协议版本上下文只在租户入口有意义；标准入口沿用 `GatewayRequestContext` |
| `interfaces/tenant/core/converter/`（`CodeMapper`、`RequestConverter`） | 2 | 75 | 租户编码 → 平台枚举的映射器，只服务租户报文 |
| `interfaces/tenant/core/exception/`（5 个异常） | 5 | 104 | 只由租户 Pipeline 抛出 |
| `interfaces/tenant/core/parser/`（`DefaultJsonParser`、`RequestParser`） | 2 | 79 | 租户原始报文解析 |
| `interfaces/tenant/core/pipeline/`（`DefaultSignConvertPipeline`、`SignConvertPipeline`） | 2 | 62 | 租户报文 → `SignCommand` 的固定编排 |
| `interfaces/tenant/core/registry/`（`TenantProtocolAdapter`、`TenantProtocolAdapterRegistry`、`TenantProtocolKey`） | 3 | 126 | 租户 + 协议版本路由注册表 |
| `interfaces/tenant/core/validator/`（`DefaultRequestValidator`、`RequestValidator`） | 2 | 59 | 租户 DTO 的 Bean Validation 包装 |
| `interfaces/tenant/reference/`（适配器、常量、converter、2 个 dto、validator） | 6 | 207 | 参考租户实现，不代表任何真实租户，只为验证机制存在 |
| `interfaces/web/controller/TenantSignController.java` | 1 | 63 | `POST /api/v1/tenant-intakes` 入口 |
| `interfaces/web/context/TenantSignContextResolver.java` | 1 | 45 | 协议版本头的可信上下文装配 |

簇 B —— 人员强类型建模（13 文件 / 400 行）：

| 路径 | 文件数 | 行数 | 删除理由 |
| --- | --- | --- | --- |
| `domain/model/person/`（`Person`、`Applicant`、`AccidentPerson`、`MainInsured`、`Payee`） | 5 | 130 | 蓝本 §5.1 的 `Sign` 聚合字段不含人员对象 |
| `infrastructure/persistence/dataobject/person/`（5 个 `*InfoDO`） | 5 | 95 | 与人员领域类型同生同灭 |
| `domain/model/CertificateType.java` | 1 | 12 | 只被人员对象与租户 converter 引用 |
| `infrastructure/persistence/converter/PersonConverter.java` | 1 | 87 | 人员 DO ↔ 领域对象双向映射 |
| `interfaces/web/converter/PersonRequestConverter.java` | 1 | 76 | 请求 JSON → 人员领域对象 |

### 3.2 测试（20 文件 / 1569 行）

| 路径 | 文件数 | 行数 |
| --- | --- | --- |
| `interfaces/tenant/**`（含 `TenantAdapterArchitectureTest` 与 8 个 `support/` 夹具） | 16 | 1126 |
| `interfaces/web/controller/TenantSignControllerTest.java` | 1 | 214 |
| `interfaces/web/context/TenantSignContextResolverTest.java` | 1 | 88 |
| `infrastructure/persistence/converter/PersonConverterTest.java` | 1 | 112 |
| `interfaces/web/converter/PersonRequestConverterTest.java` | 1 | 29 |

### 3.3 保留但曾被列为候选（核对结论）

| 对象 | 结论 |
| --- | --- |
| `application/port/`（10 个端口）+ `application/result/`（9 个结果） | 保留，发号与幂等的端口定义 |
| `application/service/SignNo*`（5 个服务）、`MysqlSignNoFallbackAllocator` | 保留，用户明确要求不动发号 |
| `infrastructure/cache/`（5 个 Redis/Redisson 实现）、`infrastructure/config/SignNo*` 与 `SignSubmission*`（5 个） | 保留 |
| `infrastructure/scheduling/SignNoReconciliationScheduler` | 保留 |
| `infrastructure/config/SignNoSchemaValidator` + `SignNoSchemaValidatorTest` | 保留，校验的是保留下来的 `uk_sign_no` |
| `infrastructure/persistence/converter/SignPersistenceConverter` + 其测试 | 保留（删人员映射行，其余映射不动） |
| `interfaces/web/context/GatewayRequestContext*`、`exception/TenantContextMissingException`、`exception/UntrustedCallerException`、`interceptor/TraceLoggingInterceptor`、`config/WebConfig` | 保留，服务间凭证与 Trace 边界 |
| `domain/model/Material`、`MaterialType`、`SignStatus`、`SignNo`、`SignDetails`、`InteractionType` | 保留 |

## 4. 修改清单（不删除，只减字段/减分支）

### 4.1 主代码（11 文件）

| 文件 | 修改内容 |
| --- | --- |
| `interfaces/web/request/SignRequest.java` | 移除 `applicantInfo`、`accidentPersonInfo`、`mainInsuredInfo`、`payeeInfo` 四个 `JsonNode` 字段 |
| `interfaces/web/controller/SignController.java` | 去掉 `PersonRequestConverter` 依赖与四次转换调用；`SignCommand` 构造参数同步收缩 |
| `application/command/SignCommand.java` | 移除 `applicant`、`accidentPerson`、`mainInsured`、`payee` 四个组件 |
| `domain/model/Sign.java` | 移除四个人员字段、构造参数、`create`/`restore` 参数与访问器；类注释同步 |
| `domain/model/SignState.java` | 移除四个人员组件 |
| `application/service/SignCreationService.java` | `Sign.create(...)` 调用参数同步 |
| `infrastructure/persistence/dataobject/SignDO.java` | 移除四个 `*InfoDO` 字段与 `JacksonTypeHandler` 注解，四列转为不写入 |
| `infrastructure/persistence/converter/SignPersistenceConverter.java` | 移除 `uses = PersonConverter.class` 与 8 条人员 `@Mapping`；`unmappedTargetPolicy = ERROR` 保留，未映射即编译失败 |
| `interfaces/web/SignWebContract.java` | 移除 `Paths.TENANT_INTAKES`、`Headers.PROTOCOL_VERSION`、`Patterns.PROTOCOL_VERSION`；`DEFAULT_FIELD` 保留（异常处理器使用） |
| `interfaces/web/advice/SignExceptionHandler.java` | 移除 `malformedPayload`、`tenantRequestValidation`、`tenantProtocolNotSupported` 三个 handler 与 5 个租户异常 import |
| `interfaces/web/advice/SignErrorCode.java` | 移除 `MALFORMED_REQUEST`、`TENANT_PROTOCOL_NOT_SUPPORTED` 两个枚举项（只随租户入口出现，不在基线保留的 6 项内） |

`infrastructure/persistence/SignTable.java` 的四个列名常量保留：Schema 本轮不改，常量仍描述真实列。

### 4.2 测试（8 文件）

| 文件 | 修改内容 |
| --- | --- |
| `application/service/SignNoGenerationIntegrationTest.java` | `shouldServeNewFormatFromBothHttpEntryPoints` 改为只验标准入口并重命名；删除 `SupportTenantTestData` import 与租户报文分支 |
| `application/handler/SignCommandHandlerTest.java` | `SignCommand` / `Sign.create` 构造参数收缩，人员断言删除 |
| `application/handler/SignSubmissionIntegrationTest.java` | `Sign.create` 参数收缩 |
| `application/service/SignCreationServiceTest.java` | `SignCommand` 构造参数收缩 |
| `application/command/SignCommandTest.java` | 按新组件数重写断言 |
| `domain/model/SignTest.java` | `Sign.create` 参数收缩 |
| `infrastructure/persistence/converter/SignPersistenceConverterTest.java` | 删除人员映射用例，保留 `extJson`、`applicantName`、状态与版本映射用例 |
| `interfaces/web/controller/SignControllerTest.java` | 去掉 `PersonRequestConverter` mock 与请求体人员字段 |

## 5. 数量核对

| 项 | 改前 | 改后 |
| --- | --- | --- |
| 主代码 | 129 文件 / 5456 行 | 90 文件 / 4166 行 |
| 测试 | 46 文件 / 5245 行 | 26 文件 / 3676 行 |
| 签收服务合计 | 175 文件 / 10701 行 | 116 文件 / 7842 行 |

实施后以 `find src -name '*.java' | xargs wc -l` 复核；主代码行数偏离 ±3% 以上视为清单不准，需回改本节而不是放过。

## 6. 契约影响与基线回写

对调用方的实际影响只有两条，均在需求第 5 条内：

1. `POST /api/v1/tenant-intakes` 与其 `X-Claim-Protocol-Version` 头整体消失。
2. `POST /api/v1/claim-intakes` 请求体少四个人员对象字段。全仓唯一的 `FAIL_ON_UNKNOWN_PROPERTIES` 开关位于被删除的 `DefaultJsonParser`，配置文件里没有任何 Jackson 定制，因此标准入口使用 Spring Boot 默认宽松反序列化：旧调用方继续多传这四个字段仍返回 200，字段被丢弃且不入库。本轮不为此新增配置。

签收详情响应不变：`SignDetailResult` 当前就不返回人员对象，因此删除人员建模不减少任何已返回字段，也不新增 `extJson` 返回。

基线 `docs/specs/baseline.md` 需在删除前完成以下回写：

| 位置 | 变更 |
| --- | --- |
| §8 保留契约 · HTTP 路径行 | 保留值改为仅 `/api/v1/claim-intakes`，并注明 `/api/v1/tenant-intakes` 已随精简删除 |
| §8 保留契约 · 领域错误码行 | 6 项不动，追加一句：`MALFORMED_REQUEST`、`TENANT_PROTOCOL_NOT_SUPPORTED` 已随租户入口删除，不再是对内承诺 |
| §8 已知偏差 · 第 1 行 | 现状改为"签收服务不再承载租户协议适配（代码已删除），网关侧未实现"，收敛归属改为"Gateway Spec，暂缓不排期" |
| §8 已知偏差 · 其余未实现项 | 问题件、撤销、材料状态、推送服务、前端功能域、网关能力统一标注"暂缓、不排期" |
| §8 已知偏差 · 新增行 | `t_claim_intake` 的 `applicant_info`、`accident_person_info`、`main_insured_info`、`payee_info` 四列不再写入；历史行不迁移不删除，列清理由数据库重设计 Spec 承接 |
| §7 全局业务规则 | 敏感信息一节补充：人员明细不做强类型建模，只经 `extJson` 原样入库，不得进入日志与响应 |
| §12 变更记录 | 新增一行记录本轮精简与契约收缩，评审列写用户确认结论 |

## 7. 测试策略

删除不新增测试，只保证既有行为仍被覆盖：

- 单元层：`SignCommandHandlerTest`、`SignCreationServiceTest`、`SignTest`、`SignCommandTest`、`SignPersistenceConverterTest` 覆盖提交编排、乐观锁冲突、状态转换与剩余字段映射。
- 边界层：`SignControllerTest` 覆盖标准入口的校验失败、可信上下文缺失与未鉴权响应。
- 集成层：`SignSubmissionIntegrationTest` 覆盖并发同键只创建一条；`SignNoGenerationIntegrationTest` 覆盖 Redis 计数、数据库高水位兜底、对账校正与 `uk_sign_no` 全局唯一，并把双入口用例收敛为单入口用例。
- 架构层：`PersistenceArchitectureTest`、`SignNoArchitectureTest` 保留；`TenantAdapterArchitectureTest` 随被守护的包一起删除，不留空守护。
- 回归判据：删除前后同一组测试的通过集合必须一致（除显式删除的租户/人员用例外），且依赖本地 MySQL 与 Redis 的用例真实执行。

## 8. 本轮不做

- 不为已删除的租户入口保留任何开关、空实现或 `@Deprecated` 壳。
- 不为 `/api/v1/claim-intakes` 的 `HttpMessageNotReadableException` 补 400 映射（当前落 `INTERNAL_ERROR` 500）：属既有缺陷，登记为偏差，不与删除混在一起。
- 不改数据库 Schema，不删列、不迁移历史行。
- 不改指标名、Redis 键前缀与 `claim.sign.*` 配置命名空间。
- 不重排包结构；`interfaces/tenant` 整包消失，不建替代包。

## 9. 风险与回滚

- 风险 1：租户入口有已部署调用方。缓解：需求第 5 条未经用户明确确认不删代码；回滚是恢复整簇文件（无数据迁移，`git revert` 单个删除提交即可）。
- 风险 2：`SignPersistenceConverter` 使用 `unmappedTargetPolicy = ERROR`，删字段时 MapStruct 会因未映射目标编译失败。缓解：§4.1 已同步删除人员映射，编译期即可暴露遗漏，不会静默丢字段。
- 风险 3：人员四列转为不写入后，依赖这些列的下游（无代码依赖，仅人工查询/报表）读到 NULL。缓解：登记为 §8 偏差行，交付说明里点名列名。
- 风险 4：并发用例依赖本地 MySQL/Redis，容器不可用时静默跳过，会让"测试全绿"失真。缓解：验证前确认 `claim-platform-mysql`、`claim-platform-redis` 健康，并在提交说明里记录实际执行的集成用例数。
