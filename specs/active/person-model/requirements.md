# Person 标准模型与 DO 转换 Spec（requirements）

## 元信息

- 功能名称：`person-model`
- 所属限界上下文：Claim Intake（`claim-intake-service`）
- 文档类型：`requirements`
- 关联根约束：`specs/requirements.md`
- 状态：In Progress（领域模型与转换已实现，待完成测试改造和构建验证）

## 背景

`claim-platform` 的理赔受理（`t_claim_intake`）将申请人、出险人、主被保险人、领款人四类人员信息分别存储于 `applicant_info`、`accident_person_info`、`main_insured_info`、`payee_info` 四个 JSON 列中。当前这些字段在领域层被当作无结构的 `String`/JSON 透传，缺少统一的人员语义模型，导致：

1. 人员字段没有类型约束，无法在编译期区分四类角色；
2. 人员相关的业务规则（如证件号、与被保人关系）无处安放；
3. DO 与领域模型之间的转换散落在字符串层面，没有可复用的转换契约。

本 Spec 引入统一的 `Person` 标准领域模型及其四个子类型，并通过 MapStruct 实现的 `PersonConverter` 在 DO 转换阶段将相关人员信息映射为 `Person` 子类型。

## 范围（Scope）

- 在 Claim Intake 限界上下文内建立统一的 `Person` 标准领域模型与子类型：`Applicant`（申请人）、`AccidentPerson`（出险人）、`MainInsured`（主被保险人）、`Payee`（领款人）。
- 在 DO ↔ 领域模型转换时，通过 MapStruct 实现的 `PersonConverter` 将 `t_claim_intake` 的四个 JSON 列转换为对应 `Person` 子类型（双向）。
- 统一项目级技术栈：`MyBatis-Plus` 作为持久化框架、`Lombok` 削减样板代码、`Logback` + `SLF4J` 作为日志方案。

## 非范围（Non-Scope）

- 不改动 Case Delivery、Gateway 限界上下文的领域模型；Lombok 仅作为工程级依赖下沉，不在其他上下文引入各自领域的 `Person`。
- 不改变 `t_claim_intake` 表结构，人员信息仍存储于既有四个 JSON 列。
- 不新增写入用例、状态转换或对外接口；HTTP 请求契约（`JsonNode` 人员字段）保持不变。
- 不将 `ext_json` 纳入 `Person` 模型，继续作为通用扩展字段以 `String` 处理。

## 可验收需求（EARS）

### FR-1：DO 读取时转换为 Person 子类型

`WHEN` 持久化层从 `t_claim_intake` 读取一条申请记录 `THEN` 系统应将 `applicant_info`、`accident_person_info`、`main_insured_info`、`payee_info` 四个 JSON 列分别转换为领域 `Applicant`、`AccidentPerson`、`MainInsured`、`Payee` 子类型，并装配进领域聚合。

- 失败行为：JSON 解析或字段映射失败应抛出基础设施异常，由既有全局异常处理器映射为 `INTERNAL_ERROR`（不得泄露原始 JSON/SQL）。
- 幂等/兼容：仅影响内部表示，不改变幂等键（`external_apply_no`）与唯一约束。

### FR-2：聚合写回时序列化为 JSON 列

`WHEN` 领域聚合导出快照并写回 `THEN` 系统应将四个 `Person` 子类型分别序列化为对应 JSON 列，保持列名与历史数据兼容。

- 失败行为：序列化失败抛出基础设施异常，由持久化事务回滚。

### FR-3：请求边界通过 Converter 转为领域 Person

`WHEN` 接收到包含人员 JSON 的签收申请请求 `THEN` 系统应通过 `PersonConverter` 将人员信息转为领域 `Person` 子类型；领域层不得直接解析 JSON 字符串。

- 失败行为：非法 JSON 由 Web Adapter 在边界内以 `MALFORMED_REQUEST` 拒绝。

### FR-4：空值映射为 null Person

`WHEN` 任一人员字段在存储中为 `NULL` 或请求 JSON 为 null `THEN` 系统应将其映射为 `null` 领域 `Person`，不得抛异常或构造空对象。

### FR-5：项目技术栈统一

`WHEN` 项目编译或运行 `THEN` 系统应使用 `MyBatis-Plus` 作为持久化框架、`Lombok` 作为样板代码工具、`Logback` + `SLF4J` 作为日志方案，且日志可通过 `traceId`/`tenantCode` 关联。

- 兼容：Lombok 不引入运行时依赖；日志输出不得包含证件号、手机号等 PII 原文。

## 失败行为汇总

| 场景 | 处理 | 是否影响幂等 |
| --- | --- | --- |
| 人员 JSON 解析失败 | 基础设施异常 → `INTERNAL_ERROR` | 否 |
| 请求人员 JSON 非法 | Web Adapter → `MALFORMED_REQUEST` | 否 |
| 人员字段为 NULL | 映射为 `null` 领域对象 | 否 |

## 兼容性与数据所有权

- 数据仍由 Claim Intake 独占写入与读取；不改变跨上下文边界。
- 表结构不变，JSON 列内容向后兼容（新增角色字段为可选，旧数据缺字段映射为 `null`）。
- 对外 HTTP 契约不变；仅内部领域表示由 `String` 提升为强类型 `Person` 子类型。
