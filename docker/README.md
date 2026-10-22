# Docker 运行依赖

`docker/` 只存放 Dockerfile、Docker Compose、中间件初始化脚本和本地容器运行配置。Kubernetes、Helm、Terraform 等生产部署资源出现后，应单独放在 `infra/`，而不混入本目录。

## 本地 MySQL 与 Redis

本地开发使用一个 MySQL 容器承载两个独立 schema：`claim_intake_db` 和 `push_db`，并启动一个 Redis 7 容器。MySQL 的单容器形态仅用于开发便利，不改变生产环境中各限界上下文独立维护数据库和迁移历史的约束；Redis 仅供 Claim Intake 的缓存、限流、短期锁或租约使用，不能作为领域终态的唯一事实来源。

```bash
cp docker/.env.example docker/.env
docker compose --env-file docker/.env -f docker/compose.yml up -d
docker compose --env-file docker/.env -f docker/compose.yml ps
```

首次创建 MySQL 数据卷时，`mysql/init/01-create-databases.sql` 会创建两个 schema 并授权本地 `claim` 用户；Claim Intake 的业务表由 `mysql/init/02-create-claim-intake-tables.sql` 在 `claim_intake_db` 中创建。Redis 使用独立命名卷、AOF 持久化和 `redis-cli ping` healthcheck。初始化脚本不会在已有 MySQL 数据卷上重复执行；若确实需要重新初始化 MySQL 或 Redis，先确认本地数据可删除，再执行：

```bash
docker compose --env-file docker/.env -f docker/compose.yml down -v
```

本地启动 Claim Intake 前需要先等待 MySQL 与 Redis health 都为 `healthy`；Redis 默认映射宿主机 `6379`。Claim Intake 可通过 `REDIS_HOST`、`REDIS_PORT` 覆盖连接地址，默认分别为 `localhost`、`6379`，并由 Spring Data Redis 与 Redisson 共享该单节点连接配置。各服务默认端口为 Gateway `8080`、Claim Intake `8081` 和 Case Delivery `8082`。

## 中间件边界

当前 Compose 提供 MySQL 和供 Claim Intake 使用的 Redis。Nacos、RocketMQ、XXL-Job 和可观测组件尚未进入本地运行闭环，因此各服务的默认本地配置不会因连接这些依赖而启动失败；启用对应能力时再以独立 Spec 增加受控配置。

| 组件 | 最小职责 | 生产要求 |
| --- | --- | --- |
| MySQL 8.x | Claim Intake、Case Delivery 各自独立数据库 | 主从或高可用、备份、慢查询与容量告警 |
| Redis 7.x | 幂等辅助、分布式锁、限流和资源配额 | Sentinel/Cluster、持久化与热点 Key 防护 |
| RocketMQ 5.x | 领域事件、延迟重试、死信 | NameServer、Broker 高可用、Topic 与 ACL 规划 |
| Nacos 2.x | 服务发现与配置 | 集群、命名空间隔离、配置变更审计 |
| XXL-Job 2.x | 补偿和扫描任务调度 | Admin 高可用、执行器隔离和任务告警 |
| Prometheus/Grafana/Loki | 指标、仪表盘与日志查询 | 长期存储、告警路由、租户标签治理 |
| OpenTelemetry Collector | Trace/Metric/Log 采集转发 | 采样策略、敏感字段脱敏与后端接入 |
