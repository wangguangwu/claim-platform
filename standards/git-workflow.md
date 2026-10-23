# Git 工作流规范

## 适用范围

适用于代码开发、缺陷修复、重构、Spec 变更、分支操作、远程推送、审核和合并流程。

提交粒度、提交信息和提交前检查遵循 `standards/commit.md`。

## 分支原则

- `main` 为稳定主分支，不直接在 `main` 上进行功能开发。
- 功能开发、缺陷修复和重构统一在独立分支完成。
- 开始任务前先同步远程仓库状态。
- 已存在对应功能分支时继续使用，不重复创建。
- 不使用 `git push --force` 覆盖共享分支。
- Agent 不得自行将功能分支合并到 `main`。

## 开始任务

开始修改代码前必须检查：

```bash
git status --short
git branch --show-current
git fetch origin
```

工作区存在未提交改动时：

- 先判断是否属于当前任务；
- 不覆盖、不丢弃用户已有修改；
- 与当前任务无关且可能产生冲突时，应停止相关 Git 操作并明确说明。

### 已存在功能分支

```bash
git switch <feature-branch>
git pull --ff-only origin <feature-branch>
```

### 创建新功能分支

先同步 `main`：

```bash
git switch main
git pull --ff-only origin main
git switch -c <feature-branch>
```

分支命名遵循项目约定。

推荐：

```text
feat/<feature-name>
fix/<feature-name>
refactor/<feature-name>
docs/<feature-name>
```

例如：

```text
feat/claim-intake
fix/material-upload
refactor/ddd-package
docs/standards
```

## 开发过程

实现期间：

- 所有修改限定在当前任务和 Spec 范围内；
- 不顺带修改无关业务；
- 不覆盖用户已有未提交修改；
- 需要调整 Spec 时先完成对应文档更新；
- 一个 Task 完成并通过适用编译、测试或构建校验后，必须先提交其全部相关修改，再开始下一 Task；不得把多个 Task 累积到同一 Commit。
- 单个 Task 必须拆分为多个独立 Commit 时，应先在 `tasks.md` 明确子任务边界；不得交错实施其他 Task。

需要从 `main` 获取最新代码时，优先：

```bash
git fetch origin
git rebase origin/main
```

存在冲突时必须基于实际代码和 Spec 解决，不得直接使用一侧内容覆盖冲突。

## 完成验证

代码完成后必须先验证，再提交。

Java 后端默认执行：

```bash
mvn verify
```

前端默认执行：

```bash
npm run typecheck
npm run build
```

同时检查：

```bash
git status --short
git diff --check
```

验证失败时：

- 优先修复本次修改导致的问题；
- 不通过跳过测试、删除测试或绕过校验强行提交；
- 无法解决时明确记录失败原因、影响范围和风险。

## 提交代码

提交前读取 `standards/commit.md`。

确认：

- 提交范围只包含当前任务；
- Spec、代码和测试保持一致；
- 不包含构建产物、IDE 临时文件、凭证或敏感信息；
- 编译和适用测试已经通过。

然后执行：

```bash
git add <files>
git commit -m "<commit-message>"
```

不要默认使用：

```bash
git add .
```

应明确检查并选择当前任务需要提交的文件。

## 推送功能分支

提交完成后，将当前功能分支推送到远程：

首次推送：

```bash
git push -u origin <feature-branch>
```

后续推送：

```bash
git push origin <feature-branch>
```

禁止：

```bash
git push --force
git push origin main
```

## 请求审核

功能分支成功推送后，Agent 的自动执行到此结束。

必须向用户提供：

- 功能分支名称；
- Commit ID；
- 本次改动内容；
- 编译结果；
- 测试结果；
- Spec、数据库、配置或接口变化；
- 已知限制和风险；
- 建议重点审核的位置。

例如：

```text
分支：feat/claim-intake
Commit：abc1234

已完成：
- 调整 DDD 四层目录
- 更新相关 package/import
- 补充 XXL-Job 入口结构

验证：
- mvn verify 通过
- 单元测试 18/18 通过

请审核当前功能分支，确认后再合并 main。
```

如果具备 Pull Request / Merge Request 能力，可以创建或更新审核请求。

创建审核请求后仍然不得：

- 自行批准；
- 自行合并；
- 绕过人工审核。

## Main 合并规则

正常流程：

```text
同步仓库
→ 创建或拉取功能分支
→ 实现
→ 编译和测试
→ Commit
→ Push 功能分支
→ 请求人工审核
→ 用户确认
