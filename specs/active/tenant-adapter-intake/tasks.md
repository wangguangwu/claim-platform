# 租户签收报文转换层任务

## 状态说明

| 状态 | 含义 |
| --- | --- |
| Done | 已完成并有可核对证据 |
| In Progress | 部分完成，剩余工作受外部契约或决策阻塞 |
| Planned | 已拆分，等待实现条件 |
| Blocked | 缺少真实租户协议、入口契约或前置决策，暂不能实现 |

当前进度：通用转换框架、`reference` 参考租户适配器、HTTP 入口与异常映射已完成；真实租户协议仍待外部提供，`reference` 不代表任何真实租户。

## 任务清单

| 任务 | 关联需求与设计 | 状态 | 依赖 | 完成证据或阻塞原因 |
| --- | --- | --- | --- | --- |
| TK-TAI-001 建立转换层 Spec 并统一 Intake 命名 | FR-TAI-001 至 FR-TAI-017；DS 第 1、2、11 节 | Done | 无 | 本目录 `requirements.md`、`design.md`、`tasks.md` 已建立；新类名使用 `IntakeConvertPipeline`、`TenantProtocolHandler`、`SubmitIntakeCommand`，不新增 `Sign*` 类型 |
| TK-TAI-002 确认租户上下文与协议版本来源 | FR-TAI-002、FR-TAI-012、FR-TAI-016、FR-TAI-018；DS 第 7 节 | Done | Gateway/入口接入契约 | 已确定 `X-Claim-Protocol-Version` 必填、格式 `[A-Za-z0-9._-]{1,32}`、无缺省版本；`TenantIntakeContextResolver` 复用现有服务凭证、租户和 Trace 头，`TenantIntakeController` 在 `finally` 清理 Holder |
| TK-TAI-003 实现转换契约与默认 Parser/Validator | FR-TAI-001、FR-TAI-003 至 FR-TAI-007、FR-TAI-015；DS 第 3、5 节 | Done | TK-TAI-002、首批真实租户报文 | 已实现 `RequestParser`、`RequestValidator`、`RequestConverter`、`DefaultJsonParser`、`DefaultRequestValidator` 和 `CodeMapper`；默认组件不绑定真实租户，测试见 TEST-TAI-001 至 TEST-TAI-009 |
| TK-TAI-004 实现组件注册表与启动校验 | FR-TAI-011 至 FR-TAI-014；DS 第 4 节 | Done | TK-TAI-003 | 已实现 `TenantProtocolKey`、`TenantProtocolHandler`、`TenantProtocolHandlerRegistry`；重复注册、组件不完整和未注册协议均在测试中验证（TEST-TAI-010、TEST-TAI-011） |
| TK-TAI-005 实现首批租户 Parser、Validator、Converter 和编码映射 | FR-TAI-003 至 FR-TAI-009、FR-TAI-015；DS 第 5、6、7.2 节 | Done（reference 参考实现） | 真实租户报文、字段说明、材料与证件编码表 | 已实现 `interfaces/tenant/reference`：复用 `DefaultJsonParser`，新增公共+差异 Validator、显式 Converter、证件编码映射和 `@Component ReferenceProtocolHandler`（`reference/v1`）；真实租户协议仍待提供，材料映射仍受 A-1 约束 |
| TK-TAI-006 接入入口并完成异常映射 | FR-TAI-001、FR-TAI-004、FR-TAI-006、FR-TAI-014、FR-TAI-016、FR-TAI-018、FR-TAI-019；DS 第 7.1、8.3 节 | Done | TK-TAI-002、TK-TAI-005、入口 API Spec | 新增 `POST /api/v1/tenant-intakes`、`TenantIntakeContextResolver` 和 `TenantIntakeController`；Holder 在 Pipeline 前写入、`finally` 清理；`MALFORMED_REQUEST`、`REQUEST_VALIDATION_FAILED`、`TENANT_PROTOCOL_NOT_SUPPORTED`、`TENANT_CONTEXT_MISSING` 已进入新入口响应 |
| TK-TAI-007 补充转换层单元测试和架构边界测试 | TEST-TAI-001 至 TEST-TAI-018；DS 第 10 节 | Done | TK-TAI-003 至 TK-TAI-006 | 本功能共 10 个测试类（其中入口与 `reference` 适配器新增 4 个）、58 个测试方法，Maven 执行 77 个测试，覆盖 TEST-TAI-001 至 TEST-TAI-018；架构边界采用源码扫描测试，未引入 ArchUnit |
| TK-TAI-008 执行构建与回归验证 | TEST-TAI-001 至 TEST-TAI-018 | Done | TK-TAI-007 | `mvn -pl backend/claim-intake-service -am verify` BUILD SUCCESS；claim-intake-service 77 个测试全部通过（Failures 0、Errors 0、Skipped 0），既有受理、持久化和控制器测试无回归 |

## 实现前门禁

当前状态：入口与 `reference` 参考租户已实现；下列真实租户门禁仍未满足，真实租户协议不得用 `reference` 冒充。

- [ ] 已取得至少一个真实租户的样例报文、字段说明和协议版本号；当前只有 `reference` 参考协议。
- [ ] 已确认材料编码的正式映射表；证件编码已按 `reference` 协议映射，材料仍受 A-1 约束。
- [ ] 已确认 `SubmitIntakeCommand` 是否承载材料；若承载，先补充 Application 事务和用例设计。
- [x] 已确认 `protocolVersion` 的可信来源、请求头名称和 Gateway 透传契约：`X-Claim-Protocol-Version` 必填，无缺省版本。
- [x] 已确认 `TenantContextHolder` 的写入时机和清理方式：入口在 Pipeline 前写入，`finally` 清理；Holder 位于 `interfaces/tenant/core/context`，暂不进入 `claim-common`。
- [x] 已确认入口 API 的路径、鉴权、错误码和兼容性影响：`POST /api/v1/tenant-intakes`，复用服务凭证、租户和 Trace 头，新增协议版本头；既有 `/api/v1/claim-intakes` 不变。
- [x] 当前根需求、本功能 Spec 和 `standards/java.md`、`standards/ddd.md`、`standards/error-handling.md` 已读取。
- [x] 当前分支为 `feat/tenant-adapter-intake`，工作区已有修改已识别，未被覆盖或误提交。

## 测试证据

| 测试 | 覆盖需求 | 状态 | 证据 |
| --- | --- | --- | --- |
| TEST-TAI-001 | FR-TAI-003 | Passed | `DefaultJsonParserTest.shouldParseValidJsonIntoTenantDto` |
| TEST-TAI-002 | FR-TAI-004 | Passed | `DefaultJsonParserTest.shouldRejectMalformedJsonWithoutLeakingSensitiveValues`、`shouldRejectUnknownField`、`DefaultIntakeConvertPipelineTest.shouldStopWhenParserFails` |
| TEST-TAI-003 | FR-TAI-005、FR-TAI-015 | Passed | `DefaultRequestValidatorTest.shouldReportMissingRequiredField`、`shouldReportNestedFieldPath` |
| TEST-TAI-004 | FR-TAI-005、FR-TAI-006 | Passed | `DefaultIntakeConvertPipelineTest.shouldStopWhenValidationFails`、`shouldApplyTenantSpecificRuleAfterPublicRules` |
| TEST-TAI-005 | FR-TAI-007 | Passed | `DefaultIntakeConvertPipelineTest.shouldReturnCompletePlatformCommand`（测试租户） |
| TEST-TAI-006 | FR-TAI-008 | Passed | `CodeMapperTest.shouldMapCertificateCodesToPlatformEnum`、`shouldMapMaterialCodesToPlatformEnum`；材料编码仅在映射层验证，进入命令待 A-1 决策 |
| TEST-TAI-007 | FR-TAI-009 | Passed | `CodeMapperTest.shouldFailWithFieldPathWhenCodeIsUnknown`、`DefaultIntakeConvertPipelineTest.shouldFailWithFieldPathWhenCertificateCodeUnknown` |
| TEST-TAI-008 | FR-TAI-007、FR-TAI-010 | Passed | `DefaultIntakeConvertPipelineTest.shouldReturnCompletePlatformCommand` |
| TEST-TAI-009 | FR-TAI-001 | Passed | `DefaultIntakeConvertPipelineTest.shouldExecuteParserValidatorConverterInFixedOrder`、`shouldStopWhenParserFails`、`shouldStopWhenValidationFails` |
| TEST-TAI-010 | FR-TAI-011、FR-TAI-013 | Passed | `TenantProtocolHandlerRegistryTest.shouldResolveExactTenantAndProtocolVersion`、`shouldKeepDifferentTenantsIsolated` |
| TEST-TAI-011 | FR-TAI-012、FR-TAI-014 | Passed | `TenantProtocolHandlerRegistryTest.shouldRejectDuplicateProtocolRegistration`、`shouldRejectIncompleteProtocolHandler`、`shouldRejectNullProtocolHandler`、`shouldRejectUnregisteredProtocolWithoutFallback`；`DefaultIntakeConvertPipelineTest.shouldRejectUnregisteredProtocolWithoutFallback` |
| TEST-TAI-012 | FR-TAI-002、FR-TAI-007、FR-TAI-010 | Passed | `DefaultIntakeConvertPipelineTest.shouldRejectWhenTenantContextMissing`、`shouldUseTrustedTenantContextInsteadOfPayloadFields` |
| TEST-TAI-013 | FR-TAI-016 | Passed | `DefaultJsonParserTest.shouldNotLogSensitiveValuesWhenParsingFails`、`DefaultIntakeConvertPipelineTest.shouldNotLogSensitiveValuesWhenConversionFails`、`shouldFailWithFieldPathWhenCertificateCodeUnknown` |
| TEST-TAI-014 | FR-TAI-017 | Passed | `DefaultIntakeConvertPipelineTest.shouldReturnEquivalentCommandsOnRepeatedConversion` |
| TEST-TAI-015 | FR-TAI-011、FR-TAI-012 | Passed | `TenantAdapterArchitectureTest.applicationAndDomainShouldNotDependOnTenantAdapter`、`routingCodeShouldNotBranchOnTenantOrProtocol` |
| TEST-TAI-016 | FR-TAI-018 | Passed | `TenantIntakeContextResolverTest.shouldResolveTrustedContext`、`shouldRejectMissingProtocolVersion`、`shouldRejectInvalidProtocolVersion`、`shouldRejectUntrustedCaller` |
| TEST-TAI-017 | FR-TAI-001、FR-TAI-004、FR-TAI-006、FR-TAI-014、FR-TAI-019 | Passed | `TenantIntakeControllerTest.shouldReturnStableEnvelopeForConvertedPayload`、`shouldClearTenantContextAfterPipeline`、`shouldMapParseFailure`、`shouldMapValidationFailure`、`shouldMapCodeMappingFailure`、`shouldMapUnsupportedProtocol`、`shouldMapMissingTenantContext` |
| TEST-TAI-018 | FR-TAI-003 至 FR-TAI-009、FR-TAI-015 | Passed | `ReferenceTenantAdapterTest.shouldConvertReferencePayload`、`shouldRejectUnknownCertificateCode`、`shouldRejectMissingRelationship`、`shouldUseTrustedContextAndIgnorePayloadTenantFields`、`shouldReturnEquivalentCommandsOnRepeatedConversion`；`ReferenceProtocolHandlerRegistrationTest.shouldRegisterReferenceProtocolInSpringContext` |

## 新增内容与兼容性

- 新增生产包：`com.claim.platform.intake.interfaces.tenant.core`（`pipeline`、`parser`、`validator`、`converter`、`registry`、`context`、`exception`）和 `interfaces.tenant.reference`（`dto`、`validator`、`converter`、协议 Handler）。
- 新增 HTTP 入口：`POST /api/v1/tenant-intakes`，请求头 `X-Claim-Service-Token`、`X-Claim-Tenant`、`X-Claim-Protocol-Version` 必填，`X-Trace-Id` 可选；成功返回 `ApiResult<ClaimIntakeResult>`。
- 新增 `TenantIntakeContextResolver`、`TenantIntakeController`；Holder 只在 Pipeline 调用期间持有，`finally` 清理。
- 新增错误映射：`MALFORMED_REQUEST`、`REQUEST_VALIDATION_FAILED`、`TENANT_PROTOCOL_NOT_SUPPORTED`、`TENANT_CONTEXT_MISSING`。
- 新增领域枚举：`CertificateType`、`IntakeMaterialType`；未修改 `Person.certificateType()` 和 `IntakeMaterial.type()` 的字符串类型。
- 未修改数据库 Schema、领域事件、配置项、MQ、`signNo` 规则和 `SIGN_RECEIVE` 交互类型；既有 `/api/v1/claim-intakes` 不变。
- `reference/v1` 以 Spring Bean 注册，仅用于参考和回归；真实租户接入仍需新增独立组件包。

## 回写规则

实现阶段完成后，必须更新本文件：

- 将对应任务状态改为 Done；
- 填写实际测试名称、测试数量和 `mvn verify` 结果；
- 记录新增的包、类、配置项、错误码和兼容性影响；
- 若真实协议与当前假设不一致，先更新 `requirements.md` 和 `design.md`，再修改代码。
