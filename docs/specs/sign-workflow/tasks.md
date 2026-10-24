# 签收流程任务

架构口径见 [签收服务架构基准](../../architecture/claim-sign-service.md)，实现决策与落点见 [design.md](design.md)。

- [ ] 创建 `Sign`、`Material`、`ClaimIssue`、`CaseRelation`、`InteractionLog` 五聚合，定义标识、不变量、幂等键与并发策略。
- [ ] 在 `Sign` 聚合实现 8 态合法流转与三类终态保护，非法流转抛领域异常。
- [ ] 编写无物理外键的 `claim_sign_db` 建表与变更脚本：唯一约束、逻辑关联索引。
- [ ] 实现 `SignNo` 发放与事实库恢复、`tenantCode` 隔离校验。
- [ ] 实现同步签收：三层幂等（快查回放、`tenantCode + externalApplyNo` 业务锁、唯一键冲突回查）与创建事务内的申请、材料关系、初始状态、提交交互日志。
- [ ] 实现租户适配层 `TenantAdapterRouter` 与 Pipeline：验签、解密、Parser、Validator、Converter、响应签名加密。
- [ ] 实现事务提交后的 `SIGN_ACCEPT` 事件发布与发布失败兜底。
- [ ] 实现 `SIGN_ACCEPT` 消费编排：影像处理、`ACCEPT_SUCCESS` / `ACCEPT_FAILED`、有效保单查询、自动承保、审核交付、`WAIT_AUDIT_ACCEPT`，锁只覆盖状态读写。
- [ ] 实现承保出站 Adapter（有效保单查询与 `signNo` 幂等自动承保）、审核出站 Adapter（审核任务与撤销通知）、OSS 影像与补材下载。
- [ ] 实现审核结果消费：`1:N` CaseRelation 落库、`SIGN_COMPLETED`、终态迟到回调只记录并告警。
- [ ] 实现问题件下发消费与租户下发；问题件回复与补材的同步接收、`ISSUE_REPLY` 异步处理与回到 `WAIT_AUDIT_ACCEPT`。
- [ ] 实现撤销：状态校验、`CLAIM_REVOKED`、已进入审核阶段时发布 `CLAIM_REVOKE`。
- [ ] 实现 XXL-Job 按状态恢复策略的补偿扫描与超时告警，只重发事件或重试幂等调用。
- [ ] 实现 `InteractionLog` 记录：首次提交的 `SIGN_SUBMIT` 日志随创建事务提交，其余交互日志独立事务写入；字段按基线 §10 脱敏，并建立按 `signNo` / `traceId` 检索所需索引。
- [ ] 实现签收、材料、问题件的查询服务与 `/api/v1/signs` 子资源接口，异常经 `interfaces/advice` 映射为基线 §8 错误码。
- [ ] 补齐聚合、幂等、消费、补偿、适配层与集成测试，执行 `mvn verify` 后勾选实际完成任务。
- [ ] 在 Docker Compose 本地环境接入 Gatling Java 签收提交压测，覆盖同一及不同幂等键并发请求，输出吞吐量、P95/P99、错误率和重复创建数基线。
