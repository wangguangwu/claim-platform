# Agent 执行规则

## 怎么干活

- 需求来自当前对话，验收靠编译与测试。不为功能新建 Spec 目录，不写需求/设计/任务三件套、追踪矩阵和逐任务证据回写。
- 稳定业务与架构约束以 `specs/requirements.md` 为准；与本文件冲突时以根需求为准，并把冲突说清楚，不猜业务规则。
- `specs/archive/` 只在追溯历史决策时读取，不参与日常开发上下文。
- 只读取命中当前改动的专项规范：`standards/java.md`、`standards/ddd.md`、`standards/frontend.md`、`standards/error-handling.md`、`standards/git.md`。

## 三条硬性检查

1. 动手前确认工作区不会覆盖或混入用户已有改动（`standards/git.md`）。
2. 交付前跑通校验：后端 `mvn verify`，前端 `npm run build`；未通过不交付。
3. 不合并 `main`、不 force push、不自行批准审核。

## 确实需要写文档时

- 用一份不超过 20 行的简短说明：目标、范围、不做什么、怎么验收。放在功能所在模块目录或提交说明里，不建目录、不做编号回链。
- 修改跨上下文 API、领域事件、数据库边界、租户模型或根约束时，先更新 `specs/requirements.md` 再动代码。
