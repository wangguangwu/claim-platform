# 分支

已有功能分支时复用：

```bash
git switch <feature-branch>
git pull --ff-only origin <feature-branch>
```

否则从最新 `main` 创建：

```bash
git switch main
git pull --ff-only origin main
git switch -c <feature-branch>
```

```text
feat/<feature-name>
fix/<feature-name>
refactor/<feature-name>
docs/<feature-name>
```
