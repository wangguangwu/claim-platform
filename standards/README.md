# 工程规范索引

`standards/` 存放项目的执行规范，解决“如何实现与协作”；业务范围、架构边界和非功能约束仍以 `specs/requirements.md` 为准。

## 权威顺序

当规则冲突时，按以下顺序处理：

1. `specs/requirements.md`
2. 对应 `specs/<feature-name>/` 功能 Spec
3. 本目录中与当前任务匹配的标准
4. `README.md`

## 标准目录

| 文件 | 解决的问题 | 读取时机 |
| --- | --- | --- |
| `ddd.md` | 如何落实限界上下文、聚合、依赖方向与跨上下文协作 | 领域建模、事件、契约、数据边界改动 |
| `spec-coding.md` | 如何编写、追踪和验收功能 Spec | 新增或修改功能 Spec、开始功能实现前 |
| `java.md` | 如何组织和验证 Java 生产代码与测试 | 修改 Java 代码或测试 |
| `frontend.md` | 如何组织 Vue、TypeScript、状态与 API 调用 | 修改前端代码 |
| `commit.md` | 如何检查改动并编写提交信息 | 准备 Git 提交 |

`AGENTS.md` 是唯一的任务路由入口。执行任务时只读取它指定的标准；不要为了“完整阅读”加载无关文件。
