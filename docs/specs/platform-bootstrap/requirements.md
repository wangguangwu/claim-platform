# 平台工程启动需求

## 项目背景与目标

仓库当前只保存架构与 Feature Spec，尚不能构建、启动或验证服务。需要先建立不承载领域事实的工程与本地运行基础，使后续 `sign-core` 与 `sign-adaptation-collaboration` 能在统一模块、依赖版本、运行环境和可观测上下文上实现，而非各自搭建。

## 目标

- 建立可构建的 Maven 多模块工程：`claim-common`、`claim-gateway`、`claim-sign-service`、`claim-case-push-service`。
- 提供 Docker Compose 本地运行环境，覆盖 MySQL、Redis、RocketMQ、Nacos、XXL-Job 与兼容 OSS 的本地对象存储，并使三个后端服务可以以本地 profile 启动。
- 提供各服务共用的稳定技术契约：`ApiResult<T>`、`traceId` 透传与生成、基础健康检查，以及不含敏感信息的错误响应与日志上下文。
- 网关具备服务路由、Trace 透传与本地开发所需的基础配置，但不提前实现未确认的认证与租户识别协议。
- 仅在 `local` profile 提供 Gateway 校验的开发测试凭证到测试 `TenantContext` 的映射，使租户隔离测试有可信输入；该凭证不可用于生产，非 `local` profile 必须拒绝该入口。

## 非目标

- 不实现 `Sign`、`Material`、`ClaimIssue`、`CaseRelation`、`InteractionLog` 或任何案件推送领域事实。
- 不创建 `claim_sign_db` 与推送库的业务表、业务迁移脚本或领域 Repository；它们分别归属 `sign-core` 与后续案件推送 Feature。
- 不定义签收 HTTP 字段、MQ Topic/Tag、消费者组、问题件定位标识，亦不实现生产租户认证、租户报文验签、加解密或 Parser / Validator / Converter。
- 不实现承保、审核、保司等外部业务系统调用，不将本地对象存储作为生产 OSS 选型。
- 不建设看板或生产告警规则。

## 所属上下文与范围

- 所属上下文：Edge & Platform 与 Platform Common；实现模块为 `claim-gateway`、`claim-common`，并为两个领域服务建立可运行骨架。
- 网关只承接认证、租户识别、路由、入口限流与 Trace 透传的技术边界；本 Feature 仅落地其中不依赖未确认认证协议的路由、限流配置与 Trace 能力。
- `claim-common` 仅包含无领域含义的稳定技术契约，不能引用任一服务的领域类型、表、Repository 或业务规则。

## 验收标准

- 在干净环境中，按仓库文档执行一次 Maven 构建即可通过 `mvn verify`。
- Docker Compose 能启动全部本地依赖并报告健康状态；停止与重新启动不会依赖人工创建中间件实例。
- 三个后端服务能使用 local profile 启动，健康检查可用，并连接 Compose 提供的依赖；任意服务的启动失败应能从安全日志和 `traceId` 定位。
- 经网关转发的请求携带或生成同一个 `traceId`；响应遵循 `ApiResult<T>`，不会暴露堆栈、SQL、令牌或跨租户信息；`local` 测试凭证只能建立配置中允许的测试 `TenantContext`。
- 本 Feature 不产生任何签收或推送领域表、领域 API、消息 Topic/Tag 与租户业务协议实现。
