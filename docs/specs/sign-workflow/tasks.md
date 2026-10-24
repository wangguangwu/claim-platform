# 签收流程任务

- [ ] 确认签收、材料、问题件、内部结果的 HTTP/事件契约与错误码，并回写稳定对外契约至基线。
- [ ] 建立 Maven 多模块工程及 `claim-common`、`claim-gateway`、`claim-sign-service` 最小可构建骨架。
- [ ] 创建 Sign、Material、SignTask、ClaimIssue、CaseRelation、InteractionLog 聚合及 `SignTask` 执行历史模型。
- [ ] 编写无物理外键的 `claim_sign_db` 迁移脚本、唯一约束和逻辑关联查询索引。
- [ ] 实现 `SignNo` 与各业务编号发放、事实库恢复、租户隔离及签收提交幂等回放。
- [ ] 实现签收落表、初始材料记录、初始 `MATERIAL_PROCESS` 任务、提交审计与事务回滚。
- [ ] 实现任务 Dispatcher、RocketMQ Producer/Consumer、条件领取、重复消费去重、重试、死信与 XXL-Job 恢复。
- [ ] 实现材料上传/处理子流程、材料状态推进、材料缺失问题件和补材恢复。
- [ ] 实现承保查询 Adapter、`INSURED_QUERY` 子流程、承保不符问题件和可恢复处理。
- [ ] 实现审核同步 Adapter、`AUDIT_SYNC` 子流程、审核问题件、内部结果接收与 `CaseRelation` 维护。
- [ ] 实现问题件通知、回复同步、恢复阶段校验及撤销终态保护。
- [ ] 实现任务执行日志、跨系统交互日志、脱敏日志及按 `traceId` / `signNo` / `taskNo` 检索所需索引；不新增运营查询服务。
- [ ] 补齐领域、任务、持久化、接口和集成测试，执行 `mvn verify` 后勾选实际完成任务。
