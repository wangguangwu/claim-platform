# 平台基础运行能力需求

## 范围

本 Spec 为本地开发提供 MySQL 运行依赖、现阶段后端模块的最小可启动骨架，以及稳定 HTTP 响应信封。所属限界上下文为 Edge & Platform 和 Platform Common。

## 非范围

- 不实现申请签收、材料处理、案件推送、审核决定或赔付决策。
- 不部署生产级 Nacos、Redis、RocketMQ、XXL-Job、Sentinel、监控系统或应用容器镜像。
- 不在本次建立业务表、业务迁移、领域事件或 Outbox；它们由后续 `application-intake` 功能 Spec 定义。
- 不实现 Operations & Diagnosis 服务、运营查询或异常诊断；出现明确业务用例后再以独立 Spec 创建其模块、数据库和接口。

## 验收需求

### FR-PLT-001 本地 MySQL

WHEN 开发者依据 Docker 运行说明启动本地依赖 THEN THE SYSTEM SHALL 启动健康检查可见的 MySQL 8 实例，并初始化 `sign_db` 和 `push_db` 两个相互独立的数据库。

### FR-PLT-002 最小可启动模块

WHEN 开发者在本地 MySQL 已就绪的条件下启动 Gateway、Application Intake 或 Case Delivery 模块 THEN THE SYSTEM SHALL 以独立 Spring Boot 进程启动，并通过 Actuator health 端点报告存活状态。

### FR-PLT-003 本地依赖边界

WHEN 本地启动配置未提供 Nacos、RocketMQ、Redis、XXL-Job 或外部系统 THEN THE SYSTEM SHALL 不因连接这些尚未启用的依赖而阻止基础进程启动。

### FR-PLT-004 稳定响应契约

WHEN 未来由 Gateway 暴露的 HTTP API 返回成功或已处理的失败结果 THEN THE SYSTEM SHALL 使用 `ApiResult<T>` 返回 `success`、`code`、`message`、`traceId` 和 `data` 字段。

## 兼容性影响

- 当前没有已发布的 HTTP API；`ApiResult<T>` 是后续由 Gateway 暴露 API 的初始稳定契约。
- 本地配置只适用于开发环境，生产配置必须由受控配置中心和密钥管理系统提供。
