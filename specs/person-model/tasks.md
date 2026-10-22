# Person 标准模型与 DO 转换 Spec（tasks）

## 元信息

- 功能名称：`person-model`
- 文档类型：`tasks`
- 关联需求：`specs/person-model/requirements.md`
- 关联设计：`specs/person-model/design.md`
- 状态：In Progress

## 任务追踪

| 任务 | 说明 | 回链 | 状态 | 证据 |
| --- | --- | --- | --- | --- |
| TK-1 | 建立领域 `Person` 封闭层级（abstract + 4 个 record 子类型） | FR-1/FR-2 → DS-1 | Done | `domain/model/person/*` |
| TK-2 | 建立 `PersonInfoDO` 层级与 JacksonTypeHandler 映射 | FR-1/FR-2 → DS-2 | Done | `infrastructure/persistence/person/*`、`ClaimIntakeDO` |
| TK-3 | 实现 MapStruct `PersonConverter` 双向转换 | FR-1/FR-2/FR-4 → DS-3 | Done | `PersonConverter.java` |
| TK-4 | `ClaimIntakePersistenceMapper` 通过 `uses=PersonConverter` 嵌套转换 | FR-1/FR-2 → DS-3.2 | Done | `ClaimIntakePersistenceMapper.java` |
| TK-5 | `ClaimIntake` 聚合 / `ClaimIntakeState` / `SubmitIntakeCommand` 改用 `Person` 子类型 | FR-1/FR-2 → DS-4 | Done | 对应领域/应用层文件 |
| TK-6 | `ClaimIntakeController` 在边界用 Jackson+Converter 转领域 `Person` | FR-3 → DS-3.3 | Done | `ClaimIntakeController.java` |
| TK-7 | 接入 Lombok（根托管 + 四模块依赖 + 注解处理器顺序） | FR-5 → DS-6 | Done | 各 `pom.xml` |
| TK-8 | 接入 Logback/SLF4J（`logback-spring.xml` + `TraceLoggingInterceptor` + SLF4J 日志） | FR-5 → DS-5 | Done | `resources/logback-spring.xml`、拦截器、`PersonConverter`、`IntakePersistenceAdapter` |
| TK-9 | 新增并改造测试：PersonConverterTest + 适配既有测试 | FR-1/2/3/4 → DS-7 | In Progress | `PersonConverterTest.java` 等 |
| TK-10 | 编译与测试验证（`mvn verify`） | 全部 | Pending | 见下方测试证据 |

## 测试证据

> 待 TK-9/TK-10 执行后回填。

## 迁移说明

- 无数据库迁移：表结构不变，四个人员信息仍为 JSON 列。
- 无对外接口迁移：HTTP 请求契约保持 `JsonNode` 人员字段。
- 内部表示由 `String` 提升为强类型 `Person` 子类型，属于同上下文内重构。

## 已知限制

- `claim-gateway` / `claim-case-delivery` 仅引入 Lombok 工程依赖，未引入各自领域 `Person`（按非范围约定）。
- 角色专属字段为可选，旧数据缺字段映射为 `null`，不做回填。
- 日志 MDC 仅在 `claim-intake-service` Web 入口注入；非 Web 入口（如定时任务）暂不注入 MDC。
