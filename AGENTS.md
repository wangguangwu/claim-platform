# Agent 执行规则

## 规则优先级

发生规则冲突时，按以下优先级执行：

```text
specs/requirements.md
→ 当前功能 Spec
→ standards/*.md
→ README.md
```

规则：

- 高优先级规则覆盖低优先级规则。
- 低优先级文件只能补充和细化，不得降低或绕过高优先级约束。
- 当前功能 Spec 包括：
    - `specs/<feature-name>/requirements.md`
    - `specs/<feature-name>/design.md`
    - `specs/<feature-name>/tasks.md`
- 规则冲突且无法按优先级消解时，不得自行补充业务规则，应明确指出冲突。

## 渐进式读取路由

开始任务前先判断任务类型，只读取当前任务需要的 Spec 和标准。

| 当前任务 | 必读文件 |
| --- | --- |
| 新增、修改、重构或修复功能 | `specs/requirements.md`、当前功能 Spec，以及当前改动命中的规范 |
| 调整限界上下文、跨上下文协作、聚合、领域事件、契约或数据边界 | `standards/ddd.md`、`standards/spec-coding.md` |
| 修改 Java 生产代码或测试 | `standards/java.md` |
| 修改 Java 领域模型、领域规则、Repository 或领域服务 | `standards/java.md`、`standards/ddd.md` |
| 修改 Vue 页面、组件、Composable、Store、API 模块或 TypeScript 业务逻辑 | `standards/frontend.md` |
| 修改 HTTP 响应、错误码、全局异常处理或异常日志 | `standards/error-handling.md`；Java 服务同时读取 `standards/java.md` |
| 新增或修改功能 Spec | `standards/spec-coding.md` |
| 开始代码开发、切换分支、创建分支、推送或请求审核 | `standards/git-workflow.md` |
| 准备 Git Commit | `standards/commit.md` |
| 修改根需求、工程标准或本文件 | `standards/README.md` 以及被修改的目标文件 |

读取原则：

- 未命中路由时不读取无关标准。
- `standards/README.md` 只负责标准索引，不替代具体标准。
- 不因为“可能相关”一次性读取全部 Spec 和标准。
- 当前上下文已经明确提供的内容无需重复读取。
- 同一任务同时命中多个规则时，组合读取对应文件。

## 实现前门禁

生产代码实现前必须确认：

- 已读取根需求和当前功能 Spec；
- 当前需求、设计和任务边界明确；
- 当前修改命中的工程标准已经读取；
- 当前代码位于正确的功能分支；
- 工作区已有修改已经识别，不会被覆盖或误提交。

以下改动必须先更新对应功能 Spec：

- 新增或修改 HTTP API；
- 新增或修改领域事件；
- 数据库 Schema 或迁移；
- 新增或修改配置项；
- Agent 工具变化；
- 跨上下文协作变化；
- 聚合、契约或数据所有权变化；
- 影响既有业务行为的关键缺陷修复。

纯代码整理且不改变业务行为时，可以使用现有 Spec，不要求创建新的功能 Spec。

## 实现规则

- 新功能目录统一使用小写 kebab-case。
- 实现严格限定在当前任务和 Spec 范围内。
- 不顺带修改无关业务、架构或工程配置。
- 不为了代码