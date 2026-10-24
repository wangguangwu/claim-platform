# 签收流程设计

分层、状态机、数据模型、核心时序、三层幂等与状态补偿均已由 [签收服务架构基准](../../architecture/claim-sign-service.md) 固定，本文件不复述，只写把它落到代码时的包位置、实现决策、契约缺口与测试口径。需求口径见 [requirements.md](requirements.md)，任务拆分见 [tasks.md](tasks.md)。

## 落地映射

| 架构基准中的角色 | 落点 |
| --- | --- |
| Tenant Adapter + Pipeline（验签/加解密/Parser/Validator/Converter/响应加密） | `interfaces/acl`，`TenantAdapterRouter` 按 `tenantCode + protocolVersion` 选择 Pipeline |
| 同步签收、撤销、问题件回复接收 | `application/command`：`SubmitSignCommand` → `SignCommandHandler`，`RevokeSignCommand` → `SignCommandHandler`，`SubmitIssueReplyCommand` → `ClaimIssueCommandHandler` |
| 状态合法性、终态保护 | `domain/sign`，`Sign` 聚合方法，不在 Consumer 或 Repository 里判断流转 |
| 事件消费与状态推进编排 | 消息入口在 `interfaces`，编排在 `application/task` |
| 按状态补偿 | `application/task` 的恢复编排 + XXL-Job Handler，扫描落在 `infrastructure` |
| 查询 | `application/query`：`SignQueryService`、`MaterialQueryService`、`ClaimIssueQueryService` |
| 持久化 | `infrastructure/persistence`，五张表各一组 DataObject / Mapper / Converter / Repository |
| 出站 | `infrastructure/insured`（有效保单查询、自动承保）、`infrastructure/audit`（审核任务、问题件回复、撤销通知）、`infrastructure/oss`（影像与补材下载） |
| 业务锁、发号、回放缓存 | 端口在 `application/port`，实现在 `infrastructure/cache` |

## 关键实现决策

- **三层幂等落点**：第一层快查走 `SignRepository`；第二层用 Redisson 锁，键由 `tenantCode + externalApplyNo` 构成，锁不作为事实来源；第三层捕获唯一键冲突后回查并发赢家，回查不到按编号碰撞处理。
- **锁与事务边界**：锁只覆盖"读状态 → 校验 → 写状态"的短事务，事务内外链式包含影像 IO、承保调用、审核投递与事件发布；`signNo` 锁与创建锁是两种粒度，不复用同一键。
- **事件发布时机**：创建事务提交后发布，发布异常不回传给租户调用方，只记交互日志并交由补偿；禁止在事务内投递 MQ 或调用外部系统。
- **交互日志独立事务**：`InteractionLog` 不与业务状态同事务提交，避免拉长事务；写失败只告警，不阻塞主流程。
- **人员信息存储**：四个 JSON 列以字符串存取，MapStruct 只做序列化转换，不建领域对象；出参默认不携带这些列。
- **状态推进入口收敛**：所有推进经 `Sign` 聚合的显式方法（受理成功/失败、发起自动承保、提交审核、签收完成、进入问题件、恢复等待、撤销），非法流转在聚合内抛出并由 `interfaces/advice` 映射为 `INVALID_SIGN_STATUS`。
- **不重建任务模型**：补偿依赖状态与时间阈值扫描，不新增任务表、执行事件历史表或工作流引擎。

## 接口与契约缺口

以下项未确认，确认前不创建对应 Controller、DTO 与消息定义；确认后先回写基线 §8 与架构基准再编码：

- HTTP 子资源的路径与请求响应字段（材料、问题件、撤销），根路径固定 `/api/v1/signs`。
- MQ Topic/Tag 与消费组命名，事件载荷字段按基线 §7 已固定。
- 问题件缺业务定位标识：多次打回场景下租户回复与补材如何绑定到具体一条 `ClaimIssue` 尚未确定。
- 审核系统与承保系统的接口报文细节与错误码到 `SignErrorCode` 的映射。

领域错误码沿用基线 §8 的六项，状态模型变更不改上报值。

## 测试策略

- **聚合测试**：8 态合法与非法流转、三类终态保护、问题件进入与离开口径、撤销前置状态。
- **幂等测试**：并发创建的三层路径、唯一键冲突回查回放、锁未获取时的可重试错误、跨租户相同 `externalApplyNo` 互不影响。
- **消费测试**：重复事件按当前状态安全结束、影像失败置 `ACCEPT_FAILED`、自动承保以 `signNo` 幂等、审核任务提交后进入 `WAIT_AUDIT_ACCEPT`、已撤销申请不推进。
- **补偿测试**：每个可推进状态命中对应恢复策略、`WAIT_AUDIT_ACCEPT` 超时只告警不重复提交、`CLAIM_REVOKED` 撤销通知补发。
- **集成测试**：无物理外键的建表脚本、唯一约束与逻辑关联索引、租户过滤、应用层关联校验、创建事务回滚、交互日志独立事务。
- **适配层测试**：多租户 Pipeline 的验签、解密、解析、校验、转换与响应加密；租户分支不得进入用例与领域模型。
- **门禁**：后端改动执行 `mvn verify`。
