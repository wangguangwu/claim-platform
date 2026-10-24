# claim-platform

理赔平台（Claim Platform）负责理赔链路的两段交互：

- **前段**：理赔申请签收（`Sign`）、材料（`Material`）、承保查询、审核同步、问题件（`ClaimIssue`）、撤销等与租户及内部系统的交互。
- **后段**：案件生成后，案件推送（`PushTask` / `PushRecord` / `PushReceipt`）等与保司的交互。

平台不承担案件审核、理算与核赔，也不处理外部保司系统的内部业务。

## 仓库内容

本仓库只保存文档与架构基线；服务代码由功能 Spec 交付时重建，历史实现保留在 Git 记录中。

```text
claim-platform/
├── docs/
│   ├── architecture/claim-sign-service.md  # 签收服务域内架构基准
│   ├── specs/baseline.md         # 平台级长期公共约束来源
│   └── engineering/              # 流程、Spec 与代码规范
├── AGENTS.md                     # Agent 开发入口与规则导航
└── README.md                     # 本文件
```

目标工程结构、服务职责与数据表归属见 [docs/specs/baseline.md](docs/specs/baseline.md) §4、§5，本文件不重复；签收服务的分层、状态机、数据模型与核心时序见 [docs/architecture/claim-sign-service.md](docs/architecture/claim-sign-service.md)。四个后端模块及其默认端口速览：`claim-gateway`（8080）、`claim-sign-service`（8081）、`claim-case-push-service`（8082）、共享技术模块 `claim-common`。

## 技术基线

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Alibaba、MySQL 8、Redis 7、RocketMQ、XXL-Job。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios。
- 可观测：OpenTelemetry、Prometheus、Grafana、Loki。
- 容器：Docker、Docker Compose。

约束的完整表述见 [docs/specs/baseline.md](docs/specs/baseline.md) §3。

## 文档入口

- [AGENTS.md](AGENTS.md)：所有协作 Agent 的强制任务入口与规则导航。
- [项目基线](docs/specs/baseline.md)：平台级长期公共约束来源。
- [签收服务架构基准](docs/architecture/claim-sign-service.md)：签收域内分层、状态机、数据模型、时序与补偿。
- `docs/specs/<feature-name>/`：活动功能的需求、设计与任务。
- `docs/engineering/`：开发流程、Spec 规范与代码规范。

本 README 只提供项目导航，不作为需求或架构规则依据。

## 验证

后端改动通过 `mvn verify`，前端改动（`claim-platform-frontend/` 建立后）通过 `npm run typecheck` 与 `npm run build`；门禁细则见 [docs/engineering/workflow.md](docs/engineering/workflow.md) 第 6 节。仓库不含代码时这两条不适用。
