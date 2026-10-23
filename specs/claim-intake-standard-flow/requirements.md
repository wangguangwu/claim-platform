# 理赔申请签收标准流程需求

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Draft（需求与设计已完成，待评审后进入实现） |
| 所属限界上下文 | Claim Intake |
| 模块 | `backend/claim-intake-service` |
| 功能分支 | `feat/claim-intake-standard-flow`（实现前创建） |
| 关联根需求 | `specs/requirements.md` |
| 关联 Spec | `specs/application-intake/`、`specs/tenant-adapter-intake/` |
| 本次交付 | 标准签收命令、三级幂等、Redisson 业务锁、事务边界、材料与交互日志原子写入、错误映射、指标与测试 |

> 编号生成变更：签收号格式、发号方式和碰撞重试策略已由 `specs/sign-no-generation/` 取代。本文件的 FR-CISF-005、FR-CISF-009、FR-CISF-017 中与编号格式、重试次数和“数据库 Schema 不变”相关的描述，以及“非范围”中“不新增或修改数据库 Schema”的描述，以新 Spec 为准；`signNo` 字段名保持不变。

## 范围

本功能定义理赔申请签收的标准流程：在 Gateway 完成鉴权、租户识别和限流后，Claim Intake 从可信租户上下文接收标准签收命令，完成组件路由、报文转换、幂等回放和持久化。

```text
租户
→ Gateway：鉴权 / 租户识别 / 限流 / Trace
→ TenantIntakeContextResolver：建立 tenantCode + protocolVersion + traceId
→ TenantProtocolHandlerRegistry：按租户和协议版本选择组件
→ IntakeConvertPipeline：Parser → Validator → Converter
→ SubmitIntakeCommand：标准签收命令
→ ClaimIntakeCommandService：三级幂等与创建编排
→ MySQL：申请、材料、初始状态、交互日志
```

覆盖内容：

- 两个 HTTP 入口共用同一个标准签收应用用例；
- 第一层：按 `tenantCode + externalApplyNo` 的数据库快速查询；
- 第二层：Redisson 租户级业务锁与锁内二次查询；
- 第三层：MySQL `tenantCode + externalApplyNo` 唯一键最终兜底；
- 平台 `signNo` 生成、申请与材料元数据原子创建；
- 锁等待、Redis 不可用、唯一键冲突、编号碰撞和数据库异常的失败行为；
- 锁配置、指标、安全日志和测试要求。

## 非范围

- Gateway 的鉴权、租户识别、限流、Trace 注入和协议版本头透传实现；本功能只消费其可信透传结果。当前 `claim-gateway` 仅为可启动骨架，端到端能力需另立 Gateway Spec。
- 验签、解密和密钥管理；本功能以已完成验签、解密的受信任请求为起点，现有 `reference` 协议不包含密码学处理。
- 真实租户协议、XML 解析和真实租户编码表；仍按 `specs/tenant-adapter-intake/` 的边界推进。
- 材料二进制内容的存储、下载、访问和 OSS 配置；本功能只处理材料元数据。
- 承保查询、审核系统同步、问题件反馈、补材重提、案件撤销、Outbox、MQ 和下游同步。
- 材料上传用例 `uploadMaterials` 的状态机和请求级幂等；该用例保持现有行为。
- 将 `signNo` 重命名为 `intakeNo`；本 Spec 将图中的 `intakeNo` 映射为现有平台编号 `signNo`。
- 新增或修改数据库 Schema；现有唯一键已满足本流程，若实现发现需要新增索引，必须先更新本 Spec。
- 使用 Redis 作为申请、材料或状态的最终事实来源；Redis 只承载短期业务锁。

## 前置条件与职责边界

| 参与方 | 责任 | 本功能是否实现 |
| --- | --- | --- |
| Gateway | 校验调用方身份，识别并覆盖可信租户头，限流，透传 Trace 和 `X-Claim-Protocol-Version` | 否，外部依赖 |
| 服务入口 | 再次校验服务间凭证，拒绝伪造租户头，建立 `TenantContext` | 否，复用现有入口 |
| 组件路由 | 按 `tenantCode + protocolVersion` 精确选择 Parser、Validator、Converter | 否，复用现有注册表 |
| 转换 Pipeline | 按 Parser → Validator → Converter 固定顺序生成 `SubmitIntakeCommand` | 否，复用现有 Pipeline |
| 签收应用服务 | 快速查询、业务锁、二次查询、编号生成、创建事务、幂等回放 | 是 |
| Redisson | 按幂等键提供租户级短期互斥锁 | 是，新增适配器 |
| MySQL | 申请、材料、交互日志的唯一事实来源 | 是，复用现有表和唯一键 |
| 验签/解密 | 校验报文来源并解密业务载荷 | 否，上游能力 |

服务不得只依赖 Gateway 的透传结果。入口仍须校验服务间凭证和租户头，避免绕过 Gateway 直接调用服务。

## 核心约束

1. MySQL 是申请、材料和状态的唯一事实来源；Redis 锁只用于减少并发冲突，不得成为幂等正确性的唯一保证。
2. 申请幂等键固定为 `tenantCode + externalApplyNo`，三层幂等必须使用同一业务键。
3. 锁键必须包含当前租户，不得让不同租户的相同 `externalApplyNo` 互相阻塞。
4. 数据库快速查询、锁等待和锁释放不得包在数据库事务中；数据库事务只覆盖创建写入。
5. 锁必须在 `finally` 中释放，且只释放当前请求持有的锁；租约过期后不得强制删除其他请求的锁。
6. 任一层发现既有申请时必须回放既有 `signNo` 和当前状态，不得比较报文差异、创建材料或追加重复成功日志。
7. MySQL `tenantCode + externalApplyNo` 唯一键是最终去重屏障；唯一键冲突不得作为未知 500 返回。
8. 申请、命令携带的材料、初始状态和 `SIGN_RECEIVE` 成功日志必须在同一数据库事务内提交或回滚。
9. 所有入口必须共用同一个标准签收应用用例，不得在 Controller 或租户适配器中重复实现幂等和事务逻辑。
10. 日志、指标和异常响应不得包含完整报文、人员信息、证件号、手机号、银行卡号、令牌或堆栈详情。
11. 失败不得留下半成品申请、孤立材料或缺失成功日志；锁不可用时仍须保证同一幂等键最多创建一条申请。
12. 本功能不得改变既有 `/api/v1/claim-intakes`、`/api/v1/tenant-intakes` 的路径和成功响应结构。

## 验收需求

### FR-CISF-001 统一标准用例

WHEN 任一已鉴别入口提交标准签收命令 THEN THE SYSTEM SHALL 调用同一个 `ClaimIntakeCommandService.submit` 用例，并对所有入口应用相同的幂等键、锁、事务和回放语义。

### FR-CISF-002 第一层数据库快速查询

WHEN 标准签收命令进入应用层 THEN THE SYSTEM SHALL 在获取业务锁和开启数据库事务之前，按 `tenantCode + externalApplyNo` 查询既有申请；命中时直接返回既有 `signNo` 和当前 `status`，不得获取锁、写数据库或追加成功日志。

### FR-CISF-003 第二层业务锁

WHEN 第一层查询未命中 THEN THE SYSTEM SHALL 按 `tenantCode + externalApplyNo` 获取带有限等待时间、持有期间由 Redisson watchdog 自动续期的租户级锁，并在不持有数据库事务的情况下执行后续检查与创建。

### FR-CISF-004 锁内二次查询

WHEN 业务锁获取成功 THEN THE SYSTEM SHALL 在锁内再次按 `tenantCode + externalApplyNo` 查询；命中时返回既有 `signNo` 和当前 `status`，不得创建申请或材料。

### FR-CISF-005 平台签收编号

WHEN 锁内二次查询仍未命中 THEN THE SYSTEM SHALL 生成新的平台 `signNo`（`SIGN` 前缀加 20 位大写十六进制字符）再进入创建事务；`signNo` 不得直接取自租户报文。

### FR-CISF-006 原子创建

WHEN 开始创建申请 THEN THE SYSTEM SHALL 在同一数据库事务内写入申请、命令携带的全部材料元数据、初始状态和一条 `SIGN_RECEIVE` 成功交互日志；任一写入失败 SHALL 回滚该事务的全部写入。

### FR-CISF-007 初始状态

WHEN 标准命令的材料列表为空 THEN THE SYSTEM SHALL 将申请初始状态置为 `IMAGE_NOT_UPLOAD`；WHEN 材料列表非空 THEN THE SYSTEM SHALL 通过聚合行为将状态置为 `IMAGE_UPLOADED` 并保存全部材料元数据。

### FR-CISF-008 第三层唯一键兜底

WHEN 创建事务因 `tenantCode + externalApplyNo` 唯一键冲突而失败 THEN THE SYSTEM SHALL 回滚创建事务，将其视为并发请求已成功，在新事务中重新查询并返回既有 `signNo` 和当前 `status`；不得向调用方返回 500。

### FR-CISF-009 编号碰撞处理

WHEN 唯一键冲突后按 `tenantCode + externalApplyNo` 查询不到既有申请 THEN THE SYSTEM SHALL 将其识别为 `signNo` 碰撞，重新生成 `signNo` 并最多重试一次创建；重试仍失败时返回稳定的服务端错误。

### FR-CISF-010 锁等待超时

WHEN 在配置的等待时间内未获取业务锁 THEN THE SYSTEM SHALL 返回 HTTP 409 和错误码 `INTAKE_SUBMISSION_IN_PROGRESS`，不得写数据库；调用方可以使用相同幂等键退避重试。

### FR-CISF-011 Redis 不可用降级

WHEN Redis 连接失败或锁操作不可用 THEN THE SYSTEM SHALL 记录告警和降级指标，执行一次即时复查并继续依赖 MySQL 唯一键完成幂等；降级路径仍须保证同一幂等键最多创建一条申请，不得把 Redis 不可用当作业务失败。

### FR-CISF-012 锁释放

WHEN 标准签收用例结束（成功、失败或异常） THEN THE SYSTEM SHALL 在 `finally` 中仅释放当前请求持有的锁；续期失败、租约已过期或锁所有权已丢失时 SHALL NOT 强制删除锁，并记录告警。

### FR-CISF-013 幂等回放

WHEN 任一层查询命中既有申请 THEN THE SYSTEM SHALL 返回既有 `signNo` 和当前 `status`，不得比较业务字段、创建材料或追加重复的 `SIGN_RECEIVE` 成功日志。

### FR-CISF-014 租户隔离

WHEN 构造锁键、数据库查询或唯一约束 THEN THE SYSTEM SHALL 始终包含 `tenantCode`；不同租户的相同 `externalApplyNo` SHALL 互相独立，锁键不得包含姓名、证件号、手机号等个人信息。

### FR-CISF-015 可观测与安全

WHEN 标准签收流程执行 THEN THE SYSTEM SHALL 记录创建、回放、锁超时、锁降级和唯一键冲突的稳定指标，并记录 `tenantCode`、已知时的 `signNo`、`traceId` 和结果类别；SHALL NOT 记录原始报文、完整命令、证件号、手机号、银行卡号或令牌。

### FR-CISF-016 锁配置

WHEN 服务启动 THEN THE SYSTEM SHALL 从 `claim.intake.submission.lock` 绑定锁等待时间、watchdog 基础租期和键前缀，并校验其合法性；非法配置 SHALL 阻止服务启动。

### FR-CISF-017 兼容性

WHEN 本功能上线 THEN THE SYSTEM SHALL 保持既有 HTTP 路径、`ApiResult` 响应信封、`ClaimIntakeResult` 字段、`signNo` 语义、数据库 Schema 和既有状态转换不变；新增内容仅限于标准命令的材料字段、锁续期与配置、指标和 409 错误码。

### FR-CISF-018 失败分类

WHEN 标准流程发生失败 THEN THE SYSTEM SHALL 按失败类别返回稳定错误码：锁等待超时为 409，参数或报文错误保持 400，唯一键竞争成功回放为 200，其他数据库或未预期错误为 `INTERNAL_ERROR` 500，且响应不得包含 SQL、堆栈或内部异常信息。

### FR-CISF-019 锁续期

WHEN 标准签收用例持有业务锁的时间达到基础租期 THEN THE SYSTEM SHALL 由 Redisson watchdog 按基础租期的三分之一自动续期，保持同一幂等键互斥；WHEN 锁被显式释放、Redisson 客户端关闭、进程退出或续期失败 THEN THE SYSTEM SHALL 停止续期，锁最迟在基础租期结束后释放。持有线程退出但客户端仍存活时 watchdog 不会主动停止，因此入口 SHALL 在 `finally` 中释放锁；不得由应用层跨线程调用 `renew()` 或强制延长锁。

## 标准命令与材料

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| `tenantCode` | 可信租户上下文 | 不接受请求体或查询参数中的租户 |
| `traceId` | 可信 Trace 或服务生成 | 贯穿锁、事务、日志和响应 |
| `externalApplyNo` | 租户标准报文 | 幂等键组成部分，租户内唯一 |
| `applicantName`、`applicant` | 转换层 | 平台标准模型 |
| `accidentPerson`、`mainInsured`、`payee` | 转换层 | 可为空 |
| `materials` | 转换层 | 新增；材料元数据列表，可为空，不含二进制内容 |
| `extJson` | 转换层 | 协议扩展信息 |

- `reference` 适配器当前不产生材料，必须传空列表；既有 `/api/v1/claim-intakes` 提交入口同样传空列表，材料仍通过 `uploadMaterials` 登记。
- 真实租户协议支持随申请提交材料时，由该租户 Converter 生成材料元数据；本功能不新增材料编码表。
- 重放请求携带的材料不会补写、覆盖或重复插入；材料补充必须使用独立的材料上传用例。

## 失败行为与错误码

| 场景 | 内部异常 | 对外错误码 | HTTP 状态 | 调用方行为 |
| --- | --- | --- | --- | --- |
| 锁等待超时 | `IntakeSubmissionInProgressException` | `INTAKE_SUBMISSION_IN_PROGRESS` | 409 | 使用相同 `externalApplyNo` 退避重试 |
| Redis 连接或锁操作不可用 | 无（降级） | 无 | 200/正常业务结果 | 无需特殊处理；监控告警 |
| 第一、二、三层命中既有申请 | 无（回放） | `OK` | 200 | 使用返回的 `signNo` |
| `signNo` 碰撞重试后仍冲突 | 原始数据库异常 | `INTERNAL_ERROR` | 500 | 携带 `traceId` 联系平台，不得更换幂等键重复提交 |
| 其他数据库或未预期异常 | 原始异常 | `INTERNAL_ERROR` | 500 | 携带 `traceId` 联系平台；可使用相同幂等键重试 |
| 报文、校验或编码映射失败 | 转换层异常 | 保持既有错误码 | 400 | 按既有入口契约修正 |
| 缺少可信租户上下文 | `TenantContextMissingException` | `TENANT_CONTEXT_MISSING` | 400 | 经可信入口补齐上下文 |
| 材料上传状态冲突 | `InvalidIntakeStatusException` | `INVALID_INTAKE_STATUS` | 409 | 按材料上传用例现状处理 |

`INTAKE_SUBMISSION_IN_PROGRESS` 只表示同一幂等键的并发请求仍在处理中，不是业务冲突；调用方使用相同幂等键重试不会产生重复申请。

## 幂等与兼容性影响

- 幂等键保持 `tenantCode + externalApplyNo`，Redis 锁键不是新的业务幂等键。
- 第一层和锁内二次查询均为快速回放；第三层唯一键兜底保证锁失效、租约过期或 Redis 故障时的最终一致性。
- 重放不比较报文差异，不新增材料，不追加成功日志；同一幂等键的不同报文视为重复提交。
- `SubmitIntakeCommand` 新增材料列表属于模块内部契约变化，需要同步修改两个 Controller、`reference` Converter 和相关测试；不改变 HTTP 请求体。
- 新增配置项 `claim.intake.submission.lock.*`，提供安全默认值；非法配置启动失败。
- 新增错误码 `INTAKE_SUBMISSION_IN_PROGRESS`；既有错误码和 HTTP 路径不变。
- 不修改数据库 Schema，不新增迁移脚本；现有 `uk_tenant_external_apply_no` 和 `uk_tenant_sign_no` 作为最终约束。
- Redis 仅保存短期锁，进程崩溃后依赖租期自动释放；不使用 Redis 保存申请、材料或状态。
- 本功能不发布领域事件，不需要 Outbox；交互日志与业务写入同事务。

## 假设与待确认事项

### A-1 Gateway 与协议头

本 Spec 假设 Gateway 已完成鉴权、租户识别、限流和 Trace 注入，并在转发前覆盖客户端伪造的 `X-Claim-Tenant`、`X-Claim-Protocol-Version`、`X-Trace-Id`。当前仓库 `claim-gateway` 只有启动骨架，Gateway 实现和端到端验证需另立 Spec。

### A-2 验签与解密

本 Spec 假设进入标准流程的报文已完成验签和解密。现有 `reference` 协议不包含密码学处理；若后续要求在 Claim Intake 内完成，需新增独立 Spec 并扩展 Pipeline 前置阶段。

### A-3 材料进入标准命令

本 Spec 决策：`SubmitIntakeCommand` 承载材料元数据，由标准创建事务原子写入，解决 `specs/tenant-adapter-intake/` 的 A-1。`reference` 和既有 HTTP 入口当前传空列表；材料二进制存储仍不在范围内。该决策需在实现前确认，确认后回写旧 Spec 的 A-1。

### A-4 初始状态

材料列表为空时初始状态为 `IMAGE_NOT_UPLOAD`；材料列表非空时通过 `registerMaterials()` 迁移为 `IMAGE_UPLOADED`。该规则由标准命令的材料语义推导，需业务确认。

### A-5 Redis 不可用策略

本 Spec 选择“降级到数据库唯一键路径”，而不是直接失败。理由是根需求规定 Redis 不能作为业务终态来源，且唯一键已能保证幂等；代价是 Redis 故障期间数据库冲突和回滚增加。

### A-6 锁参数默认值

默认等待 3 秒、租期 10 秒、键前缀 `claim:intake:submit:`；键后缀为 `tenantCode + externalApplyNo` 的 SHA-256 十六进制摘要。默认值需在实现前确认，且租期应大于创建事务的最长预期耗时。

### A-7 同一幂等键的报文差异

本 Spec 不检测同一 `tenantCode + externalApplyNo` 下报文内容差异，统一按回放处理。若业务需要“幂等键相同但报文不同”返回冲突，需要新增报文摘要字段和独立 Spec。

### A-8 并发验收基础设施

并发和唯一键行为必须使用真实 MySQL/Redis 验证。优先复用 `docker/` 下的本地容器；是否引入 Testcontainers 在实现时按现有构建约束决定，若新增测试依赖需同步更新本 Spec 和 `pom.xml`。
