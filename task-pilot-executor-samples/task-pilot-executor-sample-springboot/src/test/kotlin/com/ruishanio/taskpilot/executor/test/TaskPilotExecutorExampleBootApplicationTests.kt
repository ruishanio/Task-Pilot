package com.ruishanio.taskpilot.executor.test

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * SpringBoot 样例工程的基础上下文测试。
 * 这里只验证配置类与 Bean 装配是否可启动，不需要真的拉起执行器远程端口。
 */
@SpringBootTest(
    properties = [
        "task-pilot.executor.enabled=false",
        "task-pilot.auto-register.enabled=false"
    ]
)
class TaskPilotExecutorExampleBootApplicationTests {
    @Test
    fun test() {
        println(11)
    }
}
