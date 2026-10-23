# 提交

- 一个完成且已验证的 Task 对应一个 Commit；拆分时先在 `tasks.md` 定义可独立验收的子任务。
- 不混入无关改动或凭证、个人信息、构建产物和 IDE 临时文件。
- 暂存明确文件，禁止默认 `git add .`。

标题：

```text
<type>(<scope>): <简洁说明>
```

```text
feat | fix | refactor | test | docs | build | ci | chore
```

- `scope` 可选；破坏性变更在正文说明迁移与回滚，并加 `BREAKING CHANGE:`。

提交前：

```bash
git status --short
git diff --check
git add <files>
git commit -m "<commit-message>"
```
