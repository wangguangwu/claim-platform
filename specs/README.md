# Spec 目录与选择器

本文件是功能 Spec 的唯一目录。开始功能任务时，先在此选择一个活动 Spec；未命中时才新建 Spec。归档 Spec 只用于追溯既有业务决策、迁移原因和测试证据，不进入日常开发上下文。

## 目录职责

| 位置 | 职责 | 日常是否读取 |
| --- | --- | --- |
| `requirements.md` | 项目稳定业务、架构与非功能约束 | 新功能、行为/边界变更或目录未能消除范围疑问时 |
| `active/<feature-name>/` | 正在实现、评审、发布或受阻的功能 Spec | 仅读取当前选定目录 |
| `archive/<feature-name>/` | 已完成功能的决策、迁移和验证证据 | 仅在历史追溯或兼容性分析时 |

## 状态约定

| 状态 | 含义 | 是否可以开始实现 |
| --- | --- | --- |
| `Draft` | 需求、设计或任务尚未完成 | 否 |
| `Ready` | Spec Review 已通过，等待实施 | 是 |
| `In Progress` | 正在实施，存在可执行 Task | 是，仅处理当前 Task |
| `Blocked` | 缺少外部契约、环境或业务决策 | 否，直到解除条件满足 |
| `In Review` | 实现完成，等待人工评审 | 否，除评审反馈修正 |
| `Ready for Release` | 本地验证完成，等待发布流程 | 否，除发布问题修正 |
| `Archived` | 已完成且不再日常维护 | 否，仅读取历史证据 |

## 活动 Spec

| Spec | 状态 | 适用任务 | 当前边界或解除条件 |
| --- | --- | --- | --- |
| `active/person-model/` | `In Progress` | Claim Intake 人员领域模型、DO 与转换 | 完成测试改造与 `mvn verify` |
| `active/claim-intake-standard-flow/` | `Blocked` | 签收命令、材料、提交幂等、业务锁与服务内事务 | Gateway 可信身份、租户、Trace、协议版本头透传与端到端验证 |
| `active/tenant-adapter-intake/` | `Blocked` | 租户报文解析、校验、转换、协议注册与入口 | 取得真实租户报文、字段/材料编码表和协议版本 |
| `active/sign-no-generation/` | `Ready for Release` | 签收号分配、迁移、Redis/MySQL 降级与校准 | 进入发布验证、告警与生产迁移流程 |
| `active/backend-constants-mybatis-plus/` | `In Review` | Claim Intake 常量治理与 MyBatis-Plus 查询重构 | 完成人工评审 |

## 归档 Spec

| Spec | 归档原因 |
| --- | --- |
| `archive/application-intake/` | Claim Intake 基础受理、材料登记和查询能力已完成 |
| `archive/platform-foundation/` | 本地基础设施、最小服务骨架与统一响应信封已完成 |
| `archive/claim-intake-ddd-package-refactor/` | 四层包结构迁移已完成 |
| `archive/claim-intake-idempotency-cache/` | 幂等查询定位缓存能力已完成 |

## 新建与归档规则

- 需要新增功能、关键缺陷修复或边界变更，但活动 Spec 均不匹配时，在 `active/<feature-name>/` 新建三份 Spec；完整规则见 `standards/spec-coding.md`。
- 不为尚未确认的 Gateway、Case Delivery 或真实租户协议预建空 Spec；获得明确需求或外部契约后再创建。
- 活动 Spec 的全部 Task 完成、验证证据齐全且不再等待评审或发布时，更新状态为 `Archived` 并迁入 `archive/`。
- 归档 Spec 被新的功能替代时，在新 Spec 中记录替代关系；不得删除归档证据。
