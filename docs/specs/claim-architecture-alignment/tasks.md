# 架构标准流对齐任务

- [ ] 任务：重命名前端与后端工程目录，更新根 pom 与前端包名，保持模块内代码不变
- [ ] 任务：重命名后端两个业务服务模块目录与 Maven 坐标，更新父 pom 模块清单、`spring.application.name`、logback 上下文与日志文件名
- [ ] 任务：调整 Java 包根 `intake` → `sign`、`casedelivery` → `push`，同步 `package`、`import`、启动类名与 logback logger 名
- [ ] 任务：重命名签收服务 domain 层的聚合、值对象、状态、枚举与仓储端口词汇，并同步 infrastructure 层的 DO、表常量、Mapper、转换器与仓储实现命名
- [ ] 任务：按命令处理器与查询服务拆分 application 层包结构，重命名命令、结果、端口、异常与指标常量
- [ ] 任务：重命名 interfaces 层的控制器、请求对象、Web 契约常量类、错误码与异常处理器，保持 HTTP 路径与字段取值不变
- [ ] 任务：将租户协议抽象从 Handler 词汇改为 Adapter 词汇，包含注册表、reference 实现与协议转换管道命名
- [ ] 任务：同步测试类名、测试方法名与测试替身命名，不修改断言语义
- [ ] 任务：更新 README 的项目结构、构建与本地启动路径
- [ ] 任务：执行 `mvn verify` 并核对测试数量与本轮开始前一致
- [ ] 任务：回写基线的工程边界、限界上下文归属与 application 层包结构描述
