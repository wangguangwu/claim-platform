# 租户签收报文转换层需求

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | In Progress |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service` |
| 功能分支 | `feat/tenant-adapter-intake` |
| 关联根需求 | `specs/requirements.md` |
| 关联 Spec | `specs/application-intake/requirements.md`、`specs/claim-intake-ddd-package-refactor/requirements.md` |
| 本次交付 | 通用转换框架、默认组件、注册表、Pipeline 与测试租户验证；不包含真实租户适配器、HTTP 接口或数据库变更 |

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
- 解析异常、校验异常、编码映射异常和组件缺失异常的语义；
- 转换层单元测试与架构边界测试要求。

## 非范围

- 验签、解密、鉴权、限流和租户识别；这些能力属于 Gateway 或更前置的可信入口。
- 幂等判断和 `signNo` 生成；现有 Application 用例的 `tenantCode + externalApplyNo` 幂等规则不变。
- 数据库写入、事务、Outbox、MQ、OSS 和下游同步。
- 租户响应转换和 HTTP `ApiResult` 组装。
- 真实租户协议、Tenant DTO、Parser、Validator、Converter 和编码映射表；当前没有可用的真实租户报文。
- 新增或修改 `/api/v1/claim-intakes` 接口、请求体和可信头契约。
- 材料二进制内容的存储、下载或访问。
- 将 `Person`、`IntakeMaterial` 的字符串字段重构为强类型枚举字段；该项如需要，另立领域模型变更 Spec。
- 重命名既有 `signNo` 业务编号和 `SIGN_RECEIVE` 交互类型。

## 核心约束

1. `SubmitIntakeCommand` 是租户协议与核心签收业务之间的唯一边界；进入 Application 层后不得出现任何租户 DTO。
2. `tenantCode`、`protocolVersion` 只来自 `TenantContextHolder`，不得从 Raw Payload、查询参数或路径中读取。
3. 转换层不负责生成 `signNo`，不写数据库，不发消息，不改变应用层幂等状态。
4. 租户差异只能通过组件注册表达；禁止在 Pipeline 或任一阶段出现基于 `tenantCode` 的 `if` 分支或 `switch`。
5. 转换失败必须终止流程，不得返回半成品命令，也不得把租户原始编码回填到标准命令。
6. 日志和异常不得包含 Raw Payload、完整 Tenant DTO、证件号、手机号、银行卡号或其他敏感信息。

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

## 失败行为与错误码

以下映射面向未来的入口适配器；本功能不新增或修改现有 HTTP 接口。

| 场景 | 内部异常 | 对外错误码 | HTTP 状态 | 调用方行为 |
| --- | --- | --- | --- | --- |
| 报文结构非法 | `IntakePayloadParseException` | `MALFORMED_REQUEST` | 400 | 按租户协议修正报文后重试 |
| 公共或租户校验失败 | `IntakeRequestValidationException` | `REQUEST_VALIDATION_FAILED` | 400 | 修正字段后重试 |
| 编码无法映射 | `IntakeCodeMappingException` | `REQUEST_VALIDATION_FAILED` | 400 | 按协议修正编码；平台补充映射后发布新版本 |
| 租户或协议版本未注册 | `TenantProtocolNotSupportedException` | `TENANT_PROTOCOL_NOT_SUPPORTED` | 400 | 核对 `tenantCode`、`protocolVersion`；由平台注册适配器 |
| 缺少可信租户上下文 | `TenantContextMissingException` | `TENANT_CONTEXT_MISSING` | 400 | 经可信入口补齐上下文后重试 |
| 未预期异常 | 无 | `INTERNAL_ERROR` | 500 | 携带 `traceId` 联系平台 |

`TENANT_PROTOCOL_NOT_SUPPORTED` 是本功能新增的稳定错误码，仅在后续入口适配器接入时对外生效。其触发条件是当前租户与协议版本没有完整注册组件，调用方不得通过修改 payload 中的租户或版本字段绕过。

## 幂等与兼容性影响

- 转换层是无状态、只读的协议适配过程，不定义幂等键，不写数据库，不发送消息。
- 现有签发用例仍以 `tenantCode + externalApplyNo` 作为天然幂等键；重复转换不会影响该规则。
- 本功能不修改数据库 Schema、领域事件、配置项、HTTP API 或现有 `SubmitIntakeCommand` 字段。
- 新增类、包和文档统一使用 `Intake` 语义，不新增 `Sign*` 类型；`signNo` 作为既有业务编号保留。
- 既有 `SIGN_RECEIVE` 交互类型不在本次重命名范围内，避免改变已有审计数据语义。
- 组件注册是增量扩展；新增租户或版本不得要求修改既有租户组件或 Pipeline。
- 实现新增的类属于模块内部实现，不构成对外兼容契约。

## 假设与待决事项

### A-1 标准命令中的材料

现有 `SubmitIntakeCommand` 只包含人员信息、外部申请编号和扩展 JSON，不包含材料列表；材料目前通过独立的 `UploadMaterialsCommand` 登记。设计中的 `MaterialMapper` 若要输出到同一个完整命令，需要先决定：

- 扩展 `SubmitIntakeCommand` 并在 Application 签发用例中原子保存材料；或
- 将材料转换保留到独立的材料上传用例，签发命令只承载申请信息。

在真实租户报文确认前不实现材料映射，避免凭空扩展命令和事务边界。

### A-2 枚举与现有字符串字段

`Person.certificateType()` 和 `IntakeMaterial.type()` 当前是字符串。转换层可以先映射到平台枚举，再以 `enum.name()` 写入现有命令字段，确保租户原始编码不进入 Application/Domain。将领域字段改为强类型枚举属于独立领域模型变更，不在本功能范围。

### A-3 XML 支持

默认实现优先提供 JSON Parser。XML Parser、XML 依赖和 XML 安全配置只在真实租户协议要求时引入，不为假设需求提前增加依赖。

### A-4 租户上下文位置与协议版本来源

已确认先限制在 Claim Intake 内部：`TenantContext` 与 `TenantContextHolder` 位于 `interfaces/tenant/core/context`，不进入 `claim-common`，避免扩大共享契约。入口适配器负责从可信 Gateway 头读取上下文、在调用 Pipeline 前写入 Holder，并在 `finally` 中清理。`protocolVersion` 的具体请求头名称、缺省规则和 Gateway 契约仍需在接入 Spec 中确认；转换层不得自行猜测。

### A-5 实现时机

通用组件、默认 Parser/Validator、注册表和 Pipeline 已按明确开发指令实现，并使用测试范围内的租户组件验证扩展机制；真实租户 DTO、Parser、Validator、Converter 和编码表仍等待首批真实报文（见 A-1、A-3）。在入口适配器接入前，Pipeline 暂无生产调用方，这是当前已知限制；接入时必须先完成 TK-TAI-002、TK-TAI-005 和 TK-TAI-006 的门禁。
