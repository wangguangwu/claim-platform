# Git 规则

## 工作区

```bash
git status --short
git branch --show-current
```

- 存在未提交改动时，先判断是否属于当前任务；不覆盖、不丢弃、不顺手提交用户的改动。
- 无关改动可能冲突时停止操作，向用户说明。

## 分支

- 功能改动在短生命周期分支上完成：`feat/<name>`、`fix/<name>`、`refactor/<name>`、`docs/<name>`；小改动可以直接在 `main` 上提交。
- 已有同名分支时复用并 `git pull --ff-only`；否则从最新 `main` 创建。

## 同步

- 仅在分支尚未推送时 `git rebase origin/main`；已推送分支不自动 rebase，需要同步时由用户决定策略。
- 冲突按代码、测试、文档的预期语义解决，判断不了就停；解决后重新执行适用校验。

## 提交

- 一个完整、可独立理解和回滚的改动对应一个 Commit，不混入无关文件、凭证、构建产物或 IDE 临时文件。
- 明确 `git add <files>`，禁止默认 `git add .`。
- 标题用 `<type>(<scope>): <简洁说明>`，`type` 取 `feat | fix | refactor | test | docs | build | ci | chore`；破坏性变更在正文写明迁移与回滚并加 `BREAKING CHANGE:`。

```bash
git status --short
git diff --check
```

## 推送、审核与合并

- 首次 `git push -u origin <branch>`，后续 `git push origin <branch>`；禁止 `git push --force` 和 `git push origin main`。
- 提交审核时说明分支、改动范围、验证结果和风险点；不得自行批准。
- `main` 的合并由人工审核通过后执行，Agent 不代做合并决定。
