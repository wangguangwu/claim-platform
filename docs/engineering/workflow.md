# 开发流程

本文件负责任务从开始到结束的完整生命周期，是所有 Git 操作规则的唯一归属。业务与架构约束见 `docs/specs/baseline.md`，Spec 写法见 `docs/engineering/specification.md`，代码写法见 `docs/engineering/implementation.md`。

## 1. 开始任务前

1. 确认需求来自当前对话还是 `docs/specs/<feature-name>/`，并明确验收口径（编译、测试、页面行为）。
2. 判定改动粒度：Feature 级改动需要先建立 Spec（判定标准见 `docs/engineering/specification.md`）；轻量改动不建 Spec，直接进入流程，但仍受基线约束。
3. 读取 `docs/specs/baseline.md` 中与任务相关的章节：产品范围、核心规则、架构约束、全局需求。
4. 需求含义、业务规则或数据归属不确定时先向用户确认，不猜。

## 2. 工作区检查

```bash
git status --short
git branch --show-current
```

- 存在未提交改动时，先判断是否属于当前任务；不覆盖、不丢弃、不顺手提交用户的改动。
- 无关改动可能与本次操作冲突时停止，向用户说明。

## 3. 分支

- 功能改动在短生命周期分支上完成：`feat/<name>`、`fix/<name>`、`refactor/<name>`、`docs/<name>`；小改动可以直接在 `main` 上提交。
- 已有同名分支时复用并 `git pull --ff-only`；否则从最新 `main` 创建。

## 4. Spec 准备

Feature 级改动在编码前完成 `docs/specs/<feature-name>/` 的 `requirements.md` → `design.md` → `tasks.md`，顺序与内容边界按 `docs/engineering/specification.md`。轻量改动跳过本步。

## 5. 开发过程

- 按 `tasks.md` 的顺序实施，一次只推进一个任务；无 Spec 时按当前对话拆出的步骤推进。
- 编码规则按 `docs/engineering/implementation.md`。
- 发现设计需要调整时，先按 `docs/engineering/specification.md` 的一致性规则改文档，再继续写代码；不得用未记录的实现决策绕过 Spec。

## 6. 验证

| 改动范围 | 必须执行 |
| --- | --- |
| 后端 | `mvn verify` |
| 前端 | `npm run typecheck` 与 `npm run build` |
| 两侧都有 | 两侧各自执行 |

- 只改动单侧时只跑对应侧，不必全量验证。
- 前端门禁在 `claim-platform-frontend/` 建立后生效；该项目尚不存在时前端侧不适用，不算验证缺口，也不需要为它补跑命令。
- 验证未通过不交付；无法执行时说明原因、影响范围和风险。
- 解决冲突或改动跨层后，重新执行受影响的验证。

## 7. 提交

```bash
git add <files>
git status --short
git diff --check
```

- 一个完整、可独立理解和回滚的改动对应一个 Commit，不混入无关文件、凭证、构建产物或 IDE 临时文件。
- 明确 `git add <files>`，禁止默认 `git add .`。
- 标题用 `<type>(<scope>): <简洁说明>`，`type` 取 `feat | fix | refactor | test | docs | build | ci | chore`。
- 破坏性变更在正文写明迁移与回滚，并加 `BREAKING CHANGE:`。

## 8. 同步、推送、审核与合并

- 仅在分支尚未推送时 `git rebase origin/main`；已推送分支不自动 rebase，需要同步时由用户决定策略。
- 冲突按代码、测试、文档的预期语义解决，判断不了就停。
- 首次 `git push -u origin <branch>`，后续 `git push origin <branch>`；禁止 `git push --force`，禁止 `git push origin main`。
- 提交审核时说明分支、改动范围、验证结果和风险点；不得自行批准。
- `main` 的合并由人工审核通过后执行，Agent 不代做合并决定。

## 9. 任务结束条件

全部满足才算结束：

1. 代码覆盖任务范围，第 6 节的验证已执行且通过（或已说明无法执行的原因与风险）。
2. Feature 级任务的 `tasks.md` 勾选状态与实际代码、测试一致。
3. 已交付的 Feature Spec 按 `docs/engineering/specification.md` 第 7 节完成基线回写并删除目录；未交付的保留为活动目录。
4. 未绕过基线约束；本次改动触发基线变更条件时（见 `docs/specs/baseline.md`「与功能 Spec 的关系」），基线已先行更新并经用户确认。
5. 工作区只剩本任务的改动且已提交，未混入用户已有改动。
6. 未合并 `main`、未 force push、未自行批准审核。
