# claim-platform

理赔平台（Claim Platform）负责理赔链路的两段交互：

- **前段**：理赔申请签收（`Sign`）、材料（`Material`）、承保查询、审核同步、问题件（`ClaimIssue`）、撤销等与租户及内部系统的交互。
- **后段**：案件生成后，案件推送（`PushTask` / `PushRecord` / `PushReceipt`）等与保司的交互。

平台不承担案件审核、理算与核赔，也不处理外部保司系统的内部业务。

## 工程结构

```text
claim-platform/
├── claim-platform-backend/       # 后端 Maven 聚合工程
│   ├── claim-gateway/            # 租户接入边缘层：认证、验签、加解密、协议适配、路由、限流、Trace
│   ├── claim-sign-service/       # 理赔签收限界上下文
│   ├── claim-case-push-service/  # 案件推送限界上下文
│   └── claim-common/             # 无领域规则的共享技术能力与稳定契约（不计入服务结构）
├── docs/
│   ├── specs/                    # 项目基线与活动功能 Spec
│   └── engineering/              # 流程、Spec 与代码规范
├── AGENTS.md                     # Agent 开发入口与规则导航
└── README.md                     # 本文件
```

## 服务职责

| 模块 | 职责 | 默认端口 |
| --- | --- | --- |
| `claim-gateway` | 报文协议、认证、验签、加解密、限流、路由、Trace | 8080 |
| `claim-sign-service` | 签收提交与查询、材料处理、承保查询、审核同步、问题件、撤销 | 8081 |
| `claim-case-push-service` | 推送任务、推送记录、回执、推送策略 | 8082 |
| `claim-common` | 响应信封 `ApiResult<T>` 等稳定技术契约 | — |

## 技术基线

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Alibaba、MySQL 8、Redis 7、RocketMQ、XXL-Job。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios。
- 可观测：OpenTelemetry、Prometheus、Grafana、Loki。
- 容器：Docker、Docker Compose。

约束的完整表述见 [docs/specs/baseline.md](docs/specs/baseline.md) §3。

## 文档入口

- [AGENTS.md](AGENTS.md)：所有协作 Agent 的强制任务入口与规则导航。
- [项目基线](docs/specs/baseline.md)：唯一的长期架构与公共约束来源。
- `docs/specs/<feature-name>/`：活动功能的需求、设计与任务。
- `docs/engineering/`：开发流程、Spec 规范与代码规范。

本 README 只提供项目导航，不作为需求或架构规则依据。

## 构建与验证

```bash
mvn verify
```

后端改动必须通过 `mvn verify`；前端改动（`claim-platform-frontend/` 建立后）必须通过 `npm run typecheck` 与 `npm run build`。门禁细则见 [docs/engineering/workflow.md](docs/engineering/workflow.md) 第 6 节。
