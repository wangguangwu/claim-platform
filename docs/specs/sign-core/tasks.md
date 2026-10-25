# 签收内核任务

- [x] 在 `claim-sign-service` 建立五个签收聚合、领域异常、标识、不变量、8 态流转与三类终态保护。
- [x] 编写 `claim_sign_db` 的 Flyway 版本化 SQL：按架构基准 §3 的最终列交付五张表（含问题件 `issue_no` 与 `ext_json`）、唯一约束、逻辑关联与审计检索索引；验证空库迁移且无物理外键，对外的契约命名不进 DDL。
- [x] 实现 `SignNo` 发放与事实库恢复、`issueNo` 在 `tenantCode + signNo` 范围内发放与唯一性、基于 `TenantContext` 的租户范围校验和跨租户隔离。
- [x] 实现同步创建内核：快查回放、`tenantCode + externalApplyNo` 业务锁、唯一键冲突赢家回查，以及 Sign、初始 Material、初始状态、`SIGN_SUBMIT` 审计的创建事务。
- [x] 实现事件发布、业务锁、幂等回放、发号与恢复调度的应用端口；为事件发布端口提供只输出结构化日志、不建表不持久化的默认实现，不接入具体 MQ 路由或外部报文。
- [x] 实现状态恢复编排、XXL-Job 执行器、补偿 `JobHandler` 与本地调度定义；不可自动恢复时记录基线 §10 的结构化信号。
- [x] 实现 Sign、Material、ClaimIssue 的内核查询服务与领域结果模型，不创建 HTTP Controller 或外部 DTO。
- [x] 补齐聚合、幂等、迁移、租户隔离、创建事务、事件意图表达与补偿 Handler 测试，执行 `mvn verify` 后勾选实际完成任务。
