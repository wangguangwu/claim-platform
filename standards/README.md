# 工程规范索引

`standards/` 只保留"这类代码怎么写"的技术规则。任务入口和硬性检查在 `AGENTS.md`，稳定业务与架构约束在 `specs/requirements.md`。

## 目录

| 文件 | 解决的问题 | 读取时机 |
| --- | --- | --- |
| `java.md` | Java 生产代码与测试的组织、异常、日志与校验 | 修改 Java 代码或测试 |
| `ddd.md` | 限界上下文、聚合、依赖方向、跨上下文协作与数据边界 | 领域建模、事件、契约、数据边界改动 |
| `frontend.md` | Vue、TypeScript、状态和 API 调用的组织与校验 | 修改前端代码 |
| `error-handling.md` | 响应信封、错误码、HTTP 映射与异常日志 | 修改 HTTP 响应或异常处理 |
| `git.md` | 工作区、分支、提交、推送、审核与合并 | 执行 Git 操作 |

## 权威顺序

```text
specs/requirements.md
→ standards/*.md
→ README.md
```

专项规范只补充和细化根需求，不改变其业务范围与架构边界。

## 维护约束

- 新增规范文件前先确认现有文件无法承载该问题；本索引不重复专项规则内容。
- 修改专项规范时同步检查 `AGENTS.md` 的读取路由是否仍然一致。
