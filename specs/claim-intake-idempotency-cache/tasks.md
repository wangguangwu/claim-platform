# 理赔申请幂等查询缓存任务

## 任务清单

| 任务 | 需求 | 设计 | 状态 | 证据 |
| --- | --- | --- | --- | --- |
| TK-CIIC-001 建立变更 Spec | FR-CIIC-001 至 FR-CIIC-006 | DS-CIIC-001 至 DS-CIIC-003 | Done | 本目录三份 Spec |
| TK-CIIC-002 新增缓存端口与 Redis 适配器 | FR-CIIC-003、FR-CIIC-005 | DS-CIIC-001 | Done | `IntakeSubmissionReplayCache`、`RedisIntakeSubmissionReplayCache` 与 `RedisIntakeSubmissionReplayCacheTest` |
| TK-CIIC-003 改造查询与创建回填链路 | FR-CIIC-001、FR-CIIC-002、FR-CIIC-004 | DS-CIIC-002、DS-CIIC-003 | Done | `ClaimIntakeCommandServiceTest` 覆盖命中、回源、失效删除和提交后回填 |
| TK-CIIC-004 新增配置和校验 | FR-CIIC-006 | 核心数据模型 | Done | `IntakeSubmissionReplayCacheProperties`、`application.yml` 与配置校验测试 |
| TK-CIIC-005 完成回归验证与 Spec 回写 | FR-CIIC-001 至 FR-CIIC-006 | 测试策略 | Done | `mvn -pl claim-intake-service -am test`、`mvn verify` 均通过 |

## 追踪矩阵

| 需求 | 设计 | 任务 | 测试 |
| --- | --- | --- | --- |
| FR-CIIC-001 | DS-CIIC-001、DS-CIIC-002 | TK-CIIC-002、TK-CIIC-003 | TEST-CIIC-001 |
| FR-CIIC-002 | DS-CIIC-002 | TK-CIIC-003 | TEST-CIIC-002、TEST-CIIC-003 |
| FR-CIIC-003 | DS-CIIC-002 | TK-CIIC-002、TK-CIIC-003 | TEST-CIIC-005 |
| FR-CIIC-004 | DS-CIIC-003 | TK-CIIC-003 | TEST-CIIC-004 |
| FR-CIIC-005 | 核心数据模型 | TK-CIIC-002 | TEST-CIIC-006 |
| FR-CIIC-006 | 流量治理 | TK-CIIC-002、TK-CIIC-004 | TEST-CIIC-005、TEST-CIIC-006 |

## 实现与验证记录

### 2026-10-23

- 新增应用端口 `IntakeSubmissionReplayCache` 和 Redis 适配器 `RedisIntakeSubmissionReplayCache`；缓存键采用 `SHA-256(tenantCode + "\\n" + externalApplyNo)`，默认前缀为 `claim:intake:replay:`，默认 TTL 为 24 小时。
- `ClaimIntakeCommandService.findByExternalApplyNo` 已按 Cache-Aside 执行：缓存命中后按 `tenantCode + signNo` 从 MySQL 读取当前聚合；缓存未命中、失效或不可用时按外部申请号回源，并最佳努力回填。
- `ClaimIntakeCreationService.create` 成功返回后才写入缓存；缓存写入、读取和删除异常均由适配器吸收，不参与数据库事务、业务锁或 MySQL 唯一键幂等判断。
- 新增配置 `claim.intake.submission.replay-cache.key-prefix` 和 `ttl`，可通过 `CLAIM_INTAKE_REPLAY_CACHE_KEY_PREFIX`、`CLAIM_INTAKE_REPLAY_CACHE_TTL` 覆盖；非法空前缀和非正 TTL 在启动校验时失败。
- `mvn -pl claim-intake-service -am test` 通过：`claim-common` 2 项、`claim-intake-service` 119 项测试成功。
- `mvn verify` 通过：后端全部模块构建成功；编译中存在既有 Lombok `equals/hashCode` 警告，和本次改动无关。

## 已知限制

- Redis 缓存仅优化幂等键定位；即使命中缓存仍需由 MySQL 读取当前状态。
- 缓存适配器具备单元测试；真实 Redis 的端到端故障切换测试可在具备稳定 Redis 环境后作为运行验证补充。
