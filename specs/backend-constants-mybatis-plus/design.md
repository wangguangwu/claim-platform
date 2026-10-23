# 后端常量治理与 MyBatis-Plus 查询重构设计

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | In Progress |
| 关联需求 | `specs/backend-constants-mybatis-plus/requirements.md` |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service`、`backend/claim-common` |

## DS-CMP-001 重构原则

本次重构只替换实现方式和固定值承载方式，不改变业务行为。判断一个值是否提取的标准：

| 值类型 | 处理 |
| --- | --- |
| 数据库表、列、索引名 | 提取为持久化常量，注解和查询共用 |
| HTTP 头、API 路径、校验长度、格式正则 | 提取为 Web 契约常量 |
| 错误码、HTTP 状态、固定响应文案 | 提取为错误码枚举 |
| 指标名称、标签键、标签值 | 提取为指标常量 |
| Redis 脚本名、结果标记、列表下标、键片段 | 提取为 Redis/缓存常量 |
| 业务状态、交互类型、交互方向、处理结果 | 提取为枚举 |
| 协议字段路径、协议编码、扩展字段名 | 提取为协议常量 |
| 格式下标、容量、哨兵、退避边界、调度作用域 | 提取为领域/应用常量 |
| 配置默认值 | 提取为属性类内命名常量 |
| 日志文案、异常提示、一次性技术字符串 | 保持原样 |
| 测试夹具 SQL | 保持原样 |

约束：

- 注解属性只能引用编译期常量；
- 常量类使用 `final` 类和私有构造器，不持有可变状态；
- 常量值必须与重构前逐字一致；
- 不新增配置项，不改变配置键。

## DS-CMP-002 持久化契约常量

新增三个表契约类，位于 `infrastructure/persistence`：

```java
public final class ClaimIntakeTable {
    public static final String NAME = "t_claim_intake";
    public static final String ID = "id";
    public static final String TENANT_CODE = "tenant_code";
    public static final String SIGN_NO = "sign_no";
    public static final String EXTERNAL_APPLY_NO = "external_apply_no";
    public static final String APPLICANT_NAME = "applicant_name";
    public static final String APPLICANT_INFO = "applicant_info";
    public static final String ACCIDENT_PERSON_INFO = "accident_person_info";
    public static final String MAIN_INSURED_INFO = "main_insured_info";
    public static final String PAYEE_INFO = "payee_info";
    public static final String EXT_JSON = "ext_json";
    public static final String INTAKE_STATUS = "intake_status";
    public static final String VERSION = "version";
    public static final String IS_DELETED = "is_deleted";
    public static final String INDEX_UK_SIGN_NO = "uk_sign_no";
}
```

`IntakeMaterialTable` 与 `InteractionLogTable` 采用同样结构，覆盖各自全部列名。应用范围：

- `ClaimIntakeDO`、`IntakeMaterialDO`、`InteractionLogDO` 的 `@TableName` 与 `@TableField`；
- `SignNoSequenceMapper` 的高水位列与表；
- `SignNoSchemaValidator` 的表名与索引名。

MyBatis-Plus 的 `LambdaQueryWrapper` 继续使用属性方法引用，不引入列名字符串；只有聚合表达式、`@TableField` 和元数据校验使用列常量。

## DS-CMP-003 MyBatis-Plus 高水位查询

`SignNoSequenceMapper` 改为：

```java
@Mapper
public interface SignNoSequenceMapper extends BaseMapper<ClaimIntakeDO> {

    default String selectMaxSignNo(String start, String end) {
        QueryWrapper<ClaimIntakeDO> wrapper = new QueryWrapper<>();
        wrapper.select("MAX(" + ClaimIntakeTable.SIGN_NO + ") AS " + ClaimIntakeTable.SIGN_NO)
                .ge(ClaimIntakeTable.SIGN_NO, start)
                .lt(ClaimIntakeTable.SIGN_NO, end)
                .apply("CHAR_LENGTH(" + ClaimIntakeTable.SIGN_NO + ") = {0}",
                        SignNoFormat.SIGN_NO_LENGTH);
        ClaimIntakeDO result = selectOne(wrapper);
        return result == null ? null : result.getSignNo();
    }
}
```

等价 SQL：

```sql
SELECT MAX(sign_no) AS sign_no
FROM t_claim_intake
WHERE sign_no >= ?
  AND sign_no < ?
  AND CHAR_LENGTH(sign_no) = 17
```

语义保持：

- 不添加 `is_deleted = 0`，软删除行仍计入高水位；
- 日期区间由 `SignNoFormat.dateRangeStart/dateRangeEnd` 提供；
- 长度 17 排除历史 `SIGN` 编号及未来非 17 位编号；
- `selectOne` 只返回聚合单行，通过 `sign_no` 别名映射回 `ClaimIntakeDO.signNo`；
- `DataAccessException` 仍由 `SignNoHighWaterMarkImpl` 转换为 `HIGH_WATER_MARK_UNAVAILABLE`；
- 结果解析与越界判断保持原实现。

选择 `QueryWrapper` 而非 `LambdaQueryWrapper` 的原因：`MAX(...)` 与 `CHAR_LENGTH(...)` 是列级表达式，Lambda 方式无法在保留可读性的同时表达；条件列仍全部来自持久化常量。

## DS-CMP-004 启动索引校验

`SignNoSchemaValidator` 改为读取 JDBC 元数据，不再包含 SQL 文本：

```java
try (Connection connection = dataSource.getConnection()) {
    DatabaseMetaData metadata = connection.getMetaData();
    try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null,
            ClaimIntakeTable.NAME, true, false)) {
        while (indexes.next()) {
            boolean firstColumn = indexes.getInt(ORDINAL_POSITION) == 1;
            String indexName = indexes.getString(INDEX_NAME);
            String columnName = indexes.getString(COLUMN_NAME);
            if (firstColumn
                    && ClaimIntakeTable.INDEX_UK_SIGN_NO.equalsIgnoreCase(indexName)
                    && ClaimIntakeTable.SIGN_NO.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
    }
}
```

- `unique=true` 只返回唯一索引；
- `ORDINAL_POSITION = 1` 对应原 SQL 的 `seq_in_index = 1`；
- 索引名与列名忽略大小写，兼容 MySQL 元数据返回形式；
- `catalog` 使用当前连接数据库；
- `SQLException` 转换为 `IllegalStateException`，启动失败；
- 校验通过和失败日志保留表名与索引名，值来自常量。

选择元数据 API 而非 `information_schema` 查询的原因：

- 启动校验不是业务查询，不需要 Mapper；
- 元数据 API 避免内嵌 SQL，同时不引入 MyBatis-Plus 对系统表的非常规映射；
- 仅依赖 JDBC 标准接口和 MySQL 驱动实现，行为可在真实 MySQL 集成测试中验证。

## DS-CMP-005 Web 契约与错误映射

新增 `ClaimIntakeWebContract`：

```java
public final class ClaimIntakeWebContract {
    public static final class Headers {
        public static final String SERVICE_TOKEN = "X-Claim-Service-Token";
        public static final String TENANT = "X-Claim-Tenant";
        public static final String TRACE_ID = "X-Trace-Id";
        public static final String PROTOCOL_VERSION = "X-Claim-Protocol-Version";
    }

    public static final class Paths {
        public static final String CLAIM_INTAKES = "/api/v1/claim-intakes";
        public static final String TENANT_INTAKES = "/api/v1/tenant-intakes";
    }

    public static final class Limits {
        public static final int IDEMPOTENCY_KEY_LENGTH = 64;
        public static final int SIGN_NO_LENGTH = 64;
        public static final int MATERIAL_TYPE_LENGTH = 32;
        public static final int MATERIAL_NAME_LENGTH = 255;
        public static final int MATERIAL_PATH_LENGTH = 1000;
    }

    public static final class Patterns {
        public static final String TENANT_CODE = "[A-Za-z0-9_-]{1,64}";
        public static final String PROTOCOL_VERSION = "[A-Za-z0-9._-]{1,32}";
    }
}
```

应用：

- `GatewayRequestContextResolver`、`TenantIntakeContextResolver`、`TraceLoggingInterceptor` 使用头常量；
- 控制器使用路径常量；
- `CreateIntakeRequest`、`UploadMaterialsRequest` 使用长度常量；
- 租户与协议版本正则使用模式常量；协议版本预编译 `Pattern` 保留在解析器中。

错误映射新增枚举：

```java
public enum IntakeErrorCode {
    REQUEST_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED", "请求参数不合法"),
    UNTRUSTED_CALLER(HttpStatus.UNAUTHORIZED, "UNTRUSTED_CALLER", "调用方未通过内部鉴别"),
    TENANT_CONTEXT_MISSING(HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_MISSING", "缺少租户上下文"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "请求报文格式不合法"),
    TENANT_PROTOCOL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "TENANT_PROTOCOL_NOT_SUPPORTED", "租户协议不支持"),
    CLAIM_INTAKE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLAIM_INTAKE_NOT_FOUND", "签收申请不存在"),
    INVALID_INTAKE_STATUS(HttpStatus.CONFLICT, "INVALID_INTAKE_STATUS", "当前申请状态不允许该操作"),
    INTAKE_SUBMISSION_IN_PROGRESS(HttpStatus.CONFLICT, "INTAKE_SUBMISSION_IN_PROGRESS", "申请正在处理中，请稍后重试"),
    CLAIM_INTAKE_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "CLAIM_INTAKE_CONCURRENT_MODIFICATION", "签收申请已被并发修改"),
    INTAKE_NUMBER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "INTAKE_NUMBER_UNAVAILABLE", "签收号服务暂不可用，请稍后重试", 5),
    INTAKE_NUMBER_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "INTAKE_NUMBER_EXHAUSTED", "今日签收号已用尽，请联系平台"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统繁忙，请稍后重试");
}
```

- 只有 `INTAKE_NUMBER_UNAVAILABLE` 配置 `retryAfterSeconds = 5`，处理器据此写 `Retry-After`；
- 其余枚举该字段为 `null`，不写响应头；
- 枚举值、状态和文案与重构前逐字一致；
- 处理器保留异常分类、日志和 traceId 兜底逻辑。

## DS-CMP-006 指标与缓存常量

新增 `application/service/IntakeMetricConstants`，集中：

- 提交指标：`claim.intake.submission`；
- 发号指标：`claim.intake.number.allocation`、`fallback`、`recovery`、`conflict`、`sequence_gap`；
- 标签键：`source`、`result`、`trigger`、`type`、`scope`；
- 结果值：`success`、`skipped`、`failure`、`allocated`、`recovered`、`exhausted`、`unavailable`、`conflict`；
- 冲突类型：`sign_no`、`external_apply_no`；
- 校准作用域：`current`、`previous`。

`SignNoMetrics`、`SignNoAllocationService`、`SignNoReconciliationScheduler`、`ClaimIntakeCommandService` 统一引用上述常量。指标名称、标签键、标签值不得变化。

Redis/缓存常量：

| 常量 | 值 | 使用方 |
| --- | --- | --- |
| `SIGN_NO_LUA_PATH` | `lua/sign-no/` | `RedisSignNoSequenceCounter` |
| `ALLOCATE_SCRIPT` | `allocate.lua` | 同上 |
| `RECONCILE_AND_ALLOCATE_SCRIPT` | `reconcile-and-allocate.lua` | 同上 |
| `RECONCILE_ONLY_SCRIPT` | `reconcile-only.lua` | 同上 |
| `MISSING_MARKER` | `-1` | 同上 |
| `INVALID_MARKER` | `-2` | 同上 |
| `RECONCILE_ALLOCATE_RESULT_SIZE` | `3` | 同上 |
| `RECONCILE_RESULT_SIZE` | `2` | 同上 |
| `RECONCILE_LOCK_SEGMENT` | `reconcile-lock:` | `RedissonSignNoRecoveryLock` |
| `DATE_KEY_FORMATTER` | `BASIC_ISO_DATE` | 计数器与恢复锁 |
| `SHA_256` | `SHA-256` | 幂等缓存与业务锁 |
| `KEY_SEPARATOR` | `\n` | 幂等缓存与业务锁 |

## DS-CMP-007 领域与重试常量

`SignNoFormat`：

- 增加 `DATE_DIGITS = 8`；
- 增加 `DATE_START_INDEX = PREFIX.length()`；
- 增加 `SEQUENCE_START_INDEX = DATE_START_INDEX + DATE_DIGITS`；
- `SIGN_NO_LENGTH = PREFIX.length() + DATE_DIGITS + SEQUENCE_DIGITS`；
- 正则长度使用 `SIGN_NO_LENGTH`；
- `parseDate`、`parseSequence` 使用命名下标；
- 公开常量的值和现有格式完全一致。

`SignNoReconciliationResult`：

- 增加 `UNKNOWN_VALUE = -1L`；
- `skipped`、`failed`、`sequenceGap` 使用该常量；
- `SignNoReconciliationService` 使用 `failed(...)` 工厂方法，不再直接构造带 `-1` 的结果。

`ClaimIntakeCommandService`：

- `BACKOFF_MIN_MILLIS = 10`；
- `BACKOFF_MAX_MILLIS_EXCLUSIVE = 31`；
- 退避逻辑、区间和随机分布保持不变。

`SignNoReconciliationScheduler`：

- 线程名、关闭等待秒数、`current`/`previous` 作用域使用命名常量；
- 指标结果继续使用 `IntakeMetricConstants`。

## DS-CMP-008 交互审计与参考协议

新增 `domain/model/InteractionType`：

```java
public enum InteractionType {
    SIGN_RECEIVE,
    IMAGE_UPLOAD;

    public String code() {
        return name();
    }
}
```

新增持久化枚举 `InteractionDirection`（`IN`、`OUT`）与 `InteractionResult`（`SUCCESS`、`FAILED`），`InteractionLogDO` 构造器使用 `IN`、`SUCCESS` 的 `code()`。

新增 `ReferenceProtocolConstants`：

- `CERTIFICATE_TYPE_FIELD = "applicant.certificateType"`；
- `CERTIFICATE_CODE_ID_CARD = "IDCARD"`；
- `CERTIFICATE_CODE_PASSPORT = "PASSPORT"`；
- `RELATIONSHIP_TO_INSURED_FIELD = "applicant.relationshipToInsured"`；
- `EXTENSION_ACCIDENT_DATE = "accidentDate"`。

`ReferenceRequestConverter` 与 `ReferenceRequestValidator` 引用上述常量。参考协议的 `reference`/`v1` 已由 `ReferenceProtocolHandler` 常量承载，不重复定义。

## DS-CMP-009 配置默认值

`SignNoProperties`、`IntakeSubmissionLockProperties`、`IntakeSubmissionReplayCacheProperties` 增加 `DEFAULT_*` 常量，字段初始化引用常量。

- 配置键名、默认值和校验注解不变；
- `application.yml` 的环境变量默认值不变；
- 常量只服务于 Java 侧默认值，不替代配置文件。

## DS-CMP-010 兼容性与验证策略

兼容性检查项：

| 契约 | 验证 |
| --- | --- |
| HTTP 路径与方法 | 控制器测试、集成测试 |
| 请求头 | 上下文解析测试、集成测试 |
| 错误码/状态/文案/`Retry-After` | `SignNoExceptionHandlerTest` 与控制器测试 |
| 指标名称与标签 | `SignNoAllocationServiceTest`、调度器测试、集成测试 |
| 高水位语义 | `SignNoHighWaterMarkImplTest`、`SignNoGenerationIntegrationTest` |
| 启动索引校验 | `SignNoSchemaValidatorTest`、Spring 上下文启动 |
| 交互审计字符串 | `ClaimIntakeCreationServiceTest`、`ClaimIntakeSubmissionIntegrationTest` |
| 签收号格式与区间 | `SignNoFormatTest` |

## DS-CMP-011 测试策略

- 单元测试：错误码枚举、Web 契约常量、格式化下标、未知值、退避边界、Schema 校验元数据分支。
- 集成测试：真实 MySQL/Redis 上执行原有发号、降级、恢复、冲突、软删除、历史 `SIGN` 和跨租户场景。
- 架构测试：扫描主代码，禁止 Mapper SQL 注解、内嵌 SQL 语句和非 `BaseMapper` Mapper。
- 契约测试：处理器返回的状态码、错误码、文案与 `Retry-After` 不变。

## DS-CMP-012 风险与边界

| 风险 | 处理 |
| --- | --- |
| MyBatis-Plus 聚合结果无法映射 | 使用 `AS sign_no` 别名，真实 MySQL 集成测试验证 |
| `DatabaseMetaData` 在 MySQL 驱动返回大小写差异 | 索引名与列名忽略大小写，并要求首列 |
| 元数据 API 与 `information_schema` 行为差异 | 集成测试启动 Spring 上下文即执行校验；补充单元测试覆盖缺失、非首列和异常 |
| 常量提取误改外部契约 | 保留原测试断言字面量，逐项比对 |
| 多 Mapper 映射同一数据对象 | `SignNoSequenceMapper` 专用于高水位查询，`ClaimIntakeMapper` 继续承载常规 CRUD |
| 领域层被基础设施常量污染 | 持久化常量放在基础设施包，领域层只使用 `SignNoFormat` 和业务枚举 |
