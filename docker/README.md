# Docker 运行依赖

`docker/` 只存放本地运行所需的 Compose 编排、中间件初始化脚本和本地环境变量（`docker/.env`，由 `docker/.env.example` 复制）。中间件当前直接使用官方镜像，没有自建 Dockerfile。Kubernetes、Helm、Terraform 等生产部署资源出现后应单独放在 `infra/`，不混入本目录。

## 本地 MySQL 与 Redis

本地开发用一个 MySQL 容器承载两个互相独立的 schema——理赔签收的 `claim_intake_db` 与案件推送的 `push_db`，并启动一个 Redis 7 容器。

- MySQL 的单容器形态只为开发便利，不改变生产环境中每个限界上下文独立维护数据库与迁移历史的约束。
- Redis 仅供理赔签收的缓存、限流、短期锁或租约使用，不能作为签收、推送任务与回执终态的唯一事实来源。

```bash
cp docker/.env.example docker/.env
docker compose --env-file docker/.env -f docker/compose.yml up -d
docker compose --env-file docker/.env -f docker/compose.yml ps
```

首次创建 MySQL 数据卷时，`docker/mysql/init/01-create-databases.sql` 创建两个 schema 并授权本地 `claim` 用户；理赔签收的业务表由 `docker/mysql/init/02-create-claim-intake-tables.sql` 在 `claim_intake_db` 中创建，其中已包含签收号全局唯一索引 `uk_sign_no(sign_no)`。Redis 使用独立命名卷、AOF 持久化和 `redis-cli ping` healthcheck。

初始化脚本不会在已有 MySQL 数据卷上重复执行。若确认本地数据可以删除，需要重新初始化时执行：

```bash
docker compose --env-file docker/.env -f docker/compose.yml down -v
```

## 端口与连接

| 对象 | 默认值 | 覆盖方式 |
| --- | --- | --- |
| MySQL | 宿主机 `3306`，库名 `claim_intake_db` / `push_db`，用户 `claim` | 服务侧数据源配置 |
| Redis | 宿主机 `6379` | `REDIS_HOST`、`REDIS_PORT`，由 Spring Data Redis 与 Redisson 共享单节点连接 |
| claim-gateway | `8080` | `SERVER_PORT` |
| claim-sign-service | `8081` | `SERVER_PORT` |
| claim-case-push-service | `8082` | `SERVER_PORT` |

启动服务前先等待 MySQL 与 Redis 的 health 都为 `healthy`。

## 存量库补建签收号唯一索引

早于 `uk_sign_no` 创建的 MySQL 数据卷不会自动获得该索引，而签收服务启动时会校验它——缺失时直接拒绝启动，避免 Redis 计数器回退和 MySQL 降级失去最终唯一性屏障。

先确认存量数据没有重复签收号（结果必须为空，否则停止并人工处理）：

```sql
SELECT sign_no, COUNT(*) AS c
FROM claim_intake_db.t_claim_intake
GROUP BY sign_no
HAVING c > 1;
```

无重复时补建索引：

```sql
ALTER TABLE claim_intake_db.t_claim_intake
    ADD UNIQUE KEY uk_sign_no (sign_no);
```

索引定义与 `docker/mysql/init/02-create-claim-intake-tables.sql` 保持一致；新初始化环境无需执行本节。

## 中间件边界

当前 Compose 只提供 MySQL 和供理赔签收使用的 Redis。Nacos、RocketMQ、XXL-Job 与可观测组件尚未进入本地运行闭环，因此各服务的默认本地配置不会因连接这些依赖而启动失败；启用对应能力时再以独立 Spec 增加受控配置。

| 组件 | 最小职责 | 生产要求 |
| --- | --- | --- |
| MySQL 8.x | 理赔签收、案件推送各自独立数据库 | 主从或高可用、备份、慢查询与容量告警 |
| Redis 7.x | 幂等辅助、分布式锁、限流和资源配额 | Sentinel/Cluster、持久化与热点 Key 防护 |
| RocketMQ 5.x | 领域事件、延迟重试、死信 | NameServer、Broker 高可用、Topic 与 ACL 规划 |
| Nacos 2.x | 服务发现与配置 | 集群、命名空间隔离、配置变更审计 |
| XXL-Job 2.x | 补偿和扫描任务调度 | Admin 高可用、执行器隔离和任务告警 |
| Prometheus/Grafana/Loki | 指标、仪表盘与日志查询 | 长期存储、告警路由、租户标签治理 |
| OpenTelemetry Collector | Trace/Metric/Log 采集转发 | 采样策略、敏感字段脱敏与后端接入 |
