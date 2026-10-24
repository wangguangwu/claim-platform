# 运营管理台任务

- [ ] 建立 `claim-platform-frontend/` 的 Vue 3、TypeScript、Vite、Pinia、Vue Router 与 Axios 基础工程。
- [ ] 实现统一 API Client、认证与 `traceId` 处理、`ApiResult<T>` 解包、稳定错误映射和安全日志边界。
- [ ] 按六个业务功能域建立路由、页面和 API 模块，优先交付已稳定的 Sign 查询、材料、问题件与案件关系能力。
- [ ] 在 Push 查询 API 稳定后实现推送任务、推送记录与回执状态的查询页面。
- [ ] 在租户配置查询 API 稳定后实现只读租户配置查看，不在前端处理租户协议规则。
- [ ] 补齐路由、错误映射、局部失败、敏感数据边界与 API 契约测试，执行 `npm run typecheck` 和 `npm run build` 后勾选任务。
