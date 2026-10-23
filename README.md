# claim-platform

多租户理赔业务平台。当前覆盖线上申请签收与案件推送；运营查询与异常诊断在出现明确用例后作为独立限界上下文建设。平台不承担案件审核、赔付决策或外部保司的核心业务处理。

## 项目结构

```text
claim-platform/
├── frontend/                  # Vue 3 + TypeScript 管理端
├── backend/                   # Java 17 + Spring Boot 多模块后端
│   ├── claim-gateway/         # 认证、租户识别、路由、限流、Trace
│   ├── claim-intake-service/  # Claim Intake：理赔受理限界上下文
│   ├── claim-case-delivery/   # Case Delivery：案件推送限界上下文
│   └── claim-common/          # 无领域规则的共享技术能力与稳定契约
├── docker/                    # Dockerfile、Compose 与本地容器资源
├── docs/                      # 规范与需求文档
│   ├── specs/                 # 项目基线、功能 Spec 与历史归档
│   └── engineering/           # 流程、Spec 与代码规范
├── AGENTS.md                  # Agent 开发入口与规则导航
└── README.md                  # 项目入口与治理导航
```

## 技术基线

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Alibaba、MySQL、Redis、RocketMQ、XXL-Job。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios。
- 可观测：OpenTelemetry、Prometheus、Grafana、Loki。
- 容器：Docker、Docker Compose。

## 工程治理与渐进披露

| 文件或目录 | 职责 | 是否作为实现依据 |
| --- | --- | --- |
| [AGENTS.md](AGENTS.md) | 任务入口与规则归属导航 | 是，作为规则入口 |
| [docs/specs/baseline.md](docs/specs/baseline.md) | 项目长期有效的公共需求与架构约束 | 是，优先级最高 |
| `docs/specs/<feature-name>/` | Feature 级改动的需求、设计与任务 | 是，该功能的直接依据 |
| `docs/specs/archive/<feature-name>/` | 已完成功能的决策与验证证据 | 否，仅在历史追溯时读取 |
| [docs/engineering/workflow.md](docs/engineering/workflow.md) | 开发生命周期与 Git 操作 | 是 |
| [docs/engineering/specification.md](docs/engineering/specification.md) | Spec 编写与一致性规则 | 是 |
| [docs/engineering/implementation.md](docs/engineering/implementation.md) | 代码修改规范 | 是，改代码前读取 |
| `README.md` | 项目导航和技术基线 | 否 |

任务从 [AGENTS.md](AGENTS.md) 进入，按其执行顺序读取命中当前改动的文件，不为简单任务加载无关规范。是否需要建立 `docs/specs/<feature-name>/` 三件套由 [docs/engineering/specification.md](docs/engineering/specification.md) 判定；本 README 只作导航，不替代需求或规范。

## 构建校验

```bash
mvn verify
```

## 本地启动

先按 [Docker 运行说明](docker/README.md) 启动 MySQL 与 Redis，构建全部后端可执行包，再单独启动需要调试的服务：

```bash
mvn package
java -jar backend/claim-gateway/target/claim-gateway-0.1.0-SNAPSHOT.jar
java -jar backend/claim-intake-service/target/claim-intake-service-0.1.0-SNAPSHOT.jar
java -jar backend/claim-case-delivery/target/claim-case-delivery-0.1.0-SNAPSHOT.jar
```

每个服务都通过 `/actuator/health` 提供本地存活检查。当前只提供可启动的基础骨架，不代表签收或推送业务能力已经实现。
