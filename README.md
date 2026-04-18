# Task-Pilot

一个分布式任务调度框架：调度中心集中管理任务，执行器通过注解接入业务代码，支持分片、失败转移、多语言接入、日志追踪与可视化运维。

## 功能特性

- 集中式调度中心：Web UI 管理任务、执行器、调度日志
- 注解式接入：`@TaskPilot` 将任一 Spring Bean 方法注册为可调度任务
- 运行模式：Bean / GLUE (Groovy) / Shell / Python / PHP 等
- 路由策略：第一个、最后一个、轮询、随机、一致性 Hash、最不经常使用、最近最少使用、故障转移、忙碌转移、分片广播
- 分片广播 + 动态参数：海量数据并行处理
- 弹性容错：调度超时、失败告警、失败重试
- 日志回溯：任务执行日志按任务 ID 聚合，支持 rolling 自动清理
- 管理端本地认证：Token 登录，Cookie 会话，可通过 SPI 接入外部认证

## 模块结构

```
task-pilot/
├── task-pilot-tool/                      # 通用工具库：collection / cache / concurrency / http / json / emoji ...
├── task-pilot-core/                      # 核心框架：调度协议、执行器、路由、日志
├── task-pilot-spring-boot-starter/       # Spring Boot Starter，自动装配执行器
├── task-pilot-admin/                     # 调度中心（Web 管理后台）
├── task-pilot-executor-samples/
│   ├── task-pilot-executor-sample-springboot/       # SpringBoot 执行器示例
│   └── task-pilot-executor-sample-springboot-ai/    # Spring AI / Dify 集成示例
├── task-pilot-frontend/                  # 前端工程（Vite + Vue）
├── doc/db/tables_task_pilot.sql          # PostgreSQL 建表脚本
└── docker-compose.yml                    # 一键启动：Postgres + Admin + 示例执行器
```

## 技术栈

| 领域       | 选型                                           |
| ---------- | ---------------------------------------------- |
| 运行时     | JDK 21+ · Kotlin 2.3.20                        |
| 框架       | Spring Boot 4.0.1 · Spring 7.0                 |
| 通信       | Netty 4.2                                      |
| 存储       | PostgreSQL 16 · MyBatis · HikariCP             |
| 脚本       | Groovy 5 · Shell · Python · PHP                |
| 可选       | Spring AI (Ollama) · Dify Java Client          |
| 构建       | Maven 3.9+                                     |

## 快速开始

### 方式一：Docker Compose（推荐）

```bash
# 1. 本地构建各模块（跳过测试与 release profile）
mvn -P '!release' -DskipTests clean install

# 2. 启动 Postgres + 调度中心 + 示例执行器
docker-compose up --build
```

- 调度中心：http://127.0.0.1:8080 （默认账号 `admin` / `123456`）
- 示例执行器：`http://127.0.0.1:9999`
- PostgreSQL：`127.0.0.1:5432`，库 `task_pilot`

### 方式二：IDEA 本地开发

1. 初始化数据库：
   ```bash
   psql -U postgres -c "CREATE DATABASE task_pilot;"
   psql -U postgres -d task_pilot -f doc/db/tables_task_pilot.sql
   ```
2. IDEA 中用根 `pom.xml` 导入，Maven 面板 **Reload All Maven Projects**
3. 启动 `task-pilot-admin` 的 `TaskPilotAdminApplication`
4. 启动 `task-pilot-executor-sample-springboot` 的 `TaskPilotExecutorApplication`

## 编写一个任务（Bean 模式）

引入 Starter：

```xml
<dependency>
    <groupId>com.ruishanio</groupId>
    <artifactId>task-pilot-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

在任意 Spring Bean 中声明任务方法：

```kotlin
@Component
class SampleJob {
    @TaskPilot("demoJobHandler")
    fun demoJobHandler() {
        TaskPilotHelper.log("hello task-pilot")
    }

    // 带自动注册，首次启动即在调度中心登记
    @TaskPilotRegister(jobDesc = "分片任务", scheduleConf = "0/3 * * * * ?")
    @TaskPilot("shardingJobHandler")
    fun shardingJobHandler() {
        val idx = TaskPilotHelper.getShardIndex()
        val total = TaskPilotHelper.getShardTotal()
        TaskPilotHelper.log("shard {}/{}", idx, total)
    }
}
```

更多示例见 [`task-pilot-executor-sample-springboot`](./task-pilot-executor-samples/task-pilot-executor-sample-springboot)。

## 执行器配置

`application.yml`：

```yaml
ruishan:
  task-pilot:
    admin:
      addresses: http://127.0.0.1:8080/task-pilot-admin
      accessToken: default_token
      timeout: 3
    executor:
      enabled: true
      appname: task-pilot-executor-sample
      ip: ""                 # 留空自动识别
      port: 9999
      logpath: ${user.home}/logs/task-pilot/jobhandler
      logretentiondays: 30
    auto-register:
      enabled: true
```

## 前端

`task-pilot-frontend` 提供 Vite + Vue 的新版管理前端：

```bash
cd task-pilot-frontend
npm install
npm run dev
```

调度中心已放通本地 5173 端口跨域（`ruishan.task-pilot.frontend.allowed-origin-patterns`）。

## License

本项目采用 [GPL-3.0](./LICENSE) 协议。
