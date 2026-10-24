# 平台工程启动任务

- [ ] 建立根 Maven 工程及 `claim-common`、`claim-gateway`、`claim-sign-service`、`claim-case-push-service` 模块，并锁定基线指定的依赖与构建插件。
- [ ] 在 `claim-common` 落地 `ApiResult<T>`、Trace 上下文和通用错误模型，验证不含领域依赖。
- [ ] 为三个可执行服务建立 local profile、Actuator 健康检查、MDC 安全日志与 `traceId` 生成/透传配置。
- [ ] 实现网关的本地路由、入口限流和 Trace 透传配置；不实现未确认的认证与租户识别协议。
- [ ] 编排 Docker Compose infra profile：MySQL、Redis、RocketMQ、Nacos、XXL-Job 与 OSS 兼容存储，并提供环境变量模板和健康检查。
- [ ] 为三个后端服务提供 Dockerfile 与 Compose app profile，支持构建后启动和健康检查。
- [ ] 编写基础模块、Trace、网关路由与错误响应测试，执行 `mvn verify`、Compose 配置校验及 infra/app 启动烟囱测试后勾选任务。
