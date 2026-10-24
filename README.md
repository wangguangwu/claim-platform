# claim-platform

理赔平台（Claim Platform）负责理赔链路的两段交互：

- **前段**：理赔申请签收（`Sign`）、材料（`Material`）、承保查询、审核同步、问题件（`ClaimIssue`）、撤销等与租户及内部系统的交互。
- **后段**：案件生成后，案件推送（`PushTask` / `PushRecord` / `PushReceipt`）等与保司的交互。

平台不承担案件审核、理算与核赔，也不处理外部保司系统的内部业务。

## 项目结构

仓库当前只有可构建的后端骨架与文档；前端工程、建表脚本与本地容器环境尚未重建。目标架构以 [项目基线](docs/specs/baseline.md) 为准，具体能力由对应 Feature Spec 交付。

```text
claim-platform/
├── claim-platform-backend/       # 后端 Maven 聚合工程（当前仅骨架）
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

## 服务与当前状态

| 模块 | 职责 | 默认端口 | 当前状态 |
| --- | --- | --- | --- |
| `claim-gateway` | 报文协议、认证、验签、加解密、限流、路由、Trace | 8080 | 仅启动类，无任何网关能力 |
| `claim-sign-service` | 签收提交与查询、材料处理、承保查询、审核同步、问题件、撤销 | 8081 | 仅启动类与配置文件 |
| `claim-case-push-service` | 推送任务、推送记录、回执、推送策略 | 8082 | 仅启动类 |
| `claim-common` | 响应信封 `ApiResult<T>` 等稳定技术契约 | — | 仅 `ApiResult` 与其单元测试，无领域规则 |

三个服务都只有启动类，可编译、可打包；`/actuator/health` 是唯一的运行时端点。可启动不代表业务能力存在。前端与本地 MySQL/Redis、RocketMQ、OSS 环境均不在仓库内，需要各自的 Spec 交付。

## 技术基线

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Alibaba、MySQL 8、Redis 7、RocketMQ、XXL-Job。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios（工程尚未建立）。
- 可观测：OpenTelemetry、Prometheus、Grafana、Loki。
- 容器：Docker、Docker Compose（编排与建表脚本已随实现下线）。

约束的完整表述见 [docs/specs/baseline.md](docs/specs/baseline.md) §3。

## 文档入口

- [AGENTS.md](AGENTS.md)：任务入口与规则导航。
- [项目基线](docs/specs/baseline.md)：唯一的长期架构与公共约束来源。
- `docs/specs/<feature-name>/`：活动功能的需求、设计与任务。
- `docs/engineering/`：开发流程、Spec 规范与代码规范。

本 README 只提供导航和当前状态，不作为需求或架构规则依据。历史过程通过 `git log` 追溯；已交付功能的长期结论回写项目基线。

## 构建与验证

```bash
mvn verify
```

后端改动必须通过 `mvn verify`；前端工程建立后另需 `npm run typecheck` 与 `npm run build`。门禁细则见 [docs/engineering/workflow.md](docs/engineering/workflow.md) 第 6 节。

## 本地启动

骨架的 `application.yml` 仍指向 MySQL 与 Redis，但仓库不再提供容器编排与建表脚本：本地启动需要自备这两个中间件，或由交付对应能力的 Spec 一并重建 `docker/` 环境。构建产物可正常打包，骨架的实际启动行为未做验证。

```bash
mvn package
java -jar claim-platform-backend/claim-gateway/target/claim-gateway-0.1.0-SNAPSHOT.jar
java -jar claim-platform-backend/claim-sign-service/target/claim-sign-service-0.1.0-SNAPSHOT.jar
java -jar claim-platform-backend/claim-case-push-service/target/claim-case-push-service-0.1.0-SNAPSHOT.jar
```

## 架构状态

当前代码尚未实现目标业务能力；实现必须从 [项目基线](docs/specs/baseline.md) 出发，并通过活动 Feature Spec 定义本次交付的增量。对外兼容契约由基线第 8 节统一维护。
