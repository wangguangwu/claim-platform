# 后端常量治理与 MyBatis-Plus 查询重构任务

## 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | In Progress |
| 关联需求 | `specs/backend-constants-mybatis-plus/requirements.md` |
| 关联设计 | `specs/backend-constants-mybatis-plus/design.md` |
| Spec 分支 | `docs/backend-constants-mybatis-plus` |
| 实现分支 | `refactor/backend-constants-mybatis-plus`（基于 Spec 分支的子分支） |
| 验证环境 | 本地 MySQL 8.4 + Redis 7.4；`mvn -pl backend/claim-intake-service -am clean verify` |

## 任务清单

| ID | 任务 | 依赖 | 需求/设计回链 | 状态 | 证据 |
| --- | --- | --- | --- | --- | --- |
| TK-CMP-001 | 编写重构 Spec，明确范围、非范围、EARS 需求、兼容性和验证策略 | 无 | FR-CMP-001 至 FR-CMP-014、DS-CMP-001 | Done | `specs/backend-constants-mybatis-plus/requirements.md`、`design.md`、`tasks.md` |
| TK-CMP-002 | 新增持久化表/列/索引常量，并应用到三个数据对象 | TK-CMP-001 | FR-CMP-004、DS-CMP-002 | Pending | 待实现 |
| TK-CMP-003 | `SignNoSequenceMapper` 改为 `BaseMapper` + `QueryWrapper`，删除 `@Select` | TK-CMP-002 | FR-CMP-001、FR-CMP-002、DS-CMP-003 | Pending | 待实现 |
| TK-CMP-004 | `SignNoSchemaValidator` 改为 JDBC 元数据校验，更新单元测试 | TK-CMP-002 | FR-CMP-003、DS-CMP-004 | Pending | 待实现 |
| TK-CMP-005 | 新增 Web 契约常量，替换头、路径、校验长度与正则字面量 | TK-CMP-001 | FR-CMP-005、FR-CMP-012、DS-CMP-005 | Pending | 待实现 |
| TK-CMP-006 | 新增错误码枚举，处理器统一读取状态、文案和 `Retry-After` | TK-CMP-005 | FR-CMP-006、FR-CMP-012、DS-CMP-005 | Pending | 待实现 |
| TK-CMP-007 | 集中指标名称、标签键、标签值和结果值 | TK-CMP-001 | FR-CMP-007、DS-CMP-006 | Pending | 待实现 |
| TK-CMP-008 | 集中 Redis/Lua/缓存键常量，替换标记、下标、脚本名与摘要算法字面量 | TK-CMP-001 | FR-CMP-008、DS-CMP-006 | Pending | 待实现 |
| TK-CMP-009 | 增加签收号格式下标、未知值、退避边界和调度作用域常量 | TK-CMP-001 | FR-CMP-009、DS-CMP-007 | Pending | 待实现 |
| TK-CMP-010 | 新增交互类型/方向/结果枚举与参考协议常量 | TK-CMP-001 | FR-CMP-010、DS-CMP-008 | Pending | 待实现 |
| TK-CMP-011 | 配置属性默认值常量化 | TK-CMP-001 | FR-CMP-011、DS-CMP-009 | Pending | 待实现 |
| TK-CMP-012 | 新增架构测试，禁止 Mapper SQL 注解、内嵌 SQL 和非 `BaseMapper` Mapper | TK-CMP-003、TK-CMP-004 | FR-CMP-001、FR-CMP-013、DS-CMP-011 | Pending | 待实现 |
| TK-CMP-013 | 更新受影响测试并执行真实 MySQL/Redis 验证 | TK-CMP-002 至 TK-CMP-012 | FR-CMP-012 至 FR-CMP-014、DS-CMP-010/011 | Pending | 待实现 |
| TK-CMP-014 | 回写任务证据、验证记录，提交并推送功能分支 | TK-CMP-013 | 全部 | Pending | 待实现 |

## 需求追踪矩阵

| 需求 | 设计 | 任务 | 测试 |
| --- | --- | --- | --- |
| FR-CMP-001 Mapper 使用 MyBatis-Plus | DS-CMP-003 | TK-CMP-003、TK-CMP-012 | TEST-CMP-005、TEST-CMP-006 |
| FR-CMP-002 高水位查询等价 | DS-CMP-003 | TK-CMP-003 | TEST-CMP-007、TEST-CMP-008 |
| FR-CMP-003 启动索引校验无 SQL | DS-CMP-004 | TK-CMP-004 | TEST-CMP-009、TEST-CMP-010 |
| FR-CMP-004 持久化常量 | DS-CMP-002 | TK-CMP-002 | TEST-CMP-005 |
| FR-CMP-005 HTTP 契约常量 | DS-CMP-005 | TK-CMP-005 | TEST-CMP-001、TEST-CMP-002 |
| FR-CMP-006 错误映射集中 | DS-CMP-005 | TK-CMP-006 | TEST-CMP-003 |
| FR-CMP-007 指标契约不变 | DS-CMP-006 | TK-CMP-007 | TEST-CMP-011、TEST-CMP-012 |
| FR-CMP-008 Redis 与缓存常量 | DS-CMP-006 | TK-CMP-008 | TEST-CMP-013 |
| FR-CMP-009 领域边界与重试常量 | DS-CMP-007 | TK-CMP-009 | TEST-CMP-004 |
| FR-CMP-010 固定业务编码枚举化 | DS-CMP-008 | TK-CMP-010 | TEST-CMP-014 |
| FR-CMP-011 配置默认值命名 | DS-CMP-009 | TK-CMP-011 | TEST-CMP-015 |
| FR-CMP-012 外部行为零变化 | DS-CMP-005/010 | TK-CMP-005、TK-CMP-006、TK-CMP-013 | TEST-CMP-001、TEST-CMP-003、TEST-CMP-007 至 TEST-CMP-014 |
| FR-CMP-013 回归与架构门禁 | DS-CMP-011 | TK-CMP-012、TK-CMP-013 | TEST-CMP-006、全量回归 |
| FR-CMP-014 分层依赖不变 | DS-CMP-001 | TK-CMP-009、TK-CMP-012 | TEST-CMP-016 |

## 测试证据要求

| 测试 | 覆盖点 | 状态 | 证据 |
| --- | --- | --- | --- |
| TEST-CMP-001 | HTTP 头、路径、校验长度与正则使用统一常量，行为不变 | Pending | 待实现 |
| TEST-CMP-002 | 租户/协议上下文解析保持原有成功与失败分支 | Pending | 待实现 |
| TEST-CMP-003 | 错误码、状态、文案和 `Retry-After` 与重构前一致 | Pending | 待实现 |
| TEST-CMP-004 | 签收号格式、解析下标、未知值、退避边界不变 | Pending | 待实现 |
| TEST-CMP-005 | 数据对象表列映射与持久化常量一致 | Pending | 待实现 |
| TEST-CMP-006 | 主代码无 Mapper SQL 注解、内嵌 SQL，Mapper 均继承 `BaseMapper` | Pending | 待实现 |
| TEST-CMP-007 | MyBatis-Plus 高水位查询返回最大值、空值和异常分类 | Pending | 待实现 |
| TEST-CMP-008 | 真实 MySQL 上包含软删除、排除历史 `SIGN`、固定长度与日期区间 | Pending | 待实现 |
| TEST-CMP-009 | 存在 `uk_sign_no(sign_no)` 首列唯一索引时启动通过 | Pending | 待实现 |
| TEST-CMP-010 | 索引缺失、列不匹配、元数据异常时启动失败 | Pending | 待实现 |
| TEST-CMP-011 | 发号、降级、恢复、冲突指标名称与标签不变 | Pending | 待实现 |
| TEST-CMP-012 | 序列差值 Gauge 作用域与数值不变 | Pending | 待实现 |
| TEST-CMP-013 | Redis 脚本名、标记、结果解析和锁键结构不变 | Pending | 待实现 |
| TEST-CMP-014 | 交互类型、方向、结果与参考协议编码不变 | Pending | 待实现 |
| TEST-CMP-015 | 配置默认值与校验规则不变 | Pending | 待实现 |
| TEST-CMP-016 | 应用层/领域层不依赖存储与基础设施 | Pending | 待实现 |

## 完成门禁

- 生产代码中不存在 Mapper SQL 注解和内嵌 SQL 语句；
- 持久化表列名、HTTP 契约、错误码、指标、交互审计和参考协议均有单一常量来源；
- 所有外部契约与重构前逐字一致；
- `mvn -pl backend/claim-intake-service -am clean verify` 通过；
- 集成测试在真实 MySQL/Redis 上执行且无跳过；
- 新增架构测试通过；
- Spec 任务、证据和验证记录已回写；
- 提交并推送功能分支，等待人工评审。
