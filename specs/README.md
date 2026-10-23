# Spec 目录

2026-10-23 起项目回归 MVP 流程：不再新建功能 Spec，也不再维护活动 Spec 状态表。日常开发依据是 `requirements.md`、当前对话需求和代码与测试本身。

| 位置 | 职责 | 日常是否读取 |
| --- | --- | --- |
| `requirements.md` | 项目稳定业务、架构与非功能约束，权威最高 | 改动涉及业务边界或跨上下文契约时 |
| `archive/<feature-name>/` | 已完成功能的需求、设计、任务与验证证据 | 仅在历史追溯或兼容性分析时 |

## 归档索引

| Spec | 归档原因 |
| --- | --- |
| `archive/application-intake/` | Claim Intake 基础受理、材料登记和查询能力已完成 |
| `archive/platform-foundation/` | 本地基础设施、最小服务骨架与统一响应信封已完成 |
| `archive/claim-intake-ddd-package-refactor/` | 四层包结构迁移已完成 |
| `archive/claim-intake-idempotency-cache/` | 幂等查询定位缓存能力已完成 |

## 规则

- 归档内容不修改、不删除；被后续实现替代时，在新代码或提交说明中记录替代关系。
- 需要书面方案时用一份不超过 20 行的简短说明，不建目录、不做编号回链，具体见 `AGENTS.md`。
