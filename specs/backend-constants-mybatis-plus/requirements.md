# 后端常量治理与 MyBatis-Plus 查询重构需求

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | In Progress |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service`、`backend/claim-common` |
| 功能分支 | Spec 分支 `docs/backend-constants-mybatis-plus`；实现子分支 `refactor/backend-constants-mybatis-plus`（基于 Spec 分支） |
| 关联根需求 | `specs/requirements.md` |
| 关联 Spec | `specs/sign-no-generation/`、`specs/claim-intake-standard-flow/` |
| 本次交付 | 移除 Mapper 与启动校验中的手写 SQL、统一使用 MyBatis-Plus、集中硬编码值，外部行为保持不变 |

## 背景与目标

当前后端存在两类实现问题：

1. `SignNoSequenceMapper` 通过 `@Select` 手写签收号高水位 SQL；`SignNoSchemaValidator` 通过 `JdbcTemplate` 手写 `information_schema` 查询。后者不属于 Mapper，但同样把 SQL 内嵌在 Java 代码中。
2. 数据库表列名、HTTP 头、API 路径、错误码与文案、指标标签、Redis 键片段、Lua 脚本体量、交互类型、参考协议字段和校验长度等固定值散落在多个类中，部分以字符串字面量直接出现在方法体。

目标：

- 所有持久化查询通过 MyBatis-Plus 的 `BaseMapper` 与条件构造器完成，不在 Java 中拼接或内嵌 SQL 语句；
- 启动索引校验使用 JDBC 元数据 API，不包含 SQL 文本；
- 固定业务编码、外部契约、持久化列名、指标标签和边界值集中为枚举或常量；
- 不改变 HTTP 路径、请求头、错误响应、指标名称与标签、数据库结构和签收号语义；
- 增加架构测试，防止手写 SQL 和散落字面量重新进入生产代码。

## 范围

- `SignNoSequenceMapper` 改为 MyBatis-Plus Mapper，高水位查询使用 `QueryWrapper`。
- `SignNoSchemaValidator` 改为基于 `DataSource` 与 `DatabaseMetaData` 的索引校验。
- 新增持久化表、列和索引常量，并应用到 `@TableName`、`@TableField` 与查询构造。
- 新增 HTTP 头、API 路径、校验长度、租户编码与协议版本正则常量。
- 新增错误码枚举，集中错误码、HTTP 状态、响应文案和 `Retry-After`。
- 新增指标名称、标签键、标签值、Redis/Lua、缓存键摘要和交互审计常量。
- 新增参考协议字段路径、证件编码和扩展字段常量。
- 新增签收号格式索引、未知值哨兵、重试退避、调度作用域等常量。
- 新增配置属性默认值常量。
- 更新受影响的单元测试，新增“禁止手写 SQL”的架构测试。
- 回写 Spec 任务、测试证据与验证记录。

## 非范围

- 不修改签收号格式、Redis 发号、MySQL 降级、恢复校准、冲突重试和业务锁逻辑。
- 不修改数据库表结构、列名、索引、初始化和迁移脚本。
- 不新增、删除或修改 HTTP 接口、请求字段、响应字段和状态码。
- 不修改配置键名、配置默认值和环境变量名。
- 不修改错误码、响应文案、指标名称、指标标签和日志可观测维度。
- 不重构领域聚合、状态机、幂等键、事务边界和限界上下文协作。
- 不把日志文案、异常提示和 SQL 函数名强行抽成常量；只治理会形成契约或容易漂移的固定值。
- 不为测试代码中的 SQL 做常量化；测试夹具可使用显式 SQL 校验结果。
- 不引入 MyBatis XML、JPA、JOOQ 或新的持久化框架。

## 所属限界上下文与参与方

| 参与方 | 责任 | 本功能是否实现 |
| --- | --- | --- |
| Claim Intake 接口层 | 使用统一 HTTP 头、路径、校验长度和错误码枚举 | 是 |
| Claim Intake 应用层 | 使用统一指标标签、结果码和退避边界 | 是 |
| Claim Intake 领域层 | 使用格式索引、容量和哨兵常量 | 是 |
| Claim Intake 基础设施层 | 使用持久化常量、MyBatis-Plus 查询、JDBC 元数据和 Redis 常量 | 是 |
| MySQL | 表结构与数据保持不变，提供高水位查询和索引元数据 | 否，仅读取 |
| Redis | Key 结构与 Lua 脚本行为保持不变 | 否，仅常量引用 |

## 核心约束

1. Mapper 不得包含 `@Select`、`@Insert`、`@Update`、`@Delete` 或 XML SQL；只允许继承 `BaseMapper` 并使用 MyBatis-Plus 条件构造器。
2. 高水位查询必须保持原语义：主库读取、闭开日期区间、固定 17 位、包含软删除行、排除历史 `SIGN` 编号。
3. 启动索引校验失败时必须阻止启动；数据库元数据读取异常不得被静默忽略。
4. 注解属性必须使用编译期常量；运行期对象（如 `Pattern`、`DateTimeFormatter`）允许使用 `static final`。
5. 常量类不得持有可变状态，不得通过反射修改；枚举用于固定业务编码和错误映射。
6. 外部契约必须逐项保持：HTTP 方法、路径、头名、状态码、错误码、文案、`Retry-After`、响应信封、指标名称与标签。
7. 签收号格式、日期区间、容量、降级和恢复行为不得因重构改变。
8. 分层依赖不得逆转：应用层和领域层不依赖 MyBatis、JDBC、Redis 或基础设施常量。
9. 常量命名必须表达语义，不使用 `VALUE1`、`STR1` 等无意义名称。
10. 本重构不得借机修改业务规则或增加新配置项。

## 验收需求

### FR-CMP-001 Mapper 全面使用 MyBatis-Plus

WHEN 审查生产代码中的 Mapper THEN THE SYSTEM SHALL 只包含继承 `BaseMapper` 的接口和 MyBatis-Plus 条件构造逻辑；SHALL NOT 包含 `@Select`、`@Insert`、`@Update`、`@Delete` 注解或内嵌 SQL 文本。

### FR-CMP-002 高水位查询等价

WHEN 查询指定业务日期的签收号高水位 THEN THE SYSTEM SHALL 通过 MyBatis-Plus 生成 `MAX(sign_no)` 聚合查询，使用日期闭开区间、固定长度 17 和主库读取；SHALL 继续包含软删除行并排除历史 `SIGN` 编号；无记录时 SHALL 返回 0。

### FR-CMP-003 启动索引校验无手写 SQL

WHEN 应用启动 THEN THE SYSTEM SHALL 通过 JDBC `DatabaseMetaData` 校验 `t_claim_intake` 上存在以 `sign_no` 为首列的全局唯一索引 `uk_sign_no`；WHEN 索引缺失或元数据不可读 THEN SHALL 抛出异常阻止启动；SHALL NOT 在 Java 中内嵌 `SELECT` 语句。

### FR-CMP-004 持久化常量单一来源

WHEN 定义 MyBatis-Plus 数据对象或引用表列名 THEN THE SYSTEM SHALL 使用持久化常量类；表名、列名和唯一索引名 SHALL 各只有一处定义。

### FR-CMP-005 HTTP 契约常量集中

WHEN 解析请求头、声明控制器路径或校验请求字段 THEN THE SYSTEM SHALL 使用统一的头名、路径和长度常量；SHALL NOT 在多个类重复出现相同头名或路径字面量。

### FR-CMP-006 错误映射集中

WHEN 处理边界异常 THEN THE SYSTEM SHALL 从错误码枚举取得错误码、HTTP 状态、响应文案和可选 `Retry-After`；枚举值 SHALL 与重构前完全一致。

### FR-CMP-007 指标契约不变

WHEN 记录提交、发号、降级、恢复、冲突和序列差值指标 THEN THE SYSTEM SHALL 使用集中定义的指标名称、标签键和标签值；指标名称、标签键、标签值和指标语义 SHALL 与重构前完全一致。

### FR-CMP-008 Redis 与缓存常量集中

WHEN 访问 Redis 计数器、恢复锁或幂等定位缓存 THEN THE SYSTEM SHALL 使用统一的脚本名、结果标记、列表下标、键分隔符和摘要算法常量；Redis Key 结构和 Lua 行为 SHALL 不变。

### FR-CMP-009 领域边界与重试常量

WHEN 格式化/解析签收号、表示未知校准值或计算冲突重试退避 THEN THE SYSTEM SHALL 使用命名常量；SHALL NOT 在方法体出现格式下标、`-1` 哨兵或退避上下界字面量。

### FR-CMP-010 固定业务编码枚举化

WHEN 记录交互审计或转换参考协议 THEN THE SYSTEM SHALL 使用交互类型、交互方向、处理结果枚举和参考协议字段常量；数据库存储值与外部协议编码 SHALL 不变。

### FR-CMP-011 配置默认值命名

WHEN 声明配置属性默认值 THEN THE SYSTEM SHALL 使用命名常量；配置键名、默认值和校验规则 SHALL 不变。

### FR-CMP-012 外部行为零变化

WHEN 执行任一既有 HTTP 场景 THEN THE SYSTEM SHALL 返回与重构前相同的状态码、错误码、文案、响应头和响应结构；SHALL NOT 因常量提取改变 JSON 字段、指标标签或数据库写入值。

### FR-CMP-013 回归与架构门禁

WHEN 执行模块验证 THEN THE SYSTEM SHALL 通过原有单元测试、真实 MySQL/Redis 集成测试和新增架构测试；架构测试 SHALL 阻止 Mapper SQL 注解、内嵌 SQL 语句和非 `BaseMapper` Mapper 进入主代码。

### FR-CMP-014 分层依赖不变

WHEN 引入常量类 THEN THE SYSTEM SHALL 保持应用层与领域层不依赖 Spring、MyBatis、JDBC、Redis 或基础设施包；领域层常量 SHALL 为纯 Java 类型。

## 失败行为

| 场景 | 期望 |
| --- | --- |
| 高水位查询数据库异常 | 保持 `SignNoUnavailableException(HIGH_WATER_MARK_UNAVAILABLE)` |
| 高水位结果格式非法或越界 | 保持 `SignNoUnavailableException(INVALID_HIGH_WATER_MARK)` |
| 启动索引查询异常 | 抛出 `IllegalStateException`，应用启动失败 |
| 索引缺失或首列不是 `sign_no` | 抛出 `IllegalStateException`，应用启动失败 |
| 常量值与重构前不一致 | 相关契约测试失败，禁止合并 |

## 幂等与兼容性

- 本次重构不改变幂等键、幂等层级、回放规则和锁行为。
- 已存在的 `SIGN` 编号和新格式 `S` 编号继续共存。
- 数据库写入的交互类型、方向、结果字符串保持不变。
- 指标名称和标签保持不变，既有监控与告警无需调整。
- 配置键、默认值和环境变量保持不变，部署清单无需修改。

## 完成门禁

- 生产代码中不存在 Mapper SQL 注解和内嵌 SQL 语句；
- 高水位查询、启动校验、错误映射、指标和交互审计行为均由测试覆盖；
- `mvn -pl backend/claim-intake-service -am clean verify` 通过，集成测试在真实 MySQL/Redis 上执行且无跳过；
- 新增架构测试通过；
- 外部契约逐项比对无差异；
- Spec 任务、证据和验证记录已回写。
