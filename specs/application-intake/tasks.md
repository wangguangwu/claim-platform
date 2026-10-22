# 签收申请接入任务

| 任务 | 关联需求与设计 | 状态 | 完成证据 |
| --- | --- | --- | --- |
| TK-APP-001 建立签收领域 Spec 与追踪链路 | FR-APP-001 至 FR-APP-007、DS 全部 | Done | 本目录的需求、设计、任务文档齐全 |
| TK-APP-002 建立 Docker 签收建表脚本 | FR-APP-006、DS 核心数据模型 | Done | `docker/mysql/init/02-create-sign-tables.sql` 严格使用确认的四张表、字段注释与租户索引 |
| TK-APP-003 实现聚合、命令服务和持久化端口 | FR-APP-001 至 FR-APP-004、DS 主状态机、并发控制 | Done | MyBatis-Plus Mapper、乐观锁拦截器和事务适配器已实现；申请、材料与交互日志参加同一 Spring 事务 |
| TK-APP-004 实现 HTTP Adapter、租户边界和异常映射 | FR-APP-005、FR-APP-007、DS 接口契约 | Done | 已实现 3 个 `/api/v1/sign-applications` 接口、可信调用校验和稳定错误响应；`SignApplicationControllerTest` 覆盖成功和 404 契约 |
| TK-APP-005 覆盖状态机单元测试并执行构建校验 | TEST-APP-001 至 TEST-APP-003 | Done | `SignApplicationTest` 覆盖创建、上传与非法状态，`SignApplicationControllerTest` 覆盖 HTTP 契约，`SignApplicationCommandServiceTest` 覆盖天然幂等与唯一键竞争回读；2026-10-22 `mvn verify`（6 模块）通过，Docker Compose 配置通过，应用以 MyBatis-Plus 3.5.8 成功启动至 8081 |
| TK-APP-006 收敛 DDD 包结构并建立 DO/Domain 映射边界 | DS 分层与包结构、TEST-APP-004 | Done | 已移除空 `sign` 包、应用层改为 `usecase`、数据对象改为 `*DO`；MapStruct 生成 DO/领域状态映射，`SignApplicationPersistenceMapperTest` 覆盖 3 个转换场景，2026-10-22 `mvn verify` 通过 |
