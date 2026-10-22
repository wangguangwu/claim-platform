# 签收申请接入需求

## 范围

本功能属于 Application Intake 限界上下文，实现线上签收申请的接收、材料登记和按签收编号查询。该上下文独占申请、材料、问题件、交互日志、幂等记录和 Outbox 的写入权。

## 非范围

- 不实现承保信息查询、外部审核系统同步、问题件反馈、补材重提、案件撤销及案件推送。
- 不实现 Gateway 的身份认证、路由和限流；Application Intake 仅接受 Gateway 已鉴别后透传的受信任租户头。
- 不存储影像二进制内容，不下载或访问材料 URL。

## 验收需求

### FR-APP-001 接收签收申请

WHEN Gateway 以受信任租户身份提交包含外部申请编号的签收命令 THEN THE SYSTEM SHALL 在该租户下创建唯一的签收申请、生成平台签收编号，并将状态置为 `IMAGE_NOT_UPLOAD`。

### FR-APP-002 请求幂等

WHEN 同一租户以相同的 `Idempotency-Key` 重试同一签收或材料上传命令 THEN THE SYSTEM SHALL 返回首次成功产生的签收编号和状态，且不得再次写入申请、材料或 Outbox 事件。

### FR-APP-003 外部申请去重

WHEN 同一租户提交已存在的 `externalApplyNo` 且使用新的幂等键 THEN THE SYSTEM SHALL 拒绝请求并返回 `SIGN_APPLICATION_ALREADY_EXISTS`；不同租户的同值编号互不冲突。

### FR-APP-004 登记材料

WHEN 已存在的签收申请接收到至少一条材料登记命令 THEN THE SYSTEM SHALL 原子保存全部材料、将申请状态从 `IMAGE_NOT_UPLOAD` 或 `ISSUE_CASE` 迁移至 `IMAGE_UPLOADED`，并记录材料已上传交互日志。

### FR-APP-005 查询签收申请

WHEN 已鉴别租户查询本租户的签收编号 THEN THE SYSTEM SHALL 返回申请摘要及未删除的材料列表；该编号不存在或属于其他租户时返回 `SIGN_APPLICATION_NOT_FOUND`。

### FR-APP-006 原子事件意图

WHEN 签收申请或材料上传本地写入成功 THEN THE SYSTEM SHALL 在同一数据库事务追加版本化 Outbox 事件和安全的交互日志，供后续异步审核同步使用。

### FR-APP-007 租户与边界校验

WHEN 请求缺少受信任的 Gateway 租户头、内部调用凭证或幂等键 THEN THE SYSTEM SHALL 拒绝请求，且不得使用请求体中的租户字段决定数据范围。

## 失败行为与错误码

| 错误码 | HTTP 状态 | 触发条件 | 调用方行为 |
| --- | --- | --- | --- |
| `REQUEST_VALIDATION_FAILED` | 400 | 必填字段、格式或材料列表不合法 | 修正后以新的幂等键重试 |
| `UNTRUSTED_CALLER` | 401 | 缺失或错误的内部调用凭证 | 经 Gateway 调用，不重试原请求 |
| `TENANT_CONTEXT_MISSING` | 400 | 缺失受信任租户头 | 经 Gateway 补齐上下文后重试 |
| `SIGN_APPLICATION_NOT_FOUND` | 404 | 当前租户不存在签收编号 | 确认编号和租户 |
| `SIGN_APPLICATION_ALREADY_EXISTS` | 409 | 外部申请编号已被签收 | 查询既有申请 |
| `IDEMPOTENCY_KEY_REUSED` | 409 | 相同键对应不同命令内容 | 使用新键重新提交 |
| `IDEMPOTENCY_REQUEST_IN_PROGRESS` | 409 | 相同键的首个请求尚未完成 | 使用同一键稍后重试 |
| `INVALID_APPLICATION_STATUS` | 409 | 当前状态不可上传材料 | 刷新申请状态后处理 |
| `SIGN_APPLICATION_CONCURRENT_MODIFICATION` | 409 | 乐观锁发现申请已被并发更新 | 重新查询后决定是否以新键提交 |

## 幂等与兼容性影响

- 写入接口要求 `Idempotency-Key`，幂等范围为 `tenantCode + operation + key`；同键必须使用相同请求摘要。
- 表中 `tenant_code` 对应根需求中的 `tenantId`，只来自经受信任调用链传递的租户上下文。
- 新增 API 为 `/api/v1/sign-applications`，未修改已发布 API。Outbox 事件为 `SignApplicationReceived.v1` 与 `SignMaterialsRegistered.v1`，采用只追加兼容字段的版本策略。

## 迁移说明与已知限制

- Flyway 在 `sign_db` 创建签收业务表；已有同名人工建表的环境必须先基线评审，不允许自动覆盖。
- Outbox 仅记录投递意图，发布器和外部审核反腐层尚未实现；因此事件不会在本功能中离开数据库。
