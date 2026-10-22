# Person 标准模型与 DO 转换 Spec（design）

## 元信息

- 功能名称：`person-model`
- 文档类型：`design`
- 关联需求：`specs/person-model/requirements.md`
- 状态：Draft

## 1. 领域模型设计（DS-1）

### 1.1 标准 Person 模型

在 `domain/model/person` 包下建立以 `Person` 为根的封闭类型层级，表达理赔场景中四类核心角色：

```text
Person (sealed abstract)
 ├── Applicant       申请人
 ├── AccidentPerson  出险人
 ├── MainInsured     主被保险人
 └── Payee           领款人
```

- `Person` 为 `sealed abstract class`，通过 `permits` 限定四个子类型；四个子类型均为 `final record`（Java 17 下 record 隐式 final，满足 `permits` 约束）。
- `Person` 仅声明通用身份访问契约（`name`、`certificateType`、`certificateNo`、`mobile`、`gender`、`birthDate`），不持有字段；字段由 record 子类型持有，避免基类字段与 record 组件重复。
- record 子类型自动生成的访问器覆盖 `Person` 的抽象方法，使 `switch` 表达式可对 `Person` 做穷尽匹配，便于未来按角色分支处理。

### 1.2 字段定义

| 字段 | 类型 | 说明 | 归属 |
| --- | --- | --- | --- |
| `name` | String | 姓名 | 通用 |
| `certificateType` | String | 证件类型（如 ID_CARD） | 通用 |
| `certificateNo` | String | 证件号 | 通用（PII） |
| `mobile` | String | 手机号 | 通用（PII） |
| `gender` | String | 性别 | 通用 |
| `birthDate` | String | 出生日期（ISO-8601） | 通用 |
| `relationshipToInsured` | String | 与投保人/被保人关系 | Applicant |
| `accidentDate` | String | 出险日期 | AccidentPerson |
| `accidentType` | String | 出险类型 | AccidentPerson |
| `policyNo` | String | 保单号 | MainInsured |
| `beneficiaryRelation` | String | 与被保人关系 | MainInsured |
| `bankName` | String | 开户行 | Payee |
| `bankAccount` | String | 银行账号 | Payee（PII） |
| `payeeRelation` | String | 与出险人关系 | Payee |

## 2. 持久化模型与类型处理器（DS-2）

### 2.1 PersonInfoDO 层级

在 `infrastructure/persistence/person` 包下建立与人员 JSON 列一一对应的持久化对象层级，使用 Lombok `@Data` 削减样板：

- `PersonInfoDO`（abstract，Lombok `@Getter @Setter @NoArgsConstructor`）：通用字段。
- `ApplicantInfoDO` / `AccidentPersonInfoDO` / `MainInsuredInfoDO` / `PayeeInfoDO`：继承通用字段并补充角色字段（Lombok `@Data @NoArgsConstructor`）。

### 2.2 JSON 列映射

`ClaimIntakeDO` 的四个人员字段类型由 `String` 改为对应 `PersonInfoDO` 子类，并标注 MyBatis-Plus 类型处理器：

```java
@TableField(value = "applicant_info", typeHandler = JacksonTypeHandler.class)
private ApplicantInfoDO applicantInfo;
// accident_person_info / main_insured_info / payee_info 同理
```

- `@TableName(value = "t_claim_intake", autoResultMap = true)`：开启结果映射自动包含类型处理器，保证查询时 JSON 正确反序列化为具体 `PersonInfoDO`。
- 列名通过 `map-underscore-to-camel-case` 由字段名自动推导（`applicantInfo` → `applicant_info`），保持与现有建表脚本兼容。

## 3. 转换契约（DS-3）

### 3.1 PersonConverter（MapStruct）

`infrastructure/persistence/PersonConverter.java`：

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PersonConverter {
    Applicant toApplicant(ApplicantInfoDO source);
    AccidentPerson toAccidentPerson(AccidentPersonInfoDO source);
    MainInsured toMainInsured(MainInsuredInfoDO source);
    Payee toPayee(PayeeInfoDO source);
    ApplicantInfoDO toApplicantInfoDO(Applicant source);
    AccidentPersonInfoDO toAccidentPersonInfoDO(AccidentPerson source);
    MainInsuredInfoDO toMainInsuredInfoDO(MainInsured source);
    PayeeInfoDO toPayeeInfoDO(Payee source);
}
```

- 字段名在 `PersonInfoDO` 与 `Person` 子类型间一致，MapStruct 自动完成同名映射；`unmappedTargetPolicy = ERROR` 强制转换完整性，新增字段遗漏将编译失败。
- 空值（`null` 源）由 MapStruct 默认映射为 `null` 目标，满足 FR-4。

### 3.2 复用方式

`ClaimIntakePersistenceMapper` 通过 `@Mapper(uses = PersonConverter.class)` 在 `toState` / `toDO` 中自动嵌套调用 `PersonConverter`，完成 `ClaimIntakeDO` 中四个 `PersonInfoDO` 字段与 `ClaimIntakeState` 中四个 `Person` 子类型的双向转换，领域层不直接接触 MapStruct 或 JSON。

### 3.3 请求边界转换

`ClaimIntakeController` 注入 `PersonConverter` 与 `ObjectMapper`：将入站 `JsonNode` 先反序列化为对应 `PersonInfoDO`，再经 `PersonConverter` 转为领域 `Person` 子类型后传入命令。JSON 解析失败在边界内抛出 `MALFORMED_REQUEST`（FR-3）。

## 4. 分层与依赖方向（DS-4）

- `domain`：仅含 `Person` 层级，不依赖 MapStruct、MyBatis-Plus、Jackson、Lombok 运行行为（Lombok 仅编译期生成代码）。
- `infrastructure`：容纳 `PersonInfoDO`、`PersonConverter`、类型处理器与 `ClaimIntakeDO`。
- `adapter`：负责 `JsonNode` → `PersonInfoDO` → 领域 `Person` 的边界转换。
- 外部 DTO / JSON 协议细节止于基础设施与适配层，不穿透领域层。

## 5. 日志与可观测（DS-5）

- `resources/logback-spring.xml`：控制台 + 滚动文件 Appender，日志模式包含 MDC 占位符 `%X{traceId}`、`%X{tenantCode}`，统一项目日志格式。
- `TraceLoggingInterceptor`（Web `HandlerInterceptor`）：在请求入口从可信头 `X-Trace-Id` / `X-Claim-Tenant` 注入 MDC，`afterCompletion` 清理，确保整条链路日志可关联。
- SLF4J `Logger` 应用于 `PersonConverter`（DEBUG 记录转换方向，仅记录角色类型不记录 PII）与 `IntakePersistenceAdapter`（DEBUG 记录保存操作）。
- PII 约束：日志不得输出 `certificateNo`、`mobile`、`bankAccount` 原文；如需记录仅记录是否存在或类型。

## 6. 工程依赖（DS-6）

- 根 `pom.xml` 增加 `lombok.version` 并在 `dependencyManagement` 托管 `org.projectlombok:lombok`。
- `claim-common` / `claim-gateway` / `claim-intake-service` / `claim-case-delivery` 均引入 `lombok`（scope `provided`）。
- `claim-intake-service` 的 `maven-compiler-plugin` 的 `annotationProcessorPaths` 中，将 `lombok` 置于 `mapstruct-processor` 之前，保证 MapStruct 可见 Lombok 生成的访问器。
- `MyBatis-Plus` 与 `SLF4J`/`Logback`：前者已在 `claim-intake-service` 使用（Spring Boot 默认携带 Logback + SLF4J），本 Spec 仅显式补充 `logback-spring.xml` 与 MDC 集成。

## 7. 并发、兼容与测试策略（DS-7）

- 并发：人员字段为值对象，无并发状态；乐观锁仍由 `version` 字段与 `OptimisticLockerInnerInterceptor` 控制。
- 兼容：表结构、HTTP 契约不变；旧数据中缺失的角色字段映射为 `null`，不破坏既有记录。
- 测试：
  - `PersonConverterTest`：覆盖四个子类型双向转换与空值映射（FR-1/FR-2/FR-4）。
  - `ClaimIntakePersistenceMapperTest`：覆盖整条 DO ↔ 聚合链路含 Person 子类型。
  - 既有 `ClaimIntakeTest` / `ClaimIntakeCommandServiceTest` / `ClaimIntakeControllerTest`：适配强类型 Person 参数。
