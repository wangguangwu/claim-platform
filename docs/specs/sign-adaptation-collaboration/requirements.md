# 签收适配与协同需求

## 项目背景与目标

签收内核必须保持稳定，但租户协议与承保、审核、OSS 等外部边界按租户和系统变化。本 Feature 在不侵入领域模型的前提下交付这些快变边界的抽象与平台侧实现（含 `local` 测试租户）并复用 `sign-core` 的标准命令、查询与恢复端口；某个真实租户的具体报文协议不在本 Feature 范围内，由该租户自己的接入 Spec 承接。

## 目标与非目标

### 目标

- 在 `interfaces/acl` 实现 `TenantAdapter` 契约、`TenantAdapterRouter` 与 Pipeline 编排，完成租户报文验签、加解密、解析、校验、标准模型转换与响应保护，并用 `local` profile 的测试租户 Pipeline 验证这条抽象链。
- 在平台契约 A 档经用户确认后，先交付 `POST /api/v1/signs` 同步创建子资源（压测与端到端入口的前置项），再交付材料、问题件、撤销子资源与签收/材料/问题件查询入口、MQ 路由和 Consumer。
- 用 RocketMQ 投递实现覆盖 `sign-core` 事件端口的日志级默认实现，使异步受理链路真正端到端运行。
- 通过 `infrastructure/insured`、`infrastructure/audit`、`infrastructure/oss` 隔离承保、审核与材料协议；所有外部调用携带既定幂等键并追加安全的 `InteractionLog`。
- 实现审核结果、问题件、回复、补材、撤销等协同编排，复用内核状态机和补偿端口，不在 Controller、Consumer 或 Adapter 复制状态判断。
- 在 Docker Compose 本地环境使用 Gatling Java 仅压测同步签收提交与三层幂等；依赖不可达或结果无法验证时必须失败，不得跳过。

### 非目标

- 不修改五个签收聚合、8 态状态机、Flyway 迁移机制、幂等语义或 XXL-Job 基础注册；这些归属 `sign-core`。
- 不实现任何真实租户的验签算法、加解密方式与报文字段，也不用测试租户 Pipeline 代替它们。每个真实租户的协议、凭证与映射由该租户自己的接入 Spec（`tenant-<code>-onboarding`）承接，本 Spec 只提供它必须落进的抽象。
- 不定义生产认证协议或以业务请求字段伪造租户身份；本地仅使用 `platform-bootstrap` 的 Gateway 测试凭证。
- 不自行猜测审核、承保、案件或租户系统的真实报文；契约 B 档未确认时，只保留被阻塞项，不落地外部客户端。

## 核心业务规则

- Tenant Adapter 按 `tenantCode + protocolVersion` 路由，差异不得以 `if (tenantCode)` 散落到应用层或领域层。
- 租户侧的问题件定位字段只在 ACL 内映射为平台 `issueNo`；应用层与领域层不认识租户侧命名，也不引入第二套问题件标识。
- HTTP、MQ 与外部回调进入系统前必须取得受信任 `TenantContext`；请求体、路径和查询参数中的租户字段只能作为待校验业务数据，不能建立身份。
- 外部调用在事务外执行，状态推进始终经过 Sign 聚合；重复、乱序和终态迟到事实只写安全审计与结构化信号，不产生额外副作用。
- 平台告警通道未交付前，补偿超时、积压和外部失败以基线 §10 的结构化日志与 `InteractionLog` 留痕，不将其宣称为已触达告警。

## 所属上下文与范围

- 所属限界上下文：Sign；实现模块：`claim-sign-service` 的 `interfaces/acl`、`interfaces/sign|material|issue|internal`、`infrastructure/insured|audit|oss|mq`。
- 依赖 `sign-core` 已交付稳定聚合、命令、查询、迁移与恢复端口；依赖 `platform-bootstrap` 的本地可信测试凭证。
- 平台契约 A 档确认并回写后只解除 HTTP 子资源、MQ 路由、错误映射、消费失败口径和问题件对外定位字段命名这些**已确认项**的实现阻塞；未确认项继续阻塞，不因同档其他项已确认而放开。B 档外部报文只阻塞对应 Adapter 或 Consumer，不阻塞其他已确认能力；真实租户协议不在本 Spec 的解锁范围内。

## 验收标准

- `local` 测试租户 Pipeline 覆盖验签、解密、解析、校验、转换与响应保护全链，`TenantAdapterRouter` 的命中与未命中路径均有测试；应用与领域包不存在租户分支，也不出现租户侧问题件定位字段名。真实租户的协议测试归属其自己的接入 Spec，本 Spec 不得声称已接入任何真实租户。
- `POST /api/v1/signs` 先于其他子资源可用，使同步创建链路与三层幂等压测可执行；RocketMQ 投递实现覆盖内核默认实现后，异步受理链路可端到端跑通架构基准 §4.2。
- 已确认的 HTTP 与消息契约具有契约测试；重复或乱序消息、外部重试和终态迟到回调不推进状态或制造重复副作用。
- 外部调用携带 `signNo` 或已登记幂等键，审计只保存允许的摘要与错误码；超时、积压和恢复失败可由结构化字段及 `traceId` 定位。
- Gatling 仅覆盖同步提交和三层幂等：同键并发只创建一个 Sign，不同键不发生错误关联，并产出吞吐量、P95/P99、错误率与重复创建数；本地依赖不可达、断言失败或结果缺失均使压测失败。
- 后端测试和 `mvn verify` 通过；B 档未确认项在任务中保持未完成并注明阻塞来源。
