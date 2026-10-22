# Docker 运行依赖

`docker/` 只存放 Dockerfile、Docker Compose、中间件初始化脚本和本地容器运行配置。Kubernetes、Helm、Terraform 等生产部署资源出现后，应单独放在 `infra/`，而不混入本目录。

当前工程仅提供依赖声明，不提供应用镜像、Docker Compose 或中间件初始化脚本。环境落地时需要准备以下实例，并由 Nacos 按环境维护各模块配置。

| 组件 | 最小职责 | 生产要求 |
| --- | --- | --- |
| MySQL 8.x | Sign、Push、Manage 各自独立数据库 | 主从或高可用、备份、慢查询与容量告警 |
| Redis 7.x | 幂等辅助、分布式锁、限流和资源配额 | Sentinel/Cluster、持久化与热点 Key 防护 |
| RocketMQ 5.x | 领域事件、延迟重试、死信 | NameServer、Broker 高可用、Topic 与 ACL 规划 |
| Nacos 2.x | 服务发现与配置 | 集群、命名空间隔离、配置变更审计 |
| XXL-Job 2.x | 补偿和扫描任务调度 | Admin 高可用、执行器隔离和任务告警 |
| Prometheus/Grafana/Loki | 指标、仪表盘与日志查询 | 长期存储、告警路由、租户标签治理 |
| OpenTelemetry Collector | Trace/Metric/Log 采集转发 | 采样策略、敏感字段脱敏与后端接入 |
