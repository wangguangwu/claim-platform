# 签收适配与协同任务

- [ ] 在平台契约 A 档经用户确认并回写后，实现 `TenantAdapter` 契约、`TenantAdapterRouter` 与 Pipeline 编排（验签/解密 → `Parser` → `Validator` → `Converter` → 响应保护），并用 `local` profile 的测试租户 Pipeline 跑通全链；不自造任何真实租户的验签算法、加解密方式或报文字段。
- [ ] 在 A 档 HTTP 创建字段、幂等键与错误映射确认后，先实现 `POST /api/v1/signs` 同步创建子资源与 `interfaces/advice` 边界映射；它是本 Spec 内 Gatling 压测和首个可请求入口的前置项。
- [ ] 在 A 档其余 HTTP 字段确认后，实现材料、问题件、撤销子资源及签收/材料/问题件查询 Controller；租户侧问题件定位字段在 ACL 内映射为平台 `issueNo`，不进入应用层与领域层。
- [ ] 在 A 档 MQ 路由、重试和消费失败口径确认后，用 RocketMQ 投递实现覆盖内核事件端口的日志级默认实现，并实现消息入口与 `application/task` 消费编排。
- [ ] 在 B 档审核/承保报文、幂等键、超时和错误映射确认后，实现 `infrastructure/insured`、`infrastructure/audit` 与 OSS Adapter；未确认时保持此项阻塞，不以假定 DTO 落地。
- [ ] 在已确认的审核契约下，实现审核结果、问题件下发、问题件回复、补材和撤销通知协同；复用内核命令与 Sign 聚合状态校验，回复与补材按 `signNo + issueNo` 绑定，并追加独立事务 `InteractionLog`。
- [ ] 为内核补偿 Handler 实现外部协同恢复端口：按状态补发事件或重试幂等调用，`WAIT_AUDIT_ACCEPT` 只记录超时信号，审核任务投递失败时保持原状态不推进。
- [ ] 补齐 ACL、HTTP/消息契约、Adapter、重复/乱序/终态迟到、撤销与交互审计测试，执行 `mvn verify` 后勾选实际完成任务。
- [ ] 在 Docker Compose 健康环境运行 Gatling Java 同键与不同键并发压测，只压同步提交与三层幂等；依赖不可达、断言失败或报告缺失均使任务失败，不得跳过。
