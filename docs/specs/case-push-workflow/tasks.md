# 案件推送流程任务

- [ ] 在用户确认后建立 `docs/architecture/claim-case-push-service.md`，定义 Push 领域状态机、数据模型、幂等与补偿策略，并与 `baseline.md` 对齐；同时把它登记进 `AGENTS.md` 的规则归属表与执行顺序、`README.md` 导航，并把 `baseline.md` 头部与 §12 的域内委派句指向该基准，不留下未登记的第二份权威。
- [ ] 完成案件生成、保司推送、回执与状态同步契约的确认和基线回写，解除本 Feature 的实现阻塞。
- [ ] 在 `claim-case-push-service` 建立 `PushTask`、`PushRecord`、`PushReceipt`、`PushStrategy` 聚合、迁移脚本和 MyBatis-Plus 持久化。
- [ ] 实现案件生成事件消费、任务幂等创建、策略选择与按租户/目标系统/Adapter 的受控并发。
- [ ] 实现 `InsurerPushAdapter`、推送记录、回执处理、状态同步及错误映射。
- [ ] 实现重试、超时结构化信号和恢复策略；告警通道未交付前按基线 §10 留存可定位日志与审计，确保不以缓存或消息系统作为业务终态。
- [ ] 补齐聚合、消息、Adapter、回执、并发和集成测试，执行 `mvn verify` 后勾选任务。
