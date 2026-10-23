# Claim Intake DDD 包结构重构需求

## 项目背景与目标

Claim Intake 已具备受理、材料登记与查询能力，但 Web、应用服务和持久化实现尚未按照当前工程标准细分目录。本变更仅整理 `claim-intake-service` 的 Java 包结构和必要命名，使其统一为 `interfaces`、`application`、`domain`、`infrastructure` 四层，并维持既有业务边界和运行行为。

## 范围

- 所属限界上下文：Claim Intake。
- 将现有 `adapter/web` 入口迁移到 `interfaces/web` 的职责子包。
- 将 Application Service 迁移到 `application/service`。
- 将持久化 DO、MyBatis Mapper、Domain/DO 转换器、Store 实现和 MyBatis 配置迁移到 `infrastructure/persistence` 的职责子包。
- 将生产与测试源码的 `package`、`import`、MapStruct 生成类型引用和 Spring 组件扫描引用同步更新。
- 将对象转换器 `ClaimIntakePersistenceMapper` 重命名为 `ClaimIntakePersistenceConverter`，消除与 MyBatis Mapper 的职责歧义。

## 非范围

- 不改变 `ClaimIntake` 聚合、状态机、幂等键、事务边界、租户校验、错误码或交互日志行为。
- 不改变 HTTP 路径、请求字段、响应信封或数据库 Schema、迁移脚本与配置项。
- 不新增 Repository/Store 抽象、不合并现有 Store、不引入 CQRS、Port/Adapter 或其他架构模式。
- 不创建当前不存在的 Job、消息消费、缓存或外部客户端空包和占位类。

## 验收需求

### FR-REF-001 四层包结构

WHEN 开发者检查 Claim Intake 的生产源码 THEN THE SYSTEM SHALL 使用 `interfaces`、`application`、`domain`、`infrastructure` 四层包组织实际存在的职责，且不再保留 `adapter` 生产包。

### FR-REF-002 输入边界隔离

WHEN HTTP 请求进入 Claim Intake THEN THE SYSTEM SHALL 由 `interfaces/web` 下的 Controller、Request、Advice、Context、Exception、Interceptor 和 Config 完成协议处理，并且 Controller 不得直接访问 Store、Mapper、DO 或数据库客户端。

### FR-REF-003 应用与领域边界

WHEN 签收、材料登记或查询用例执行 THEN THE SYSTEM SHALL 保持 `application/service` 对用例的编排职责，以及 `domain` 对聚合、状态转换、领域异常和 Store 抽象的所有权，且 Domain 不依赖 Interfaces 或 Infrastructure 类型。

### FR-REF-004 持久化职责分类

WHEN 基础设施执行持久化 THEN THE SYSTEM SHALL 将 DO、MyBatis Mapper、Domain/DO Converter、Store 实现和 MyBatis 配置分别放在 `persistence/dataobject`、`mapper`、`converter`、`repository` 和 `config`，且技术类型不得泄露至 Domain。

### FR-REF-005 行为兼容

WHEN 原有 HTTP 接口和单元测试在重构后运行 THEN THE SYSTEM SHALL 保持 API 契约、状态迁移、幂等语义、租户隔离和数据库访问行为不变。

## 失败行为与兼容性影响

- 本变更不新增业务失败类型或错误码；重构造成的包名、组件扫描、MapStruct 生成或依赖注入错误必须在构建或启动阶段显式失败，不得通过保留旧包兼容副本掩盖。
- 生产源码包名改变是模块内实现变更；未发布 Java 包作为外部契约，不影响 HTTP、数据库或领域事件兼容性。
- 写入幂等键仍为 `tenantCode + externalApplyNo`，材料登记仍不声明请求级幂等；重构不得改变该语义。

## 迁移与已知限制

- 不需要数据库、配置或数据迁移；部署时以本次构建产物整体替换即可。
- 当前不存在 XXL-Job Handler、消息消费者、缓存或外部客户端实现，因此对应目录不创建。
