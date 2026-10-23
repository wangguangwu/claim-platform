# 租户签收报文转换层设计

> 状态：In Progress。通用框架、默认组件、注册表和 Pipeline 已实现并通过测试租户验证；真实租户适配器与入口仍待接入。

## 1. 目标与边界

转换层位于 Gateway 验签、解密之后，Application 用例之前，负责把租户非标报文收敛为平台标准签收命令。

```text
Gateway 验签/解密
        ↓
Raw Payload + TenantContext
        ↓
interfaces/tenant
        ├─ RequestParser
        ├─ RequestValidator
        └─ RequestConverter
        ↓
SubmitIntakeCommand
        ↓
application/command → ClaimIntakeCommandService
```

转换层负责：

- 将 Raw Payload 解析为当前租户 DTO；
- 执行公共校验和租户差异校验；
- 将 Tenant DTO 完整转换为 `SubmitIntakeCommand`；
- 将材料、证件等租户编码收敛为平台标准枚举或标准码；
- 按 `tenantCode + protocolVersion` 选择租户组件。

转换层不负责：

- 验签、解密、鉴权、限流和租户识别；
- 幂等判断和 `signNo` 生成；
- 数据库写入、事务、Outbox、MQ、OSS 和下游同步；
- 租户响应转换和 HTTP `ApiResult` 组装。

核心边界：

> `SubmitIntakeCommand` 是租户协议和核心签收业务之间的唯一边界。进入 Application 层后不得出现任何租户 DTO、JSON/XML 节点或租户私有类型。

## 2. 分层与包结构

转换层属于入站协议适配，放在 `interfaces/tenant`，依赖方向保持 `interfaces → application → domain`。它不进入 `domain`，也不下沉到 `infrastructure`。

```text
com.claim.platform.intake
├─ application
│  └─ command
│     └─ SubmitIntakeCommand
├─ domain
│  └─ model
│     ├─ CertificateType
│     └─ IntakeMaterialType
└─ interfaces
   └─ tenant
      ├─ core
      │  ├─ pipeline
      │  │  ├─ IntakeConvertPipeline
      │  │  └─ DefaultIntakeConvertPipeline
      │  ├─ parser
      │  │  ├─ RequestParser
      │  │  └─ DefaultJsonParser
      │  ├─ validator
      │  │  ├─ RequestValidator
      │  │  └─ DefaultRequestValidator
      │  ├─ converter
      │  │  ├─ RequestConverter
      │  │  └─ CodeMapper
      │  ├─ context
      │  │  ├─ TenantContext
      │  │  └─ TenantContextHolder
      │  ├─ registry
      │  │  ├─ TenantProtocolKey
      │  │  ├─ TenantProtocolHandler
      │  │  └─ TenantProtocolHandlerRegistry
      │  └─ exception
      │     ├─ IntakeConversionException
      │     ├─ IntakePayloadParseException
      │     ├─ IntakeRequestValidationException
      │     ├─ IntakeCodeMappingException
      │     └─ TenantProtocolNotSupportedException
      └─ <tenant-alias>
         ├─ dto
         ├─ parser
         ├─ validator
         ├─ converter
         └─ mapper
```

约束：

- `<tenant-alias>` 使用小写、可安全作为 Java 包名的租户别名，不直接使用可能包含大写、连字符或下划线的原始 `tenantCode`。
- 租户 DTO 只能存在于 `interfaces/tenant/<tenant-alias>/dto`，不得被 `application` 或 `domain` 引用。
- `application` 只依赖 `SubmitIntakeCommand`，不依赖任何 `interfaces/tenant` 类型。
- `domain` 不依赖 Parser、Validator、Converter、Jackson、Jakarta Validation 或任何租户协议类型。
- `CertificateType`、`IntakeMaterialType` 是平台标准枚举，放在 `domain/model`；转换层负责映射，领域字段的强类型化另行设计。
- `TenantContext`、`TenantContextHolder` 先放在 `interfaces/tenant/core/context`，暂不进入 `claim-common`，避免扩大共享契约；入口适配器负责写入与清理。

## 3. 核心契约

### 3.1 IntakeConvertPipeline

```java
public interface IntakeConvertPipeline {

    SubmitIntakeCommand execute(String payload);
}
```

Pipeline 只做三件事：

1. 从 `TenantContextHolder` 获取可信上下文；
2. 从注册表解析当前租户组件；
3. 按 Parser → Validator → Converter 顺序执行。

Pipeline 不包含租户判断、字段映射、编码转换或异常到 HTTP 的映射。

### 3.2 RequestParser

```java
public interface RequestParser<T> {

    T parse(String payload);
}
```

要求：

- 只负责报文结构解析；
- JSON 等公共格式复用默认实现；
- 解析失败统一抛出 `IntakePayloadParseException`，保留原始异常；
- 不执行字段校验，不构造 `SubmitIntakeCommand`，不读写数据库。

### 3.3 RequestValidator

```java
public interface RequestValidator<T> {

    void validate(T request);
}
```

要求：

- 公共必填、格式和字段组合规则由 `DefaultRequestValidator<T>` 基于 Jakarta Bean Validation 完成；
- 公共字段规则优先落在平台共享的 DTO 视图、注解约束或默认校验规则集中，避免每个租户重复实现；
- 租户 Validator 先调用默认校验，再追加差异规则；
- 校验失败统一抛出 `IntakeRequestValidationException`；
- 不修改 DTO，不执行模型转换。

默认校验器示例：

```java
public class DefaultRequestValidator<T> implements RequestValidator<T> {

    private final Validator validator;

    @Override
    public void validate(T request) {
        if (request == null) {
            throw new IntakeRequestValidationException("request");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IntakeRequestValidationException(firstFieldPath(violations));
        }
    }
}
```

租户扩展示例：

```java
public class TenantARequestValidator implements RequestValidator<TenantAIntakePayload> {

    private final DefaultRequestValidator<TenantAIntakePayload> defaultValidator;

    @Override
    public void validate(TenantAIntakePayload request) {
        defaultValidator.validate(request);
        // 仅追加 Tenant A 的差异规则
    }
}
```

### 3.4 RequestConverter

```java
public interface RequestConverter<T> {

    SubmitIntakeCommand convert(T request, TenantContext context);
}
```

`TenantContext` 是不可变快照，Converter 只用它填充命令的 `tenantCode`、`traceId` 等可信元数据；协议字段仍然只能来自 Tenant DTO，不得从上下文伪造业务字段。

要求：

- 一次调用返回完整命令，不返回半成品；
- 租户字段、材料编码和证件类型必须在 Converter 内完成收敛；
- 不允许把租户原始编码写入命令；
- 未知编码抛出 `IntakeCodeMappingException`，并携带字段路径；
- `extJson` 只允许从协议声明的扩展字段构造，不得直接序列化 Tenant DTO，避免租户私有字段穿透；
- Converter 可以组合 `ApplicantMapper`、`AccidentPersonMapper`、`CertificateTypeMapper` 和 `CodeMapper`；`MaterialMapper` 是否参与签发命令取决于第 12 节 A-1 的材料归属决策。

Converter 是租户差异最集中的阶段，不提供“字段全自动复制”的默认兜底。字段完全一致的租户可以复用显式的共享 Mapper，但必须由该租户组件声明，避免静默映射错误。

### 3.5 组件包

每个租户提供一个完整组件包，由注册表统一持有：

```java
public interface TenantProtocolHandler<T> {

    TenantProtocolKey key();

    RequestParser<T> parser();

    RequestValidator<T> validator();

    RequestConverter<T> converter();
}
```

```java
public record TenantProtocolKey(String tenantCode, String protocolVersion) {
}
```

`TenantProtocolKey` 在构造时校验非空和格式；键一旦注册不可变。

## 4. 路由与注册

### 4.1 精确匹配

```text
TenantContextHolder
├─ tenantCode
└─ protocolVersion
        ↓
TenantProtocolHandlerRegistry
        ↓
TenantProtocolHandler<T>
├─ Parser
├─ Validator
└─ Converter
```

匹配规则：

- 只做 `tenantCode + protocolVersion` 精确匹配；
- 不提供默认租户、默认版本或跨版本回退；
- 同一键重复注册直接启动失败；
- Parser、Validator、Converter 任一缺失直接启动失败；
- 键不存在时抛出 `TenantProtocolNotSupportedException`，不得使用其他租户组件兜底。

### 4.2 注册方式

租户组件以 Spring Bean 形式提供，注册表在启动时收集全部 `TenantProtocolHandler` 并构建不可变映射：

```java
@Component
public class TenantAProtocolHandler implements TenantProtocolHandler<TenantAIntakePayload> {

    private final RequestParser<TenantAIntakePayload> parser = new TenantAParser();
    private final RequestValidator<TenantAIntakePayload> validator;
    private final RequestConverter<TenantAIntakePayload> converter = new TenantARequestConverter();

    public TenantAProtocolHandler(Validator validator) {
        this.validator = new TenantARequestValidator(new DefaultRequestValidator<>(validator));
    }

    @Override
    public TenantProtocolKey key() {
        return new TenantProtocolKey("TENANT_A", "v1");
    }

    @Override
    public RequestParser<TenantAIntakePayload> parser() {
        return parser;
    }

    @Override
    public RequestValidator<TenantAIntakePayload> validator() {
        return validator;
    }

    @Override
    public RequestConverter<TenantAIntakePayload> converter() {
        return converter;
    }
}
```

`TenantProtocolHandler<T>` 只负责绑定一个协议键和三个组件，不承担租户判断或流程决策。Pipeline 仍然按固定顺序调用 Parser、Validator 和 Converter。无论采用组件包还是其他等价结构，类型擦除只允许在一个位置处理，不得在各阶段重复强转。

### 4.3 新增租户或版本

```text
新增 Tenant DTO
├─ 必要时新增 Parser
├─ 必要时新增 Validator
├─ 实现 Converter
├─ 注册 tenantCode + protocolVersion
└─ 补充单元测试
```

不得修改：

- `IntakeConvertPipeline` 主流程；
- 既有租户组件；
- 既有协议版本行为。

### 4.4 禁止事项

```java
// 禁止
if ("TENANT_A".equals(tenantCode)) {
    return tenantAParser.parse(payload);
}
```

```java
// 禁止
switch (tenantCode) {
    case "TENANT_A" -> ...
    case "TENANT_B" -> ...
}
```

所有租户差异通过组件注册完成。

## 5. 默认组件与租户差异

| 阶段 | 默认实现 | 租户差异 |
| --- | --- | --- |
| Parser | `DefaultJsonParser<T>` 基于 Jackson，按目标类型反序列化 | 非 JSON 报文、特殊结构或特殊日期格式由租户 Parser 实现 |
| Validator | `DefaultRequestValidator<T>` 执行 Jakarta Bean Validation | 租户特殊必填、字段组合和协议规则由租户 Validator 追加 |
| Converter | 无通用兜底 | 每个租户显式实现字段映射和编码映射 |

JSON Parser 要求：

- 使用项目现有 Jackson 能力，不新增重复 JSON 库；
- 按租户协议显式配置未知字段策略，默认拒绝未知字段，避免租户字段被静默丢弃；
- 解析异常只暴露异常类别和字段路径，不输出原始报文；
- 不把 `JsonNode` 传入 Converter 之后的任何层。

XML 支持只在真实租户协议要求时引入，并同时确认 XML 外部实体、DTD 和深度限制等安全配置。

## 6. 编码映射

### 6.1 平台标准枚举

```java
public enum CertificateType {
    ID_CARD,
    PASSPORT
}
```

```java
public enum IntakeMaterialType {
    ID_CARD,
    MEDICAL_INVOICE,
    DIAGNOSIS_CERTIFICATE
}
```

枚举值以真实业务协议确认结果为准，文档中的值只用于说明映射方式。材料映射进入签发命令还是独立上传用例，取决于第 12 节 A-1 的决策。

### 6.2 映射规则

```text
01     → IntakeMaterialType.ID_CARD
IDCARD → CertificateType.ID_CARD
```

要求：

- 映射表由租户 Converter 或租户 Mapper 显式声明；
- 映射失败必须报错，不得使用 `valueOf`、字符串透传或“未知即原值”兜底；
- 映射异常携带字段路径，例如 `applicant.certificateType`；
- 同一租户不同协议版本的映射表可以不同，不能跨版本复用未声明的映射；
- 映射表优先使用不可变 `Map` 或 `EnumMap`，在组件构造时完成校验。

### 6.3 与现有领域字段的衔接

当前 `Person.certificateType()` 和 `IntakeMaterial.type()` 是字符串。Converter 可以：

1. 将租户编码映射为平台枚举；
2. 将 `enum.name()` 写入现有命令字段；
3. 保证命令中不再出现租户原始编码。

该方式不改变现有领域字段类型。将字段改为枚举类型需要单独的领域模型变更 Spec，并同步持久化、请求和测试。

## 7. 租户上下文

```java
public record TenantContext(String tenantCode, String protocolVersion, String traceId) {
}
```

`TenantContext` 与 `TenantContextHolder` 先位于 `interfaces/tenant/core/context`，不进入 `claim-common`，由入口适配器负责：

1. 从可信 Gateway 头读取 `tenantCode`、`protocolVersion` 和 `traceId`；
2. 在调用 Pipeline 前写入 Holder；
3. 在 `finally` 中清理，避免线程池复用导致租户串扰；
4. 上下文缺失或格式非法时在进入 Pipeline 前拒绝请求。

Pipeline 在入口处解析一次上下文；Parser 和 Validator 不重复接收它，Converter 接收同一个不可变快照，只用于填充命令的可信元数据。任何阶段都不从 payload 读取 `tenantCode`、`protocolVersion` 或 `traceId`。

`protocolVersion` 的具体请求头名称、缺省值、版本格式和 Gateway 透传契约尚未确定，必须在入口接入 Spec 中明确。转换层只消费上下文，不定义协议版本来源。

当前通用框架只提供 Holder 与 Pipeline 读取；真实入口适配器的写入、`finally` 清理和协议版本校验随 TK-TAI-006 实现。Pipeline 在缺少上下文时复用现有 `TenantContextMissingException`，入口接入时由统一异常处理器映射为 `TENANT_CONTEXT_MISSING`。

## 8. 失败处理与可观测性

### 8.1 异常层次

```text
IntakeConversionException
├─ IntakePayloadParseException
├─ IntakeRequestValidationException
└─ IntakeCodeMappingException

TenantProtocolNotSupportedException
```

- 解析、校验和编码映射异常都属于可预期输入失败，不打印完整堆栈到业务日志，除非需要保留根因；
- 未预期异常保留原始异常链，由入口适配器统一映射为 `INTERNAL_ERROR`；
- 异常消息只包含字段路径、租户、协议版本和异常类别，不包含字段值或原始报文。

### 8.2 日志字段

允许：

```text
tenantCode
protocolVersion
traceId
stage（parse / validate / convert）
fieldPath
exceptionType
```

禁止：

```text
Raw Payload
完整 Tenant DTO
证件号
手机号
银行卡号
材料内容
访问令牌或密钥
```

### 8.3 HTTP 映射边界

本功能不新增 HTTP 入口。未来入口适配器按以下稳定错误码映射：

| 异常 | 错误码 | HTTP 状态 |
| --- | --- | --- |
| `IntakePayloadParseException` | `MALFORMED_REQUEST` | 400 |
| `IntakeRequestValidationException` | `REQUEST_VALIDATION_FAILED` | 400 |
| `IntakeCodeMappingException` | `REQUEST_VALIDATION_FAILED` | 400 |
| `TenantProtocolNotSupportedException` | `TENANT_PROTOCOL_NOT_SUPPORTED` | 400 |
| `TenantContextMissingException` | `TENANT_CONTEXT_MISSING` | 400 |
| 其他未预期异常 | `INTERNAL_ERROR` | 500 |

`TENANT_PROTOCOL_NOT_SUPPORTED` 在接入入口前只作为内部异常语义存在，不对现有 API 生效。

## 9. 并发、幂等与数据归属

- Pipeline、注册表、Parser、Validator、Converter 和映射表在启动后保持不可变，可被并发请求共享。
- 转换层不持有请求级可变状态；租户上下文通过 ThreadLocal 隔离，并由入口适配器清理。
- 转换层不写数据库、不写 Redis、不发送 MQ、不生成 `signNo`，因此不需要分布式锁或幂等表。
- 转换层没有本地写入或外部副作用，失败时只终止当前请求，不存在需要回滚或补偿的状态。
- 签发用例的幂等键仍是 `tenantCode + externalApplyNo`；转换层重复执行不改变该幂等结果。
- 数据所有权不变：申请、材料和交互日志仍由 Claim Intake 的 Application 与 Infrastructure 写入。

## 10. 测试策略

| 测试 | 覆盖需求 | 内容 |
| --- | --- | --- |
| TEST-TAI-001 | FR-TAI-003 | 合法 JSON 解析为对应 Tenant DTO |
| TEST-TAI-002 | FR-TAI-004 | 非法报文抛出解析异常，Validator/Converter 不被调用 |
| TEST-TAI-003 | FR-TAI-005、FR-TAI-015 | 公共必填字段缺失时由默认 Validator 拒绝 |
| TEST-TAI-004 | FR-TAI-005、FR-TAI-006 | 租户特殊规则失败时抛出校验异常 |
| TEST-TAI-005 | FR-TAI-007 | Converter 完成人员、扩展信息和协议已声明字段的映射 |
| TEST-TAI-006 | FR-TAI-008 | 材料编码 `01` 映射为 `IntakeMaterialType.ID_CARD`，证件编码 `IDCARD` 映射为 `CertificateType.ID_CARD` |
| TEST-TAI-007 | FR-TAI-009 | 未知编码抛出映射异常，命令中不出现原始编码 |
| TEST-TAI-008 | FR-TAI-007、FR-TAI-010 | 转换结果为完整命令，且不包含租户 DTO 或 JSON 节点 |
| TEST-TAI-009 | FR-TAI-001 | Pipeline 按固定顺序执行，Parser 或 Validator 失败后不再调用后续阶段 |
| TEST-TAI-010 | FR-TAI-011、FR-TAI-013 | 不同租户、不同协议版本使用各自组件，互不覆盖 |
| TEST-TAI-011 | FR-TAI-012、FR-TAI-014 | 组件缺失、重复注册或组件不完整时快速失败 |
| TEST-TAI-012 | FR-TAI-002、FR-TAI-007、FR-TAI-010 | 缺少租户上下文时拒绝执行；payload 中的租户字段不生效；命令的 `tenantCode`、`traceId` 来自上下文 |
| TEST-TAI-013 | FR-TAI-016 | 日志和异常不包含原始报文、证件号、手机号等敏感信息 |
| TEST-TAI-014 | FR-TAI-017 | 重复转换返回等价命令，且无数据库、MQ 或幂等副作用 |
| TEST-TAI-015 | FR-TAI-011、FR-TAI-012 | 架构测试：`application`/`domain` 不依赖 `interfaces.tenant`；不存在基于 `tenantCode` 的分支或 `switch` |

测试落地建议：

- 使用 JUnit 5、AssertJ 和现有 Mockito 技术栈；
- 使用测试租户组件验证扩展机制，不把测试 DTO 放入生产包；
- 架构边界使用源码扫描测试验证（`TenantAdapterArchitectureTest`），未引入 ArchUnit；
- XML 测试只在引入真实 XML 租户后补充，不为假设需求增加依赖。

## 11. 兼容性与迁移

- 本功能只新增 Spec，不修改已有 API、数据库、领域事件或配置。
- 现有 `interfaces/web/request/CreateIntakeRequest` 和 `PersonRequestConverter` 继续服务 `/api/v1/claim-intakes`，不迁移到本转换层；本转换层只服务后续租户 Raw Payload 入口。
- 后续实现是增量代码，不改变现有 `/api/v1/claim-intakes` 的行为。
- 新增类统一使用 `Intake` 命名，不新增 `Sign*` 类；`signNo` 保留。
- 既有 `SIGN_RECEIVE` 交互类型不改名，避免改变已有审计数据语义。
- 新增租户和协议版本通过注册完成，不要求修改既有组件。
- `TENANT_PROTOCOL_NOT_SUPPORTED` 只在入口适配器接入后进入对外错误契约；届时需要同步更新入口 Spec。

### 命名收敛

| 原设计命名 | 本 Spec 命名 | 说明 |
| --- | --- | --- |
| `SignConvertPipeline` | `IntakeConvertPipeline` | 转换层主流程 |
| `SignApplicationCommand` | `SubmitIntakeCommand` | 复用现有 Application 命令，不新增平行命令 |
| `Sign*` 租户组件 | `TenantProtocolHandler` / `Request*` | 新组件统一落在 `interfaces/tenant` |
| `signNo` | `signNo` | 既有业务编号，保持兼容，不在本功能重命名 |

## 12. 已知缺口与待决事项

1. **材料归属**：现有 `SubmitIntakeCommand` 不包含材料。设计中的 `MaterialMapper` 需要先决定是扩展签发命令并同步扩展 Application 事务，还是把材料映射留在独立上传用例。真实租户报文确认前不实现。
2. **协议版本来源**：需要入口接入 Spec 明确可信头、缺省规则和 Gateway 契约。
3. **真实租户协议**：当前没有 Tenant DTO、编码表和样例报文，首批 Parser/Validator/Converter 无法实现。
4. **枚举强类型化**：已新增 `CertificateType`、`IntakeMaterialType` 标准枚举供转换层收敛；`Person.certificateType()` 与 `IntakeMaterial.type()` 仍为字符串，是否改为枚举字段另行评审。
5. **XML 依赖**：仅在真实租户要求时引入，并完成 XML 安全配置。
6. **实现时机**：通用框架、默认组件、注册表和 Pipeline 已实现；真实租户组件、材料命令归属和入口适配仍按 TK-TAI-002/005/006 门禁等待。

## 13. 实现状态（2026-10-23）

- 已实现：`interfaces/tenant/core` 下的 Pipeline、Parser/Validator/Converter 契约与默认实现、`CodeMapper`、组件注册表、异常层次、`TenantContext`/`TenantContextHolder`；`domain/model` 下新增 `CertificateType`、`IntakeMaterialType`。
- 测试租户：`src/test/java/.../interfaces/tenant/support` 下的 DTO、Parser、Validator、Converter，仅用于验证扩展机制，不进入生产包。
- 测试：6 个测试类、36 个用例覆盖 TEST-TAI-001 至 TEST-TAI-015；`mvn -pl backend/claim-intake-service -am verify` 通过（claim-intake-service 55 个测试，新增 36 个、既有 19 个）。
- 未实现：真实租户组件、材料进入签发命令、HTTP 入口与 `TENANT_PROTOCOL_NOT_SUPPORTED` 对外映射。
