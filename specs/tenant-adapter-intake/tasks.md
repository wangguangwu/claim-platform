# 租户签收报文转换层任务

## 状态说明

| 状态 | 含义 |
| --- | --- |
| Done | 已完成并有可核对证据 |
| In Progress | 部分完成，剩余工作受外部契约或决策阻塞 |
| Planned | 已拆分，等待实现条件 |
| Blocked | 缺少真实租户协议、入口契约或前置决策，暂不能实现 |

当前进度：通用转换框架、默认 Parser/Validator、注册表、Pipeline 和测试已完成；真实租户适配器（TK-TAI-005）和 HTTP 入口（TK-TAI-006）仍受真实协议与入口契约阻塞。

## 任务清单

| 任务 | 关联需求与设计 | 状态 | 依赖 | 完成证据或阻塞原因 |
| --- | --- | --- | --- | --- |
| TK-TAI-001 建立转换层 Spec 并统一 Intake 命名 | FR-TAI-001 至 FR-TAI-017；DS 第 1、2、11 节 | Done | 无 | 本目录 `requirements.md`、`design.md`、`tasks.md` 已建立；新类名使用 `IntakeConvertPipeline`、`TenantProtocolHandler`、`SubmitIntakeCommand`，不新增 `Sign*` 类型 |
| TK-TAI-002 确认租户上下文与协议版本来源 | FR-TAI-002、FR-TAI-012、FR-TAI-016；DS 第 7 节 | In Progress | Gateway/入口接入契约 | 已在 `interfaces/tenant/core/context` 落地 `TenantContext`、`TenantContextHolder`，并确定暂不进入 `claim-common`；`protocolVersion` 的可信头名称、格式、缺省规则和请求结束清理仍待入口 Spec 确认 |
| TK-TAI-003 实现转换契约与默认 Parser/Validator | FR-TAI-001、FR-TAI-003 至 FR-TAI-007、FR-TAI-015；DS 第 3、5 节 | Done | TK-TAI-002、首批真实租户报文 | 已实现 `RequestParser`、`RequestValidator`、`RequestConverter`、`DefaultJsonParser`、`DefaultRequestValidator` 和 `CodeMapper`；默认组件不绑定真实租户，测试见 TEST-TAI-001 至 TEST-TAI-009 |
| TK-TAI-004 实现组件注册表与启动校验 | FR-TAI-011 至 FR-TAI-014；DS 第 4 节 | Done | TK-TAI-003 | 已实现 `TenantProtocolKey`、`TenantProtocolHandler`、`TenantProtocolHandlerRegistry`；重复注册、组件不完整和未注册协议均在测试中验证（TEST-TAI-010、TEST-TAI-011） |
| TK-TAI-005 实现首批租户 Parser、Validator、Converter 和编码映射 | FR-TAI-003 至 FR-TAI-009、FR-TAI-015；DS 第 5、6 节 | Blocked | 真实租户报文、字段说明、材料与证件编码表 | 测试范围内已提供 `interfaces/tenant/support` 组件验证扩展机制；生产 Tenant DTO、样例报文和正式编码表仍缺失，未写入生产包 |
| TK-TAI-006 接入入口并完成异常映射 | FR-TAI-001、FR-TAI-004、FR-TAI-006、FR-TAI-014、FR-TAI-016；DS 第 8 节 | Blocked | TK-TAI-002、TK-TAI-005、入口 API Spec | 本功能不新增 HTTP 接口；`TENANT_PROTOCOL_NOT_SUPPORTED` 仍仅作为内部异常语义，未进入对外响应 |
| TK-TAI-007 补充转换层单元测试和架构边界测试 | TEST-TAI-001 至 TEST-TAI-015；DS 第 10 节 | Done | TK-TAI-003 至 TK-TAI-006 | 新增 6 个测试类、36 个用例，覆盖 TEST-TAI-001 至 TEST-TAI-015；架构边界采用源码扫描测试，未引入 ArchUnit |
| TK-TAI-008 执行构建与回归验证 | TEST-TAI-001 至 TEST-TAI-015 | Done | TK-TAI-007 | `mvn -pl backend/claim-intake-service -am verify` BUILD SUCCESS；claim-intake-service 55 个测试全部通过，既有受理、持久化和控制器测试无回归 |

## 实现前门禁

当前状态：通用框架已在明确开发指令下先行实现；下列真实租户和入口门禁仍未满足，TK-TAI-005、TK-TAI-006 不得开始。

- [ ] 已取得至少一个真实租户的样例报文、字段说明和协议版本号。
- [ ] 已确认材料、证件类型的平台标准枚举值与租户编码映射表。
- [ ] 已确认 `SubmitIntakeCommand` 是否承载材料；若承载，先补充 Application 事务和用例设计。
- [ ] 已确认 `protocolVersion` 的可信来源、请求头名称和 Gateway 透传契约。
- [ ] 已确认 `TenantContextHolder` 的写入时机和清理方式；Holder 已确定位于 `interfaces/tenant/core/context`，暂不进入 `claim-common`。
- [ ] 已确认入口 API 的路径、鉴权、错误码和兼容性影响。
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

## 新增内容与兼容性

- 新增生产包：`com.claim.platform.intake.interfaces.tenant.core`，包含 `pipeline`、`parser`、`validator`、`converter`、`registry`、`context`、`exception`。
- 新增领域枚举：`CertificateType`、`IntakeMaterialType`；未修改 `Person.certificateType()` 和 `IntakeMaterial.type()` 的字符串类型。
- 未新增或修改 HTTP API、数据库 Schema、领域事件、配置项、MQ、`signNo` 规则和 `SIGN_RECEIVE` 交互类型。
- `DefaultIntakeConvertPipeline` 与 `TenantProtocolHandlerRegistry` 以 Spring Bean 形式注册；当前没有生产调用方，接入入口前不会改变既有 `/api/v1/claim-intakes` 行为。
- `TENANT_PROTOCOL_NOT_SUPPORTED` 仍为内部异常语义，尚未进入对外错误码契约。

## 回写规则

实现阶段完成后，必须更新本文件：

- 将对应任务状态改为 Done；
- 填写实际测试名称、测试数量和 `mvn verify` 结果；
- 记录新增的包、类、配置项、错误码和兼容性影响；
- 若真实协议与当前假设不一致，先更新 `requirements.md` 和 `design.md`，再修改代码。
