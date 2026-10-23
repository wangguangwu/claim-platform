# claim-platform

理赔平台（Claim Platform）负责理赔链路的两段交互：

- **前段**：理赔申请签收（`Sign`）、材料（`Material`）、问题件（`ClaimIssue`）、撤销等与租户的交互。
- **后段**：案件生成后，案件推送（`PushTask` / `PushRecord` / `PushReceipt`）等与保司的交互。

平台不承担案件审核、理算与核赔，也不处理外部保司系统的内部业务。

## 项目结构

```text
claim-platform/
├── claim-platform-frontend/      # 统一前端（Vue 3 + TypeScript）：sign / material / issue / case / push / tenant
├── claim-platform-backend/       # 后端 Maven 聚合工程
│   ├── claim-gateway/            # 租户接入边缘层：认证、验签、加解密、协议适配、路由、限流、Trace
│   ├── claim-sign-service/       # 理赔签收限界上下文
│   ├── claim-case-push-service/  # 案件推送限界上下文
│   └── claim-common/             # 无领域规则的共享技术能力与稳定契约（不计入服务结构）
├── docker/                       # Docker Compose、中间件初始化脚本与本地运行配置
├── docs/
│   ├── specs/                    # 项目基线与活动功能 Spec
│   └── engineering/              # 流程、Spec 与代码规范
├── AGENTS.md                     # Agent 开发入口与规则导航
└── README.md                     # 本文件
```

## 服务与当前状态

| 模块 | 职责 | 默认端口 | 当前状态 |
| --- | --- | --- | --- |
| `claim-gateway` | 所有外部租户接入相关能力 | 8080 | 仅可启动骨架；租户协议适配仍在签收服务 |
| `claim-sign-service` | 签收提交、幂等回放、发号、材料登记与查询 | 8081 | 核心链路已实现，库表为 `claim_intake_db` |
| `claim-case-push-service` | 推送任务、推送记录、回执、推送策略 | 8082 | 仅可启动骨架；`push_db` 尚无业务表 |
| `claim-common` | 响应信封 `ApiResult<T>` 等稳定技术契约 | — | 无领域规则 |
| `claim-platform-frontend` | 六个功能域的查询与交互页面 | — | 仅工程占位，尚无页面代码 |

每个可执行服务通过 `/actuator/health` 提供本地存活检查。可启动骨架不代表对应业务能力已经实现。

## 技术基线

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Alibaba、MySQL 8、Redis 7、RocketMQ、XXL-Job。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios。
- 可观测：OpenTelemetry、Prometheus、Grafana、Loki。
- 容器：Docker、Docker Compose。

## 工程治理与渐进披露

| 文件或目录 | 职责 | 是否作为实现依据 |
| --- | --- | --- |
| [AGENTS.md](AGENTS.md) | 任务入口与规则归属导航 | 是，作为规则入口 |
| [docs/specs/baseline.md](docs/specs/baseline.md) | 长期有效的公共需求、命名权威与架构约束 | 是，优先级最高 |
| `docs/specs/<feature-name>/` | Feature 级改动的需求、设计与任务 | 是，该功能的直接依据 |
| [docs/engineering/workflow.md](docs/engineering/workflow.md) | 开发生命周期与全部 Git 操作 | 是 |
| [docs/engineering/specification.md](docs/engineering/specification.md) | Spec 的建与不建、职责边界、交付后清理 | 是 |
| [docs/engineering/implementation.md](docs/engineering/implementation.md) | 代码修改规范与 HTTP 契约细则 | 是，改代码前读取 |
| `README.md` | 项目导航与技术基线速览 | 否 |

历史决策不单独归档：已交付功能的长期结论回写基线，过程性内容随 Spec 目录删除，需要追溯时用 `git log` 与提交说明。

任务从 [AGENTS.md](AGENTS.md) 进入，按其执行顺序读取命中当前改动的文件，不为简单任务加载无关规范；本 README 只作导航，不替代需求或规范。

## 构建与验证

```bash
mvn verify
```

后端改动必须通过 `mvn verify`；涉及前端时另需 `npm run typecheck` 与 `npm run build`。门禁细则见 [docs/engineering/workflow.md](docs/engineering/workflow.md) 第 6 节。

## 本地启动

先按 [Docker 运行说明](docker/README.md) 启动 MySQL 与 Redis，再构建并单独启动需要调试的服务：

```bash
mvn package
java -jar claim-platform-backend/claim-gateway/target/claim-gateway-0.1.0-SNAPSHOT.jar
java -jar claim-platform-backend/claim-sign-service/target/claim-sign-service-0.1.0-SNAPSHOT.jar
java -jar claim-platform-backend/claim-case-push-service/target/claim-case-push-service-0.1.0-SNAPSHOT.jar
```

## 已知偏差

目标架构与代码现状的差距、以及刻意保持不变的对外契约（HTTP 路径、错误码字符串、指标名、Redis 键前缀、库表列名）统一登记在 [docs/specs/baseline.md](docs/specs/baseline.md) 第 8 节，本 README 不重复维护。
