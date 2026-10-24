# 代码修改规范

本文件负责落笔改代码时的规则：先查后写、复用优先、新类职责与位置、范围纪律、测试与自检。分层结构、模块归属、租户与敏感信息红线等业务和架构约束以 `docs/specs/baseline.md` 为准，验证命令与交付门禁见 `docs/engineering/workflow.md`。

## 1. 修改前：先查找已有实现

- 动手前先检索同类能力：工具类、异常体系、响应信封、转换器、缓存 / 存储 / 发号等端口实现，以及同模块内已有的 Service、Repository、Mapper。
- 优先复用已有能力和项目既有习惯，不重复实现同类功能。
- 只有确认没有等价实现，或复用会破坏职责边界时才新建，并在提交说明里写明为什么不能复用。

## 2. 新建类

- 职责必须能用一句话说清；说不清就并入已有类或继续拆需求，不建"什么都干"的类。
- 放入正确模块和分层，位置与依赖方向按 `docs/specs/baseline.md` 的架构约束；不创建无实际用途的空包。
- 使用统一命名后缀，同一项目不混用职责不明确的近似命名（例如同时存在 `SignMapper` 与 `SignPersistenceMapper`，一个表示 MyBatis Mapper、一个表示对象转换时应分别命名为 `SignMapper` 和 `SignPersistenceConverter`）。

| 类型 | 后缀 |
| --- | --- |
| Controller | `Controller` |
| Service | `Service` |
| Repository 接口 / 实现 | `Repository` / `RepositoryImpl` |
| MyBatis Mapper | `Mapper` |
| 数据库对象 | `DO` |
| 请求 / 响应对象 | `Request` / `Response` |
| 应用命令 / 结果 | `Command` / `Result` |
| 写入用例的 command handler | `CommandHandler`（如 `SignCommandHandler`、`MaterialCommandHandler`） |
| 读取用例的查询服务 | `QueryService`（如 `SignQueryService`、`PushTaskQueryService`） |
| 协议与外部系统适配器 | `Adapter`（如 `TenantProtocolAdapter`、`InsurerPushAdapter`） |
| 转换器 | `Converter` |
| 外部客户端 | `Client` |
| MQ 消费者 | `Consumer` |
| 定时任务 | `JobHandler` |
| 配置类 | `Configuration` / `Config` |

- 方法名用小驼峰直接表达业务行为（`findBySignNo()`、`markMaterialUploaded()`），避免 `handle()`、`process()`、`deal()` 这类含义模糊的命名。
- 查询方法语义统一：`get` 明确应该存在，`find` 允许不存在，`list` 返回集合，`count` 返回数量，`exists` 判断是否存在。
- 不保留无实际用途的类、方法、字段、注释代码、空实现和占位代码。
- 业务状态、类型、来源、固定编码用枚举或常量，不散落魔法值。

## 3. 范围纪律

- 只修改当前任务范围内的代码，不借机重构无关代码、不顺手调整格式之外的公共逻辑。
- 不做任务范围外的重构，包括"看到就不顺眼"的命名与结构调整。
- 不随意调整公共 API、共享契约、领域事件字段和响应信封语义；确实需要变更时按 `docs/engineering/specification.md` 更新 Spec，触及项目级边界时按 `docs/specs/baseline.md`「与功能 Spec 的关系」先更新基线。
- 任务范围外发现的问题记录后反馈给用户，不顺手改。

## 4. 测试

- 修改核心业务逻辑必须同步新增或更新测试。
- 测试至少覆盖：正常路径、关键边界、异常路径，以及需求明确涉及的幂等、重复请求或并发场景。
- 测试名称直接体现"业务场景 + 条件 + 预期结果"，例如 `shouldUploadMaterialsWhenStatusIsImageNotUpload`、`shouldThrowWhenSignNotFound`。
- 不得为了提高覆盖率编写没有有效断言的测试。

## 5. 改完后的代码自检

交付前确认（执行哪条验证命令由 `docs/engineering/workflow.md` 规定）：

- 项目能够正常编译；
- 不存在未使用的 import；
- 包名和类名调整后 `package`、`import` 已同步更新；
- 受影响测试正常通过；
- 不存在明显编译告警或 IDE 错误。

## 6. Java 细则

### 时间与类型

新代码优先使用 Java Time API（`LocalDate`、`LocalDateTime`、`Instant`、`Duration`），避免新增 `Date`、`Calendar`、`SimpleDateFormat`。

### 注释与署名

- 每个生产类、接口、枚举、Record 和 Spring 配置类都有类级 Javadoc，说明主要职责、必要业务含义和关键约束，并包含 `@author wangguangwu`；不写"XxxService 类"这类没有实际信息的注释。
- 有业务含义的 `public` / `protected` 方法提供 Javadoc，存在重要业务约束（状态前置条件、幂等处理）时写明。
- Getter、Setter、构造器、简单 DTO 转换和语义明确的简单私有方法不强制加重复注释。
- 行内注释只说明设计原因和特殊约束（例如"外部系统可能重复推送，数据库唯一键作为最终幂等兜底"），不复述代码在做什么。

### 异常

- 异常要具备明确语义，例如 `SignNotFoundException`、`InvalidSignStatusException`、`ConcurrentSignModificationException`；不直接 `throw new RuntimeException("error")`。
- 只在以下场景主动捕获：转换为当前业务能理解的异常、补充必要业务上下文、执行明确的降级或补偿、隔离第三方 SDK 或外部系统异常。
- 重新抛出必须保留原始异常；禁止 `catch (Exception e) { throw new RuntimeException(e.getMessage()); }` 这类丢失堆栈与上下文的写法。

### 日志

- 统一使用 SLF4J（`@Slf4j`）和占位符：`log.info("签收申请创建成功，signNo={}", signNo)`；禁止字符串拼接。
- 异常日志保留完整堆栈：`log.error("签收申请处理失败，signNo={}", signNo, e)`；不得只记 `e.getMessage()`。
- 日志优先携带能定位业务问题的标识：`tenantCode`、`signNo`、`externalApplyNo`、`caseNo`、`taskNo`、`traceId`。
- 日志内容不得触碰 `docs/specs/baseline.md` 的敏感信息红线；高频循环中避免打印大量 `info`。

### Spring 与 Lombok

- Spring Bean 统一构造器注入（`@RequiredArgsConstructor` + `final` 字段），禁止 `@Autowired` 字段注入。
- 可以按项目现有习惯使用 Lombok（`@Getter`、`@Setter`、`@Builder`、`@RequiredArgsConstructor` 等）；包含核心业务状态的对象谨慎使用 `@Data`，避免自动暴露 Setter 让外部任意修改状态，状态变更走领域行为方法（如 `markMaterialUploaded()`）。

### 集合与 Stream

- 返回集合时优先返回空集合，不返回 `null`（如 `Collections.emptyList()`）。
- Stream 只用于简单过滤、转换和聚合；包含复杂状态修改、异常处理或多步骤业务逻辑时用普通循环。
- 禁止用 `peek()` 承担业务修改。

## 7. 前端细则

页面与状态的组织、API 访问边界、敏感信息限制见 `docs/specs/baseline.md` 的架构约束，本节只规定代码写法：

- 每个 Vue 页面、复用组件、Composable、Store、API 模块和导出的业务函数使用 JSDoc 或文件头注释，说明职责、所属功能域和关键交互约束，并包含 `@author wangguangwu`。
- 状态管理、权限控制、租户切换、错误映射和敏感信息展示逻辑必须说明业务原因；不给显而易见的样式或简单赋值加噪声注释。
- 注释不得包含令牌、外部凭证、真实个人信息或内部地址；过期注释视为缺陷。

## 8. HTTP 响应与错误码

适用于 Gateway 暴露的 HTTP API、全局异常处理器和异常日志。稳定响应契约放在 `claim-common`，具体服务的异常映射器留在该服务自身，不由其他服务代处理。

成功与失败响应统一使用 `ApiResult<T>`：

```json
{
  "success": false,
  "code": "REQUEST_VALIDATION_FAILED",
  "message": "请求参数不合法",
  "traceId": "...",
  "data": null
}
```

- `success` 表示请求是否成功；成功码固定为 `OK`。
- `code` 是稳定、面向调用方的机器可读错误码；领域错误码由所属限界上下文定义，不复用技术异常类名。
- `message` 是可安全展示的说明，不含令牌、凭证、材料内容、跨租户数据、SQL 或堆栈信息。
- `traceId` 用于定位请求：网关或链路追踪已提供时必须透传，缺失时由服务生成并写入安全日志。
- `data` 仅在成功时返回业务数据，失败响应为 `null`。

| 场景 | HTTP 状态 | 错误码 | 日志要求 |
| --- | --- | --- | --- |
| 参数校验失败 | 400 | `REQUEST_VALIDATION_FAILED` | 记录字段名和 `traceId`，不记录原始敏感值 |
| JSON 或请求格式错误 | 400 | `REQUEST_VALIDATION_FAILED` | 记录异常类别和 `traceId`，不记录原始报文 |
| 调用方身份无效 | 401 | `UNAUTHORIZED` | 记录调用方类别和 `traceId`，不记录凭证 |
| 资源不存在 | 404 | 上下文定义的错误码 | 记录资源类型与 `traceId`，不记录跨租户标识 |
| 状态冲突或幂等冲突 | 409 | 上下文定义的错误码 | 记录聚合类型、已脱敏标识和 `traceId` |
| 服务暂不可用或资源耗尽 | 503 | 上下文定义的错误码 | 记录依赖类别、聚合类型和 `traceId`，不记录外部响应原文 |
| 未预期服务端错误 | 500 | `INTERNAL_ERROR` | 记录异常类别和 `traceId`，响应不暴露内部信息 |

- 不把异常信息、堆栈、SQL、外部系统响应原文或敏感请求体直接返回调用方。
- 异常处理器不得吞掉异常后返回 HTTP 200，状态码必须表达失败类别。
- 全局处理器只做边界异常映射，不承载领域决策、补偿或跨上下文编排。
- 新增领域错误码在 `design.md` 的异常处理部分说明触发条件、HTTP 映射、调用方行为与兼容性影响。
