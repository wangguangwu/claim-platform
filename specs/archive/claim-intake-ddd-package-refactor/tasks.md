# Claim Intake DDD 包结构重构任务

| 任务 | 关联需求与设计 | 状态 | 完成证据 |
| --- | --- | --- | --- |
| TK-REF-001 建立重构 Spec 与追踪链路 | FR-REF-001 至 FR-REF-005、DS 全部 | Done | 本目录需求、设计、任务文档齐全，明确范围、兼容性与测试策略 |
| TK-REF-002 迁移输入边界与应用服务包 | FR-REF-001、FR-REF-002、DS 包结构与类型映射 | Done | `adapter/web` 已迁至 `interfaces/web/**`；Application Service 已迁至 `application/service`；新增 `PersonRequestConverter` 在输入边界完成 JSON→领域对象转换，Controller 不依赖 Infrastructure |
| TK-REF-003 分类迁移持久化类型并消除 Mapper 命名歧义 | FR-REF-001、FR-REF-004、DS 核心链路设计 | Done | DO、MyBatis Mapper、Converter、Store 实现和配置已分别迁至 `dataobject`、`mapper`、`converter`、`repository`、`config`；`ClaimIntakePersistenceMapper` 已更名为 `ClaimIntakePersistenceConverter`，实现类为 `IntakePersistenceRepositoryImpl` |
| TK-REF-004 迁移测试并验证行为兼容 | FR-REF-003、FR-REF-005、TEST-REF-001 至 TEST-REF-006 | Done | 已迁移现有测试并新增 `PersonRequestConverterTest`；`mvn -pl backend/claim-intake-service -am test` 通过（公共模块 2 项、Claim Intake 19 项）；`mvn verify` 通过全部 6 个模块 |
| TK-REF-005 回写迁移说明、限制与测试证据 | FR-REF-005、DS 测试策略 | Done | 无数据库、配置或数据迁移，无 HTTP 契约变更；旧 Java 包不保留兼容副本；存在 3 条既有 Lombok `equals/hashCode` 编译警告，未在本次目录重构范围内修改，构建成功 |

## 迁移说明

- 部署无需执行数据库迁移或修改 Redis、MySQL 等配置，以本次构建产物替换原服务即可。
- Java 包名为模块内部实现，不作为对外兼容契约；旧 `adapter` 包和旧转换器类名已移除，模块内调用方均已更新。
- HTTP 路径、请求字段、响应信封、错误码、聚合状态机、幂等键、事务边界及交互日志行为保持不变。

## 已知限制

- `AccidentPersonInfoDO`、`MainInsuredInfoDO`、`PayeeInfoDO` 保留既有 Lombok `equals/hashCode` 编译警告；该问题与本次包结构迁移无关，未修改其对象相等性语义。
