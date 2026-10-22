# Git 提交规范

## 提交粒度

- 一个提交只表达一个可独立理解、审查和回滚的意图；不要混合功能、重构、格式化和无关文件。
- 先提交 Spec 与实现所需的规则变更，再提交对应实现；同一逻辑变更无法拆分时可放入同一提交，并在正文说明关联关系。
- 不提交令牌、凭证、个人信息、构建产物、IDE 临时文件或与当前任务无关的改动。

## 提交信息

标题使用 Conventional Commits 格式：

```text
<type>(<scope>): <简洁说明>
```

`scope` 可选，使用受影响的上下文或工程区域，例如 `application`、`case-delivery`、`operations`、`gateway`、`frontend`、`specs`、`standards` 或 `build`。

| type | 使用场景 |
| --- | --- |
| `feat` | 新增可见业务能力 |
| `fix` | 修复缺陷 |
| `refactor` | 不改变外部行为的重构 |
| `test` | 新增或调整测试 |
| `docs` | README、Spec 或工程规范变更 |
| `build` | Maven、Node、依赖或构建配置变更 |
| `ci` | 持续集成配置变更 |
| `chore` | 不属于以上类型的维护工作 |

示例：

```text
docs(standards): 建立渐进式工程规范路由

将执行规范拆分为 DDD、Spec Coding、语言、前端与提交规则，
并由 AGENTS.md 按任务类型加载。
```

破坏性变更必须在正文说明受影响对象、迁移方式和回滚方案，并在页脚增加 `BREAKING CHANGE: <说明>`。

## 提交前检查

1. 使用 `git status --short` 和 `git diff --check` 确认提交范围与空白错误。
2. 检查功能 Spec、迁移说明和测试证据是否已随实现更新。
3. 执行与改动范围匹配的校验：后端使用 `mvn verify`；前端使用 `npm run typecheck` 与 `npm run build`。
4. 校验提交标题的 type、scope 和说明是否准确；无法执行的校验必须在提交正文说明原因与风险。
