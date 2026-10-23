# 签收申请接入需求

## 范围

本功能属于 Claim Intake 限界上下文，实现线上签收申请的接收、材料登记和按签收编号查询。该上下文独占申请、材料、问题件和交互日志的写入权。

## 非范围

- 不实现承保信息查询、外部审核系统同步、问题件反馈、补材重提、案件撤销及案件推送。
- 不实现 Gateway 的身份认证、路由和限流；Claim Intake 仅接受 Gateway 已鉴别后透传的受信任租户头。
- 不存储影像二进制内容，不下载或访问材料 URL。

## 验收需求

### FR-APP-001 接收签收申请

WHEN Gateway 以受信任租户身份提交包含外部申请编号的签收命令 THEN THE SYSTEM SHALL 在该租户下创建唯一的签收申请、生成平台签收编号，并将状态置为 `IMAGE_NOT_UPLOAD`。

### FR-APP-002 申请天然幂等

WHEN 同一租户再次提交相同的 `externalApplyNo` THEN THE SYSTEM SHALL 返回首次创建的 `signNo` 和当前状态，且不得重复创建申请。

### FR-APP-003 外部申请租户隔离

WHEN 不同租户提交相同的 `externalApplyNo` THEN THE SYSTEM SHALL 分别创建各自的签收申请，且查询不可跨租户可见。

### FR-APP-004 登记材料

WHEN 已存在的签收申请接收到至少一条材料登记命令 THEN THE SYSTEM SHALL 原子保存全部材料、将申请状态从 `IMAGE_NOT_UPLOAD` 或 `ISSUE_CASE` 迁移至 `IMAGE_UPLOADED`，并记录材料已上传交互日志。

### FR-APP-005 查询签收申请

WHEN 已鉴别租户查询本租户的签收编号 THEN THE SYSTEM SHALL 返回申请摘要及未删除的材料列表；该编号不存在或属于其他租户时返回 `CLAIM_INTAKE_NOT_FOUND`。

### FR-APP-006 原子交互日志

WHEN 签收申请或材料上传本地写入成功 THEN THE SYSTEM SHALL 在同一数据库事务追加不含敏感材料内容的交互日志。

### FR-APP-007 租户与边界校验

WHEN 请求缺少受信任的 Gateway 租户头或内部调用凭证 THEN THE SYSTEM SHALL 拒绝请求，且不得使用请求体中的租户字段决定数据范围。

## 失败行为与错误码

| 错误码 | HTTP 状态 | 触发条件 | 调用方行为 |
| --- | --- | --- | --- |
| `REQUEST_VALIDATION_FAILED` | 400 | 必填字段、格式或材料列表不合法 | 修正后重试 |
| `UNTRUSTED_CALLER` | 401 | 缺失或错误的内部调用凭证 | 经 Gateway 调用，不重试原请求 |
| `TENANT_CONTEXT_MISSING` | 400 | 缺失受信任租户头 | 经 Gateway 补齐上下文后重试 |
| `CLAIM_INTAKE_NOT_FOUND` | 404 | 当前租户不存在签收编号 | 确认编号和租户 |
| `INVALID_INTAKE_STATUS` | 409 | 当前状态不可上传材料 | 刷新申请状态后处理 |
| `CLAIM_INTAKE_CONCURRENT_MODIFICATION` | 409 | 乐观锁发现申请已被并发更新 | 重新查询后决定是否以新键提交 |

## 幂等与兼容性影响

- 申请签收以 `tenantCode + externalApplyNo` 唯一约束作为天然幂等键；重复签收回放既有申请。原始材料表未定义请求键或批次唯一约束，材料上传的请求级幂等将在补材功能明确契约后实现。
- 表中 `tenant_code` 对应根需求中的 `tenantId`，只来自经受信任调用链传递的租户上下文。
- 当前 API 为 `/api/v1/claim-intakes`，错误码为 `CLAIM_INTAKE_NOT_FOUND`、`INVALID_INTAKE_STATUS`、`CLAIM_INTAKE_CONCURRENT_MODIFICATION`；该 API 尚未对外发布，本次由 `sign-applications` 更名不产生兼容性负担。`sign` 命名仅保留在业务编号 `sign_no` 上，数据库为 `claim_intake_db` 下的 `t_claim_intake`、`t_claim_intake_material`、`t_claim_intake_issue`、`t_claim_intake_interaction_log`、`t_claim_intake_case_rel` 五张表，未发布领域事件。

## 数据库初始化与已知限制

- Docker 首次创建 MySQL 数据卷时执行 `docker/mysql/init/02-create-claim-intake-tables.sql`，在 `claim_intake_db` 创建上述五张业务表。已有数据卷不会重复执行初始化脚本；本地需要重建表时必须显式重建开发数据卷，不允许应用启动时覆盖 Schema。
- 当前功能不包含外部审核同步、消息发布或补材请求级幂等；这些能力必须在后续 Spec 明确其表结构与契约后实现。
