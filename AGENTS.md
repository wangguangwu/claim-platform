# Agent 开发入口

本文件只做入口和导航，不写具体规则。每条规则只有一个归属文件，需要细节时直接读归属文件；发现同一规则出现在两处，以归属文件为准并报告冲突。

## 协作权威与强制入口

- 所有人类与 AI Agent 在读取、分析或修改仓库前都必须先读取本文件；任何后续规则都通过本文件定位，不能以未阅读为由绕过。
- 所有写入操作都必须完成下方执行顺序中与任务匹配的前置阅读；需求不明确时停止写入，按归属规则向用户确认。
- 本文件是协作入口的唯一权威，不复制业务、流程或编码细则；具体规则仍以本文件链接的归属文档为准。
- 交付时必须能说明使用了哪些归属文档、完成了哪些验证。未满足归属规则的改动不是可交付结果。

## 执行顺序

接到任务后按这个顺序走：

1. 读 `docs/specs/baseline.md` 中与任务相关的章节，确认范围、约束和所属限界上下文。
2. 按 `docs/engineering/workflow.md` 执行任务生命周期（工作区检查 → 分支 → Spec 准备 → 开发 → 验证 → 提交 → 结束条件）。
3. 判定为 Feature 级改动时，按 `docs/engineering/specification.md` 建立并维护 `docs/specs/<feature-name>/`。
4. 动手改代码前读 `docs/engineering/implementation.md`，按语言与契约细则执行。

## 规则归属

| 文件 | 负责的问题 |
| --- | --- |
| `docs/specs/baseline.md` | 产品范围、核心业务规则、技术约束、架构与分层、全局需求、范围之外；已吸收签收域架构蓝本 |
| `docs/engineering/workflow.md` | 开发生命周期与全部 Git 操作：工作区、分支、同步、提交、推送、审核、合并、验证门禁、任务结束条件 |
| `docs/engineering/specification.md` | Spec 的建与不建、`requirements.md` / `design.md` / `tasks.md` 的职责边界与一致性规则、交付后清理 |
| `docs/engineering/implementation.md` | 代码修改规范：先查后写、复用、新类职责与位置、范围纪律、测试、Java / 前端 / HTTP 契约细则 |
| `README.md` | 项目导航与技术基线速览，不作为规则或需求依据 |

## 冲突与不确定

- 权威顺序与基线变更规则见 `docs/specs/baseline.md`「与功能 Spec 的关系」。
- 业务规则、数据归属或需求含义不明确时，按 `docs/engineering/workflow.md` 第 1 节先向用户确认，不自行扩展流程。
