# 平台基础运行能力设计

## DS-PLT-001 本地数据库拓扑

Docker Compose 仅运行一个 MySQL 8 容器；通过初始化脚本创建 `sign_db` 和 `push_db`。一个容器仅是本地开发便利，两个 schema 对应当前两个领域限界上下文的独立数据所有权，后续生产环境仍应独立部署和迁移。

## DS-PLT-002 启动配置

每个可部署模块提供自己的 Spring Boot 启动类和 `application.yml`。本地默认连接对应 MySQL schema，并显式关闭未在本地 Compose 中提供的 Nacos 发现与配置功能；Gateway 不依赖数据库。Redis 依赖的 health contributor 在本地默认关闭，避免未进入本地闭环的可选依赖使基础服务被误判为不可用。OpenTelemetry instrumentation 与 SDK 版本在顶层 POM 统一对齐，防止 Spring Boot 的默认依赖管理造成运行时类缺失；本地默认禁用 SDK，生产环境再由受控配置接入 Collector。

## DS-PLT-003 响应与异常边界

`claim-common` 持有无领域含义的 `ApiResult<T>` 响应契约。全局异常映射器属于具体服务的 `adapter/web` 边界，待该服务提供实际 HTTP API 时按 `standards/error-handling.md` 落地；不得以空的 Operations 服务承载其他服务的异常处理。

异常处理器优先读取已有 `traceId`；若缺失则生成新的关联标识并输出不含敏感内容的结构化日志。未知异常仅返回 `INTERNAL_ERROR`，不回显异常消息或堆栈。

## DS-PLT-004 测试策略

- Compose 配置使用 `docker compose config` 校验。
- `claim-common` 使用单元测试验证 `ApiResult<T>` 的成功与失败响应字段。
- 使用 `mvn verify` 验证全部 Maven 模块可编译和测试。

## 失败与补偿

- MySQL 未就绪时服务启动失败是可见的基础设施失败，不回退为内存数据库或创建隐式业务数据。
- Compose 初始化脚本只在空数据卷首次创建时执行；需要重新初始化时必须显式删除本地卷，避免误清空数据。
- 本 Spec 不发布领域事件，因此不创建 Outbox；后续签收服务发布下游事件时必须由其功能 Spec 定义 Outbox 与消费者去重。
