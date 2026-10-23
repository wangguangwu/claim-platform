# Agent 执行规则

## 文档职责

- `AGENTS.md`：决定当前任务读什么，以及何时可以进入下一阶段。
- `specs/README.md`：定位当前活动 Spec；归档 Spec 仅在追溯历史决策时读取。
- 当前活动 Spec：定义当前任务做什么、为什么做和如何验收。
- `standards/*.md`：定义所选实现动作如何执行。

除本文件和当前任务命中的文档外，不为了“完整阅读”加载其他 Spec 或标准。

## 规则优先级

```text
specs/requirements.md
→ 当前活动 Spec
→ standards/*.md
→ README.md
```

- 高优先级规则覆盖低优先级规则；低优先级文件只能补充和细化。
- 冲突无法按优先级消解时，说明冲突与影响，不得猜测业务规则。

## 上下文选择

| 当前动作 | 必读文档 | 进入下一步前确认 |
| --- | --- | --- |
| 不确定应修改哪个功能 | `specs/README.md` | 已选定一个活动 Spec，或已确认需要新建 Spec |
| 新功能、行为变化、跨上下文/API/事件/Schema/配置/数据所有权变更 | `specs/README.md`、`specs/requirements.md`、当前活动 Spec、`standards/spec-coding.md` | 范围、边界和验收标准明确 |
| 实施已有 Task | 当前活动 Spec 的 `tasks.md` 与关联需求、设计章节 | 当前 Task、依赖和完成标准明确 |
| Java 代码或测试 | `standards/java.md` | 适用校验已确定 |
| 领域模型、应用服务、Repository、事件或跨上下文协作 | `standards/ddd.md`、`standards/java.md` | 分层、所有权和兼容性明确 |
| Vue、TypeScript、Store、Composable 或前端 API | `standards/frontend.md` | 前端边界与校验明确 |
| HTTP 响应、错误码、全局异常或异常日志 | `standards/error-handling.md`；Java 服务同时读 `standards/java.md` | 错误契约和安全日志明确 |
| 分支、工作区、推送或审核 | `standards/git-workflow.md` | 不会覆盖或混入已有改动 |
| Git Commit | `standards/commit.md` | 当前提交只对应一个完成的 Task |
| 修改本文件、根需求或专项规范 | `standards/README.md` 和被修改目标 | 入口、目录和专项规则一致 |

## 实施门禁

开始生产代码前必须满足：

- 已选定当前活动 Spec；纯重构必须确认不改变业务行为。
- 当前 Task 及其关联需求、设计和依赖已明确。
- 当前修改命中的专项规范已经读取。
- 已按 `git-workflow.md` 确认分支和工作区，不覆盖用户已有改动。
- 新功能或影响业务行为的变更已完成 Spec Review；未建立活动 Spec 时不得开始生产代码。

活动 Spec 为 `Draft`、`Blocked` 或缺少必要前置决策时，不得以实现代码绕过门禁。

## Task 闭环

1. 按 `tasks.md` 处理一个当前 Task，不顺带实现其他 Task。
2. 需求变化先更新 `requirements.md`；设计变化先更新 `design.md`；受影响时同步更新任务并重新 Review。
3. 实现后执行适用编译、测试或构建校验，回写 Task 的状态和验证证据。
4. 校验通过后，提交该 Task 的全部相关修改，再开始下一 Task；一个 Commit 不得混入多个 Task。
5. 无法完成时标记 `Blocked`，记录原因、影响、已尝试措施和解除条件。

## 交付约束

- 新功能目录创建于 `specs/active/<feature-name>/`，名称使用小写 kebab-case，并包含 `requirements.md`、`design.md`、`tasks.md`。
- 完成且不再需要日常维护的 Spec 迁入 `specs/archive/`；归档不删除决策、任务和测试证据。
- 每个已完成 Task 的 Commit 必须独立可理解、审查和回滚；不得自动合并到 `main`。
