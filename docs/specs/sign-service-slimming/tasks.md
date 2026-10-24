# 签收服务精简任务

顺序要求：第 1 条未完成前不得开始任何删除；第 2 条（基线回写）先于代码删除。

- [x] 1. 取得用户对 `POST /api/v1/tenant-intakes` 删除与请求体四字段移除的明确确认
- [ ] 2. 回写 `docs/specs/baseline.md`：§7 敏感信息补充人员明细边界、§8 保留契约表（HTTP 路径行、错误码行）、§8 已知偏差表（第 1 行现状、其余项标注暂缓、新增四列不再写入）、§12 变更记录
- [ ] 3. 删除簇 A 主代码：`interfaces/tenant/**`（24 文件）、`TenantSignController`、`TenantSignContextResolver`
- [ ] 4. 删除簇 A 对外契约：`SignWebContract` 的 `TENANT_INTAKES` 路径与 `PROTOCOL_VERSION` 头/模式，`SignExceptionHandler` 的三个租户 handler，`SignErrorCode` 的 `MALFORMED_REQUEST` 与 `TENANT_PROTOCOL_NOT_SUPPORTED`
- [ ] 5. 删除簇 B 主代码：`domain/model/person/**`、`infrastructure/persistence/dataobject/person/**`、`CertificateType`、`PersonConverter`、`PersonRequestConverter`
- [ ] 6. 收缩人员建模链路：`SignRequest`、`SignCommand`、`Sign`、`SignState`、`SignCreationService`、`SignController`、`SignDO`、`SignPersistenceConverter`
- [ ] 7. 删除簇 A/B 测试：`interfaces/tenant/**`（16 文件）、`TenantSignControllerTest`、`TenantSignContextResolverTest`、`PersonConverterTest`、`PersonRequestConverterTest`
- [ ] 8. 同步剩余测试：`SignNoGenerationIntegrationTest`（双入口用例改单入口）、`SignCommandHandlerTest`、`SignSubmissionIntegrationTest`、`SignCreationServiceTest`、`SignCommandTest`、`SignTest`、`SignPersistenceConverterTest`、`SignControllerTest`
- [ ] 9. 残留自检：全仓不再出现 `interfaces.tenant`、`domain.model.person`、`*InfoDO`、`TenantSignController` 引用；无只被已删代码使用的残留类型
- [ ] 10. 数量核对：主代码 90 文件 / 4166 行、测试 26 文件 / 3676 行，与设计 §5 偏差在 ±3% 内，超出则回改设计
- [ ] 11. 确认 `claim-platform-mysql` 与 `claim-platform-redis` 健康后执行 `mvn -B clean verify`，核对集成用例执行数不为零
- [ ] 12. 显式 `git add` 本任务文件后提交，提交说明记录删除文件数、行数与契约影响
