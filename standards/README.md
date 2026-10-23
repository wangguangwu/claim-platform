# 工程规范索引

`standards/` 存放“如何实现与协作”的专项规范；稳定业务边界、架构约束和非功能要求以 `specs/requirements.md` 为准。`AGENTS.md` 是唯一的任务流程入口，负责决定何时读取本目录中的哪一份规范。

## 权威顺序

规则冲突时按以下顺序处理：

1. `specs/requirements.md`
2. 对应 `specs/active/<feature-name>/` 功能 Spec
3. 本目录中与当前任务匹配的专项规范
4. `README.md`

专项规范只能补充和细化高优先级文档，不能改变其业务范围或架构边界。

## 开发链路

```text
任务识别 → 分支与工作区检查 → Spec → Spec Review → Task 实施 → 验证 → Commit / 交付
```

- `AGENTS.md` 定义上下文选择与实施门禁。
- `specs/README.md` 决定选择哪个活动 Spec；归档 Spec 不参与日常读取。
- `spec-coding.md` 定义 Spec 内容、追踪关系、Review 和完成证据。
- 其余文件只在对应实现或交付动作发生时读取，不需要为了“完整阅读”全部加载。

## 标准目录

| 文件 | 解决的问题 | 读取时机 |
| --- | --- | --- |
| `git-workflow.md` | 分支、工作区、推送、审核与合并边界 | 代码开发前、Git 操作、推送或请求审核 |
| `spec-coding.md` | 功能 Spec 的创建、更新、追踪、Review 与验收 | 新增/修改 Spec、开始功能实现前、Spec 变更后 |
| `ddd.md` | 限界上下文、聚合、依赖方向、跨上下文协作与数据边界 | 领域建模、事件、契约、数据边界改动 |
| `java.md` | Java 生产代码与测试的组织、异常、日志与校验 | 修改 Java 代码或测试 |
| `frontend.md` | Vue、TypeScript、状态和 API 调用的组织与校验 | 修改前端代码 |
| `error-handling.md` | 响应信封、错误码、HTTP 映射与异常日志 | 修改 HTTP 响应或异常处理 |
| `commit.md` | 提交范围、信息格式和提交前检查 | 准备 Git Commit |

## 维护约束

- 修改 `AGENTS.md` 时，同步检查本索引与对应专项规范是否仍然一致。
- 修改专项规范时，若其适用时机、门禁或交付要求发生变化，必须同步更新 `AGENTS.md`。
- 不在本索引重复专项规则；新增专项规范前先确认现有规范无法清晰承载该问题。
