# 理赔申请幂等查询缓存任务

## 任务清单

| 任务 | 需求 | 设计 | 状态 | 证据 |
| --- | --- | --- | --- | --- |
| TK-CIIC-001 建立变更 Spec | FR-CIIC-001 至 FR-CIIC-006 | DS-CIIC-001 至 DS-CIIC-003 | Done | 本目录三份 Spec |
| TK-CIIC-002 新增缓存端口与 Redis 适配器 | FR-CIIC-003、FR-CIIC-005 | DS-CIIC-001 | Todo | 待实现与单测 |
| TK-CIIC-003 改造查询与创建回填链路 | FR-CIIC-001、FR-CIIC-002、FR-CIIC-004 | DS-CIIC-002、DS-CIIC-003 | Todo | 待实现与单测 |
| TK-CIIC-004 新增配置和校验 | FR-CIIC-006 | 核心数据模型 | Todo | 待实现与绑定测试 |
| TK-CIIC-005 完成回归验证与 Spec 回写 | FR-CIIC-001 至 FR-CIIC-006 | 测试策略 | Todo | 待执行 Maven 测试 |

## 追踪矩阵

| 需求 | 设计 | 任务 | 测试 |
| --- | --- | --- | --- |
| FR-CIIC-001 | DS-CIIC-001、DS-CIIC-002 | TK-CIIC-002、TK-CIIC-003 | TEST-CIIC-001 |
| FR-CIIC-002 | DS-CIIC-002 | TK-CIIC-003 | TEST-CIIC-002、TEST-CIIC-003 |
| FR-CIIC-003 | DS-CIIC-002 | TK-CIIC-002、TK-CIIC-003 | TEST-CIIC-005 |
| FR-CIIC-004 | DS-CIIC-003 | TK-CIIC-003 | TEST-CIIC-004 |
| FR-CIIC-005 | 核心数据模型 | TK-CIIC-002 | TEST-CIIC-006 |
| FR-CIIC-006 | 流量治理 | TK-CIIC-002、TK-CIIC-004 | TEST-CIIC-005、TEST-CIIC-006 |

## 已知限制

- 该分支当前只完成 Spec，未实现生产代码、配置或测试。
- Redis 缓存仅优化幂等键定位；即使命中缓存仍需由 MySQL 读取当前状态。
- 缓存 TTL、默认前缀和指标名是待实现的默认设计，生产落地前需通过配置绑定与集成测试确认。
