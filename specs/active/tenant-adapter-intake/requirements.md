# 租户签收报文转换层需求

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Blocked（reference 参考协议已接入；等待真实租户协议、字段与材料编码映射） |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service` |
| 功能分支 | `feat/tenant-adapter-intake` |
| 关联根需求 | `specs/requirements.md` |
| 关联 Spec | `specs/archive/application-intake/requirements.md`、`specs/archive/claim-intake-ddd-package-refactor/requirements.md` |
| 本次交付 | 通用转换框架、默认组件、注册表、Pipeline、`reference` 参考租户适配器、`POST /api/v1/tenant-intakes` HTTP 入口与异常映射；不包含真实租户协议或数据库变更 |

## 范围

本功能在 Claim Intake 入站侧建立租户协议转换层，把已完成验签、解密后的非标 Raw Payload 转换为平台标准签收命令。

```text
Raw Payload
→ Tenant DTO
→ Validate
→ Convert
→ SubmitIntakeCommand
```

输入：

- 已完成验签、解密和鉴权的 Raw Payload；
- 可信租户上下文，至少包含 `tenantCode`、`protocolVersion`、`traceId`。

输出：

- 完整的 `SubmitIntakeCommand`；
- 命令中只允许出现平台类型和平台标准码，不允许出现租户 DTO、`JsonNode`、XML 节点或租户私有字段类型。

覆盖内容：

- `IntakeConvertPipeline` 固定流程；
- `RequestParser`、`RequestValidator`、`RequestConverter` 契约；
- 默认 Parser、默认 Validator 与租户差异组件；
- 按 `tenantCode + protocolVersion` 匹配的组件注册表；
- `reference` 参考租户适配器（JSON Parser、公共与差异校验、字段映射、证件编码映射）；
- `POST /api/v1/tenant-intakes` HTTP 入口、可信头解析、租户上下文生命周期与异常映射；
- 解析异常、校验异常、编码映射异常和组件缺失异常的语义；
- 转换层单元测试、入口测试与架构边界测试要求。

## 非范围

- 验签、解密、鉴权、限流和租户识别；这些能力属于 Gateway 或更前置的可信入口。
- 幂等判断和 `signNo` 生成；现有 Application 用例的 `tenantCode + externalApplyNo` 幂等规则不变。
- 数据库写入、事务、Outbox、MQ、OSS 和下游同步。
- 租户响应转换和 HTTP `ApiResult` 组装。
- 真实租户协议；当前没有可用的真实租户报文，`reference` 只是打通入口和扩展机制的参考协议，不代表任何真实租户。
- 新增或修改既有 `/api/v1/claim-intakes` 接口和请求体；本功能新增独立的 `/api/v1/tenant-intakes` 入口。
- 材料二进制内容的存储、下载或访问；材料元数据进入标准命令由 `claim-intake-standard-flow` 单独扩展（见 A-1），不在本功能实现。
- 将 `Person`、`IntakeMaterial` 的字符串字段重构为强类型枚举字段；该项如需要，另立领域模型变更 Spec。
- 重命名既有 `signNo` 业务编号和 `SIGN_RECEIVE` 交互类型。

## 核心约束

1. `SubmitIntakeCommand` 是租户协议与核心签收业务之间的唯一边界；进入 Application 层后不得出现任何租户 DTO。
2. `tenantCode`、`protocolVersion` 只来自 `TenantContextHolder`，不得从 Raw Payload、查询参数或路径中读取。
3. 转换层不负责生成 `signNo`，不写数据库，不发消息，不改变应用层幂等状态。
4. 租户差异只能通过组件注册表达；禁止在 Pipeline 或任一阶段出现基于 `tenantCode` 的 `if` 分支或 `switch`。
5. 转换失败必须终止流程，不得返回半成品命令，也不得把租户原始编码回填到标准命令。
6. 日志和异常不得包含 Raw Payload、完整 Tenant DTO、证件号、手机号、银行卡号或其他敏感信息。
7. HTTP 入口必须先校验服务间调用凭证，再从可信头构造 `TenantContext`；调用 Pipeline 前写入 `TenantContextHolder`，并在 `finally` 中清理。
8. HTTP 入口不得信任请求体、查询参数或路径中的 `tenantCode`、`protocolVersion`、`traceId`。

## 验收需求

### FR-TAI-001 固定转换流程

WHEN 转换层接收到已完成验签和解密的 Raw Payload THEN THE SYSTEM SHALL 按 Parser → Validator → Converter 的固定顺序执行，并在任一阶段失败后终止后续阶段。

### FR-TAI-002 可信租户上下文

WHEN 转换层确定当前租户、协议版本和 Trace THEN THE SYSTEM SHALL 只从 `TenantContextHolder` 读取 `tenantCode`、`protocolVersion` 和 `traceId`，不得从 Raw Payload 或请求体中的租户、版本字段推断。

### FR-TAI-003 报文解析

WHEN Raw Payload 符合当前租户协议 THEN THE SYSTEM SHALL 将其解析为当前租户专属 DTO。

### FR-TAI-004 解析失败

WHEN Raw Payload 不符合当前租户报文结构 THEN THE SYSTEM SHALL 抛出可识别的报文解析异常，并且不执行字段校验和命令转换。

### FR-TAI-005 公共与租户校验

WHEN 租户 DTO 解析完成 THEN THE SYSTEM SHALL 先执行平台公共校验，再执行当前租户声明的差异校验。

### FR-TAI-006 校验失败

WHEN 租户 DTO 未通过任一校验规则 THEN THE SYSTEM SHALL 抛出可识别的输入校验异常，并且不执行命令转换。

### FR-TAI-007 完整命令

WHEN 租户 DTO 通过校验 THEN THE SYSTEM SHALL 通过一次 Converter 调用返回完整 `SubmitIntakeCommand`，其中 `tenantCode` 和 `traceId` 来自可信租户上下文，且不得返回需要其他组件补全的半成品。

### FR-TAI-008 编码收敛

WHEN Converter 处理租户材料编码或证件类型编码 THEN THE SYSTEM SHALL 将其转换为平台标准枚举或平台标准码。

### FR-TAI-009 未知编码

WHEN 租户编码在平台映射中不存在 THEN THE SYSTEM SHALL 终止转换并报告字段路径，不得把租户原始编码透传到命令、Application 或 Domain。

### FR-TAI-010 边界隔离

WHEN 转换结果进入 Application 层 THEN THE SYSTEM SHALL 只包含 `SubmitIntakeCommand` 和平台类型，不得包含 Tenant DTO、JSON/XML 节点或租户私有类型。

### FR-TAI-011 无侵入扩展

WHEN 新增租户或新增协议版本 THEN THE SYSTEM SHALL 允许通过新增并注册租户组件完成接入，不得修改 `IntakeConvertPipeline` 主流程。

### FR-TAI-012 禁止租户分支

WHEN 转换层需要处理租户差异 THEN THE SYSTEM SHALL 仅通过注册表匹配选择组件，不得使用基于 `tenantCode` 或 `protocolVersion` 的 `if` 分支或 `switch`。

### FR-TAI-013 版本并存

WHEN 同一租户注册多个 `protocolVersion` THEN THE SYSTEM SHALL 按 `tenantCode + protocolVersion` 精确匹配，不同版本互不覆盖。

### FR-TAI-014 组件缺失

WHEN 当前 `tenantCode + protocolVersion` 缺少完整组件 THEN THE SYSTEM SHALL 快速失败并给出可诊断错误，不得回退到其他租户或其他协议版本。

### FR-TAI-015 默认能力复用

WHEN 多个租户的报文格式或公共校验规则一致 THEN THE SYSTEM SHALL 允许复用默认 Parser 和默认 Validator，租户只实现存在差异的组件。

### FR-TAI-016 安全日志

WHEN 转换层记录日志或构造异常 THEN THE SYSTEM SHALL 只输出 `tenantCode`、`protocolVersion`、`traceId`、字段路径和异常类别，不得输出 Raw Payload、完整 Tenant DTO、证件号、手机号或银行卡号。

### FR-TAI-017 无副作用

WHEN 同一 Raw Payload 和同一租户上下文被重复转换 THEN THE SYSTEM SHALL 返回等价命令，且不得生成 `signNo`、写数据库、发送消息或改变幂等状态。

### FR-TAI-018 可信 HTTP 入口

WHEN 租户报文请求进入 `POST /api/v1/tenant-intakes` THEN THE SYSTEM SHALL 先校验 `X-Claim-Service-Token`，再仅从 `X-Claim-Tenant`、`X-Claim-Protocol-Version` 和可选 `X-Trace-Id` 构造租户上下文，在调用 Pipeline 前写入 `TenantContextHolder`，并在 `finally` 中清理。

### FR-TAI-019 入口失败响应

WHEN HTTP 入口的上下文解析或报文转换失败 THEN THE SYSTEM SHALL 返回 `ApiResult` 失败信封、稳定错误码和 `traceId`，且响应和日志不得包含原始报文、字段值或异常堆栈详情。

## HTTP 入口契约

本功能新增独立入口，不修改既有 `/api/v1/claim-intakes`。

| 项目 | 契约 |
| --- | --- |
| 方法 | `POST` |
| 路径 | `/api/v1/tenant-intakes` |
| 请求体 | 当前租户协议的 Raw Payload；`reference` 协议使用 JSON |
| 成功响应 | HTTP 200，`ApiResult<ClaimIntakeResult>`，包含 `signNo` 和 `status` |
| 幂等 | 复用 `ClaimIntakeCommandService.submit` 的 `tenantCode + externalApplyNo` 规则 |

请求头：

| 请求头 | 必填 | 规则 |
| --- | --- | --- |
| `X-Claim-Service-Token` | 是 | 与现有服务间调用凭证一致，复用 `GatewayRequestContextResolver` |
| `X-Claim-Tenant` | 是 | `[A-Za-z0-9_-]{1,64}`，只来自可信 Gateway |
| `X-Claim-Protocol-Version` | 是 | `[A-Za-z0-9._-]{1,32}`，不提供缺省版本，不做跨版本回退 |
| `X-Trace-Id` | 否 | 缺失时由服务生成 UUID |

入口不接受请求体、查询参数或路径中的租户和协议版本字段；请求体中的同名字段不会参与路由。

## 参考租户协议

`reference` 是用于打通入口、验证扩展机制和回归测试的参考协议，不是任何真实租户协议。真实租户接入时必须新增独立租户包和编码表，不得复用 `reference` 的键冒充真实租户。

| 项目 | 内容 |
| --- | --- |
| `tenantCode` | `reference` |
| `protocolVersion` | `v1` |
| 报文格式 | JSON，未知字段拒绝 |
| 顶层字段 | `externalApplyNo`、`applicant`、`accidentDate`、`extension`；可携带 `tenantCode`、`protocolVersion`，但转换层忽略 |
| 申请人字段 | `name`、`certificateType`、`certificateNo`、`mobile`、`gender`、`birthDate`、`relationshipToInsured` |
| 证件编码 | `IDCARD` → `CertificateType.ID_CARD`，`PASSPORT` → `CertificateType.PASSPORT` |
| 扩展 JSON | 仅由 `accidentDate` 和协议声明的 `extension` 构造，不序列化完整 DTO |
| 材料 | 本功能不映射；标准命令的材料元数据由 `claim-intake-standard-flow` 扩展，`reference` 传空列表（见 A-1） |

## 失败行为与错误码

以下映射面向未来的入口适配器；本功能不新增或修改现有 HTTP 接口。

| 场景 | 内部异常 | 对外错误码 | HTTP 状态 | 调用方行为 |
| --- | --- | --- | --- | --- |
| 报文结构非法 | `IntakePayloadParseException` | `MALFORMED_REQUEST` | 400 | 按租户协议修正报文后重试 |
| 公共或租户校验失败 | `IntakeRequestValidationException` | `REQUEST_VALIDATION_FAILED` | 400 | 修正字段后重试 |
| 编码无法映射 | `IntakeCodeMappingException` | `REQUEST_VALIDATION_FAILED` | 400 | 按协议修正编码；平台补充映射后发布新版本 |
| 租户或协议版本未注册 | `TenantProtocolNotSupportedException` | `TENANT_PROTOCOL_NOT_SUPPORTED` | 400 | 核对 `tenantCode`、`protocolVersion`；由平台注册适配器 |
| 缺少可信租户上下文 | `TenantContextMissingException` | `TENANT_CONTEXT_MISSING` | 400 | 经可信入口补齐上下文后重试 |
| 协议版本头格式非法 | `IllegalArgumentException` | `REQUEST_VALIDATION_FAILED` | 400 | 按 `[A-Za-z0-9._-]{1,32}` 修正后重试 |
| 未预期异常 | 无 | `INTERNAL_ERROR` | 500 | 携带 `traceId` 联系平台 |

`TENANT_PROTOCOL_NOT_SUPPORTED` 在 `/api/v1/tenant-intakes` 入口生效，既有 `/api/v1/claim-intakes` 行为不变。其触发条件是当前租户与协议版本没有完整注册组件，调用方不得通过修改 payload 中的租户或版本字段绕过。

## 幂等与兼容性影响

- 转换层是无状态、只读的协议适配过程，不定义幂等键，不写数据库，不发送消息。
- 现有签发用例仍以 `tenantCode + externalApplyNo` 作为天然幂等键；重复转换不会影响该规则。
- 本功能不修改数据库 Schema、领域事件、配置项或现有 `SubmitIntakeCommand` 字段；新增独立 HTTP 入口 `/api/v1/tenant-intakes`，既有 `/api/v1/claim-intakes` 的路径、请求体和错误行为不变。
- `reference` 租户通过组件注册接入，不修改 Pipeline、注册表或既有租户组件；真实租户新增时按同一方式注册。
- 新增类、包和文档统一使用 `Intake` 语义，不新增 `Sign*` 类型；`signNo` 作为既有业务编号保留。
- 既有 `SIGN_RECEIVE` 交互类型不在本次重命名范围内，避免改变已有审计数据语义。
- 组件注册是增量扩展；新增租户或版本不得要求修改既有租户组件或 Pipeline。
- 实现新增的类属于模块内部实现，不构成对外兼容契约。

## 假设与待决事项

### A-1 标准命令中的材料（已解决，2026-10-23）

`SubmitIntakeCommand` 已由 `specs/active/claim-intake-standard-flow/` 扩展，新增不可变 `materials` 列表，并在同一创建事务中与申请、初始状态和 `SIGN_RECEIVE` 日志一起写入。材料非空时初始状态为 `IMAGE_UPLOADED`，为空时为 `IMAGE_NOT_UPLOAD`。

本功能的 `reference` 适配器和既有 HTTP 入口均传空列表，不映射材料；真实租户报文中的材料字段仍需在获得首批真实报文后，以独立变更补齐 Parser、Validator、Converter 和编码表。材料二进制内容的上传、存储和下载仍不在上述两个功能的范围内。

### A-2 枚举与现有字符串字段

`Person.certificateType()` 和 `IntakeMaterial.type()` 当前是字符串。转换层可以先映射到平台枚举，再以 `enum.name()` 写入现有命令字段，确保租户原始编码不进入 Application/Domain。将领域字段改为强类型枚举属于独立领域模型变更，不在本功能范围。

### A-3 XML 支持

默认实现优先提供 JSON Parser。XML Parser、XML 依赖和 XML 安全配置只在真实租户协议要求时引入，不为假设需求提前增加依赖。

### A-4 租户上下文位置与协议版本来源

已确认先限制在 Claim Intake 内部：`TenantContext` 与 `TenantContextHolder` 位于 `interfaces/tenant/core/context`，不进入 `claim-common`，避免扩大共享契约。入口适配器负责从可信 Gateway 头读取上下文、在调用 Pipeline 前写入 Holder，并在 `finally` 中清理。

`protocolVersion` 的可信来源确定为请求头 `X-Claim-Protocol-Version`：必填，格式 `[A-Za-z0-9._-]{1,32}`，不提供缺省版本，不做跨版本回退。`tenantCode` 复用 `X-Claim-Tenant`，`traceId` 复用可选 `X-Trace-Id`，调用方凭证复用 `X-Claim-Service-Token`。Gateway 必须清除客户端伪造的上述头后再透传，本服务不信任请求体中的租户和版本字段。

### A-5 实现时机

通用组件、默认 Parser/Validator、注册表和 Pipeline 已实现；`reference` 参考租户适配器与 `POST /api/v1/tenant-intakes` 入口已接入，Pipeline 已有生产调用方。真实租户 DTO、Parser、Validator、Converter 和编码表仍等待首批真实报文（见 A-1、A-3），`reference` 不能替代真实租户验收。材料进入签发命令已按 A-1 解决，但真实租户材料映射仍未实现。
