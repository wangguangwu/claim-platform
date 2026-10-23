# 同步

仅在功能分支尚未推送时 rebase：

```bash
git fetch origin
git rebase origin/main
```

- 已推送分支不自动 rebase；需要同步时由用户决定后续策略。
- 按代码、Spec 和测试预期解决冲突；无法判断时停止。
- 冲突解决后执行适用验证。
