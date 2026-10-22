# DDD 设计规范

## 适用范围

适用于聚合、领域服务、仓储、领域事件、跨上下文 API、数据模型和外部系统接入的新增或修改。具体限界上下文、数据所有权和安全边界以 `specs/requirements.md` 为准。

## 分层规范

统一采用四层结构：

```text
interfaces
application
domain
infrastructure
```

整体调用关系：

```text
外部请求
    ↓
interfaces
    ↓
application
    ↓
domain

application
    ↓
domain Repository 接口
    ↑
infrastructure Repository 实现

application
    ↓
infrastructure 外部能力
```

依赖原则：

```text
interfaces → application → domain

infrastructure → domain
```

`domain` 不依赖其他层。

## Interfaces

`interfaces` 负责系统入口，将外部协议转换成应用层可以理解的输入。

目录：

```text
interfaces
├─ web
│  ├─ controller
│  ├─ request
│  ├─ response
│  ├─ advice
│  ├─ exception
│  ├─ context
│  ├─ interceptor
│  └─ config
│
├─ job
│  └─ handler
│
└─ message
   └─ consumer
```

按实际需要创建目录，不创建无实际用途的空包。

### web

存放 HTTP 接口相关代码。

```text
web
├─ controller    HTTP 接口入口
├─ request       HTTP 请求对象
├─ response      HTTP 返回对象
├─ advice        全局异常处理
├─ exception     Web 接入层异常
├─ context       请求上下文
├─ interceptor   Web 拦截器
└─ config        Web 配置
```

调用关系：

```text
HTTP Request
    ↓
Controller
    ↓
Request → Command
    ↓
Application Service
```

Controller 只负责：

- 接收请求；
- 基础参数校验；
- 获取请求上下文；
- Request 转换为 Command；
- 调用 Application Service；
- 将 Result 转换为 Response。

Controller 不直接访问 Repository、Mapper 或数据库。

### job

存放 XXL-Job 等调度入口。

```text
job
└─ handler
   ├─ ClaimIntakeRetryJobHandler
   └─ ClaimIntakeCompensateJobHandler
```

调用关系：

```text
XXL-Job
    ↓
JobHandler
    ↓
Application Service
```

JobHandler 负责：

- 接收调度；
- 获取任务参数；
- 调用 Application Service；
- 记录任务执行结果。

业务状态判断、补偿规则不放在 JobHandler。

### message

存在 MQ 消费入口时使用。

```text
message
└─ consumer
   └─ ClaimStatusConsumer
```

调用关系：

```text
RocketMQ
    ↓
Consumer
    ↓
Application Service
```

Consumer 负责协议解析、消息基础校验和应用服务调用。

消息中的外部 DTO 不直接进入 Domain。

## Application

`application` 负责一个完整业务用例的编排。

目录：

```text
application
├─ service
├─ command
└─ result
```

### service

存放 Application Service。

例如：

```text
service
├─ ClaimIntakeCommandService
└─ ClaimIntakeQueryService
```

职责：

- 接收 Command；
- 加载领域对象；
- 调用领域行为；
- 调用 Repository；
- 调用必要的外部能力；
- 控制事务边界；
- 返回 Result。

典型调用：

```text
Interfaces
    ↓
Application Service
    ├─ Repository
    ├─ Domain
    └─ External Service
```

Application Service 负责流程编排，聚合内部业务规则放在 Domain。

### command

表示一个写入用例的输入。

```text
command
├─ SubmitIntakeCommand
└─ UploadMaterialsCommand
```

例如：

```text
CreateIntakeRequest
        ↓
SubmitIntakeCommand
        ↓
ClaimIntakeCommandService
```

Command 不包含 Web、MyBatis 或外部系统协议对象。

### result

表示 Application 对外返回的应用结果。

```text
result
├─ ClaimIntakeResult
└─ ClaimIntakeDetailResult
```

调用关系：

```text
Domain / Repository
        ↓
Application Service
        ↓
Result
        ↓
Interfaces Response
```

## Domain

`domain` 放置核心业务模型和业务规则。

目录：

```text
domain
├─ model
├─ repository
├─ service
└─ exception
```

### model

存放：

- 聚合根；
- 实体；
- 值对象；
- 状态；
- 枚举；
- 聚合内部业务行为。

例如：

```text
model
├─ ClaimIntake
├─ IntakeMaterial
├─ IntakeStatus
└─ person
   ├─ Applicant
   ├─ AccidentPerson
   ├─ MainInsured
   └─ Payee
```

例如签收聚合：

```text
ClaimIntake
├─ signNo
├─ externalApplyNo
├─ tenantCode
├─ status
├─ applicant
├─ accidentPerson
├─ mainInsured
├─ payee
└─ materials
   └─ List<IntakeMaterial>
```

数据库可以对应：

```text
t_sign_application
t_sign_material
```

数据库表的拆分不决定领域模型的拆分。

如果 `ClaimIntake` 加载时要求包含全部材料，则 Repository 负责将多张表重建为完整 `ClaimIntake`。

### repository

定义领域需要的持久化能力。

```text
repository
└─ ClaimIntakeRepository
```

例如：

```java
public interface ClaimIntakeRepository {

    Optional<ClaimIntake> findBySignNo(String signNo);

    void save(ClaimIntake claimIntake);
}
```

Repository：

- 面向领域模型；
- 返回 Domain 对象；
- 不返回 DO；
- 不暴露 Mapper；
- 不包含 MyBatis 实现。

调用关系：

```text
Application
    ↓
ClaimIntakeRepository
```

具体实现由 Infrastructure 提供。

### service

只有业务规则无法自然归属单个聚合或实体时，才使用 Domain Service。

```text
domain
└─ service
   └─ XxxDomainService
```

调用关系：

```text
Application Service
        ↓
Domain Service
        ↓
Domain Model
```

不要把 Application Service 的流程编排放入 Domain Service。

### exception

存放领域规则产生的异常。

例如：

```text
exception
├─ ClaimIntakeNotFoundException
├─ InvalidIntakeStatusException
└─ ConcurrentIntakeModificationException
```

## Infrastructure

`infrastructure` 负责技术实现和外部系统访问。

目录：

```text
infrastructure
├─ persistence
│  ├─ dataobject
│  ├─ mapper
│  ├─ converter
│  ├─ repository
│  └─ config
│
├─ client
├─ messaging
├─ cache
└─ storage
```

### persistence

负责数据库访问。

```text
persistence
├─ dataobject
├─ mapper
├─ converter
├─ repository
└─ config
```

#### dataobject

数据库对象：

```text
dataobject
├─ ClaimIntakeDO
├─ IntakeMaterialDO
├─ InteractionLogDO
└─ person
```

DO 只用于 Infrastructure，不进入 Domain。

#### mapper

MyBatis Mapper：

```text
mapper
├─ ClaimIntakeMapper
├─ IntakeMaterialMapper
└─ InteractionLogMapper
```

Mapper 面向数据库表。

#### converter

负责：

```text
DO ↔ Domain
```

例如：

```text
converter
├─ ClaimIntakePersistenceConverter
└─ PersonConverter
```

#### repository

实现 Domain Repository。

```text
repository
└─ ClaimIntakeRepositoryImpl
```

完整调用关系：

```text
Application
    ↓
ClaimIntakeRepository
    ↓
ClaimIntakeRepositoryImpl
    ├─ ClaimIntakeMapper
    ├─ IntakeMaterialMapper
    └─ Converter
           ↓
      ClaimIntake
```

Repository 面向聚合，Mapper 面向数据表。

### client

存放当前服务调用其他服务的客户端。

例如：

```text
client
├─ PolicyClient
└─ AuditClient
```

OpenFeign 放在这里。

调用关系：

```text
Application
    ↓
Client / Gateway
    ↓
Feign
    ↓
External Service
```

别人通过 Feign 调用当前服务时，当前服务的入口仍然属于：

```text
interfaces/web
```

### messaging

存放当前服务向 MQ 发送消息的技术实现。

```text
messaging
└─ producer
   └─ ClaimEventProducer
```

MQ Consumer 属于 `interfaces/message`。

方向：

```text
发送消息：
Application → infrastructure/messaging

接收消息：
interfaces/message → Application
```

### cache

Redis 等缓存实现：

```text
cache
└─ XxxCacheRepository
```

Domain 不直接依赖 Redis。

### storage

OSS、S3、文件存储等能力：

```text
storage
└─ OssStorageService
```

调用关系：

```text
Application
    ↓
Storage abstraction
    ↓
OSS implementation
```

## 建模规则

- 一个聚合只维护必须原子一致的业务事实；跨聚合和跨上下文协作通过事件或明确的应用服务边界完成。
- 聚合必须显式定义标识、合法状态转换、不变量、并发策略与幂等键；不能把这些规则分散到 Controller、Repository 或定时任务中。
- `domain` 不依赖 Web、JPA、消息客户端、缓存或外部协议；`application` 编排用例与事务边界；`interfaces` 处理输入协议；`infrastructure` 实现持久化和外部系统访问。
- `claim-common` 只能包含无领域含义的技术能力和稳定契约；业务实体、Repository、领域规则及跨上下文共享表不得进入该模块。

## 上下文协作

- 不跨上下文访问数据库、Repository 或领域实体；需要数据时使用版本化 API、事件投影或反腐层。
- API 与事件只暴露调用方需要的稳定契约；外部 DTO、错误码和协议细节必须在反腐层终止，不得穿透领域层。
- 新增或变更接口、事件、共享契约、数据库边界、租户模型时，先更新功能 Spec；影响项目级边界时同步更新根需求并评审。
- 领域事件的字段、版本与兼容性规则遵循根需求；事件消费者必须把重复投递、乱序、失败重试和人工介入纳入设计。

## 公共模块规范

`claim-common` 只能包含无领域含义的技术能力和稳定契约；业务实体、Repository、领域规则及跨上下文共享表不得进入该模块。

## 设计检查

- 每个写入用例是否能说明事实所有者、幂等键、状态变化、失败分类与补偿路径？
- 每份数据是否只有一个限界上下文负责写入？
- 任何跨边界调用是否有清晰的契约、超时、失败语义和兼容策略？
- 租户、审计和敏感信息约束是否在模型、存储、缓存、事件和观测链路中一致生效？