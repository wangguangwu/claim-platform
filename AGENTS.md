# Agent 执行规则

## 规则权威顺序

规则冲突时，按以下顺序处理：`specs/requirements.md` → 对应功能 Spec → 适用的 `standards/*.md` → `README.md`。低优先级文件只能细化高优先级文件，不能降低或绕过其约束。

## 渐进式读取路由

| 当前任务 | 必读文件 |
| --- | --- |
| 新增、修改、重构或修复功能 | `specs/requirements.md`、对应 `specs/<feature-name>/requirements.md`（或 `change.md`）、`design.md`、`tasks.md`，以及下表命中的规范 |
| 调整限界上下文、跨上下文协作、聚合、事件、契约或数据边界 | `standards/ddd.md`、`standards/spec-coding.md` |
| 修改 Java 生产代码或测试 | `standards/java.md`；涉及领域逻辑时同时读取 `standards/ddd.md` |
| 修改 Vue 页面、组件、Composable、Store、API 模块或 TypeScript 业务函数 | `standards/frontend.md` |
| 定义或修改 HTTP 响应、错误码、全局异常处理或异常日志 | `standards/error-handling.md`；Java 服务同时读取 `standards/java.md` |
| 新增或修改功能 Spec | `standards/spec-coding.md` |
| 修改根需求、工程标准或本文件 | `standards/README.md` 及被修改的目标标准；修改根需求时同时进行项目级评审 |
| 准备 Git 提交 | `standards/commit.md` |

未命中路由时，不读取无关标准。`standards/README.md` 只负责索引，不替代具体标准。

## 实现与提交门禁

- 新功能目录使用小写 kebab-case；生产代码实现前必须具备需求、设计和任务文档。
- 新增接口、领域事件、数据库迁移、配置项、Agent 工具、跨上下文改动或关键缺陷修复，必须先更新对应功能 Spec。
- 实现完成后，必须回写任务状态、测试证据、迁移说明和已知限制；追踪链路未闭合时不得标记为 Done。
- 提交前必须按 `standards/commit.md` 完成范围检查、适用校验与提交信息检查。
