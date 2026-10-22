# Java 开发规范

## 1. 基础编码规范

- 使用项目 `pom.xml` 指定的 Java 版本和依赖版本，不得自行升级 JDK、Spring Boot 或核心依赖。
- 修改代码只处理当前需求范围，不借机重构无关代码。
- 新增代码优先复用项目已有工具类、异常体系、基础组件和公共能力，不重复实现同类功能。
- 不保留无实际用途的类、方法、字段、注释代码、空实现和占位代码。
- 业务状态、类型、来源、固定编码不得使用散落的魔法值，应使用枚举或常量。
- 新代码中的日期时间优先使用 Java Time API：

```java
LocalDate
LocalDateTime
Instant
Duration
```

避免新增：

```java
Date
Calendar
SimpleDateFormat
```

## 2. 命名规范

### 类命名

类名使用大驼峰，并明确表达职责。

常见类型统一使用以下后缀：

| 类型 | 命名 |
| --- | --- |
| Controller | `Controller` |
| Service | `Service` |
| Repository 接口 | `Repository` |
| Repository 实现 | `RepositoryImpl` |
| MyBatis Mapper | `Mapper` |
| 数据库对象 | `DO` |
| 请求对象 | `Request` |
| 响应对象 | `Response` |
| 应用命令 | `Command` |
| 应用结果 | `Result` |
| 转换器 | `Converter` |
| 外部客户端 | `Client` |
| MQ 消费者 | `Consumer` |
| 定时任务 | `JobHandler` |
| 配置类 | `Configuration` / `Config` |

例如：

```java
ClaimIntakeController
ClaimIntakeCommandService
ClaimIntakeRepository
ClaimIntakeRepositoryImpl
ClaimIntakeMapper
ClaimIntakeDO
UploadMaterialsCommand
ClaimIntakeResult
ClaimIntakePersistenceConverter
```

同一项目中不得混用职责不明确的命名。

例如同时存在：

```java
ClaimIntakeMapper
ClaimIntakePersistenceMapper
```

如果一个表示 MyBatis Mapper，一个表示对象转换，应明确命名为：

```java
ClaimIntakeMapper
ClaimIntakePersistenceConverter
```

### 方法命名

方法使用小驼峰，直接表达业务行为。

推荐：

```java
findBySignNo()
uploadMaterials()
markMaterialUploaded()
createClaimIntake()
validateStatus()
convertToDomain()
```

避免使用含义模糊的方法名：

```java
handle()
process()
deal()
doSomething()
```

查询方法统一语义：

```text
get     明确应该存在
find    允许不存在
list    返回集合
count   返回数量
exists  判断是否存在
```

## 3. 注释与署名

### 类注释

每个生产类、接口、枚举、Record 和 Spring 配置类必须包含类级 Javadoc。

统一包含：

```java
/**
 * 签收申请命令服务。
 *
 * @author wangguangwu
 */
public class ClaimIntakeCommandService {
}
```

类注释说明：

- 类的主要职责；
- 必要的业务含义；
- 存在特殊设计时说明关键约束。

避免没有实际信息的注释：

```java
/**
 * ClaimIntakeService 类。
 *
 * @author wangguangwu
 */
```

### 方法注释

有业务含义的 `public` 或 `protected` 方法应提供 Javadoc。

例如：

```java
/**
 * 根据签收编号查询完整申请信息。
 *
 * @param signNo 签收编号
 * @return 签收申请
 */
public ClaimIntake getBySignNo(String signNo) {
}
```

存在重要业务约束时应明确说明：

```java
/**
 * 完成申请材料上传。
 *
 * <p>仅待上传材料状态允许执行，重复请求按业务幂等规则处理。</p>
 *
 * @param command 材料上传命令
 */
public void uploadMaterials(UploadMaterialsCommand command) {
}
```

以下内容不强制增加重复注释：

- Getter；
- Setter；
- 构造器；
- 简单 DTO 转换；
- 语义明确的简单私有方法。

行内注释优先说明设计原因和特殊约束，不重复代码本身。

推荐：

```java
// 外部系统可能重复推送，数据库唯一键作为最终幂等兜底
claimIntakeRepository.save(claimIntake);
```

避免：

```java
// 查询申请
ClaimIntake claimIntake = repository.findBySignNo(signNo);
```

## 4. 异常与日志

### 异常

异常应具有明确语义。

推荐：

```java
ClaimIntakeNotFoundException
InvalidIntakeStatusException
ConcurrentIntakeModificationException
```

避免直接使用：

```java
throw new RuntimeException("error");
```

只有以下场景需要主动捕获异常：

- 转换为当前业务能够理解的异常；
- 补充必要业务上下文；
- 执行明确的降级或补偿；
- 隔离第三方 SDK 或外部系统异常。

重新抛出时必须保留原始异常：

```java
throw new ClaimIntakeException(
        "材料上传失败，signNo=" + signNo,
        e
);
```

避免：

```java
catch (Exception e) {
    throw new RuntimeException(e.getMessage());
}
```

### 日志

统一使用 SLF4J。

```java
@Slf4j
```

日志使用占位符：

```java
log.info("签收申请创建成功，signNo={}", signNo);
```

禁止字符串拼接：

```java
log.info("签收申请创建成功，signNo=" + signNo);
```

异常日志必须保留完整异常堆栈：

```java
log.error("签收申请处理失败，signNo={}", signNo, e);
```

避免：

```java
log.error("签收申请处理失败：{}", e.getMessage());
```

日志优先携带能够定位业务问题的标识，例如：

```text
tenantCode
signNo
externalApplyNo
claimNo
traceId
```

禁止记录敏感信息：

```text
完整身份证号
完整银行卡号
Token
密码
密钥
完整敏感请求报文
```

高频循环中避免打印大量 `info` 日志。

## 5. Java 与 Spring 使用约束

### 依赖注入

Spring Bean 统一使用构造器注入。

推荐：

```java
@Service
@RequiredArgsConstructor
public class ClaimIntakeCommandService {

    private final ClaimIntakeRepository claimIntakeRepository;
}
```

禁止字段注入：

```java
@Autowired
private ClaimIntakeRepository claimIntakeRepository;
```

### Lombok

可以按项目现有习惯使用 Lombok。

常用：

```java
@Getter
@Setter
@Data
@Builder
@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
```

包含核心业务状态的对象谨慎使用 `@Data`，避免自动暴露 Setter。

推荐：

```java
@Getter
public class ClaimIntake {

    private IntakeStatus status;

    public void markMaterialUploaded() {
        // 状态变更
    }
}
```

避免通过：

```java
claimIntake.setStatus(...);
```

任意修改核心状态。

### 集合

返回集合时优先返回空集合，不返回 `null`。

推荐：

```java
return Collections.emptyList();
```

Stream 适合简单过滤、转换和聚合：

```java
materials.stream()
        .filter(IntakeMaterial::isValid)
        .map(IntakeMaterial::getFileName)
        .toList();
```

包含复杂状态修改、异常处理或多步骤业务逻辑时优先使用普通循环。

禁止使用 `peek()` 承担业务修改：

```java
list.stream()
        .peek(item -> item.setStatus(...))
        .toList();
```

## 6. 测试与校验

修改业务行为时必须同步新增或修改测试。

测试至少覆盖：

- 正常路径；
- 关键边界；
- 异常路径；
- 需求明确涉及的幂等、重复请求或并发场景。

测试名称应直接体现：

```text
业务场景 + 条件 + 预期结果
```

例如：

```java
void shouldUploadMaterialsWhenStatusIsImageNotUpload()

void shouldRejectUploadWhenMaterialsAlreadyUploaded()

void shouldThrowWhenClaimIntakeNotFound()
```

不得为了提高覆盖率编写没有有效断言的测试。

代码修改后至少确认：

- 项目能够正常编译；
- 不存在未使用的 import；
- 包名和类名调整后的 `package`、`import` 已同步更新；
- 受影响测试正常通过；
- 不存在明显编译告警或 IDE 错误。

准备合并或提交后端代码时执行：

```bash
mvn verify
```

无法执行时，需要说明原因和可能影响。