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
├── specs/                     # 项目根约束和功能变更 Spec
├── standards/                 # DDD、代码、Spec、提交等工程规范
├── AGENTS.md                  # Agent 规则路由与执行门禁
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
| [specs/requirements.md](specs/requirements.md) | 项目稳定业务与架构约束 | 是，优先级最高 |
| `specs/<feature-name>/` | 功能需求、设计、任务和验收证据 | 是，不得与根需求冲突 |
| [standards/](standards/README.md) | DDD、Spec Coding、语言、前端和提交规范 | 是，按当前任务增量读取 |
| [AGENTS.md](AGENTS.md) | 指定任务需要读取哪些规范及执行门禁 | 是，作为规则入口 |
| `README.md` | 项目导航和技术基线 | 否 |

新增、修改、重构或修复功能时，先阅读根需求及对应功能 Spec，再按 [AGENTS.md](AGENTS.md) 的路由加载相关规范。不要为简单任务加载无关规范，也不要以 README 替代 Spec 或标准。

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
