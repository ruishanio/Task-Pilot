package com.ruishanio.taskpilot.executor.test

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 样例工程的基础上下文测试。
 * AI 样例在默认配置下会尝试启动固定端口的执行器，这里显式关闭执行器与启动同步，只保留上下文烟测。
 */
@SpringBootTest(
    properties = [
        "task-pilot.executor.enabled=false",
        "task-pilot.sync.enabled=false"
    ]
)
class BaseTests {
    @Test
    fun test() {
        println(11)
    }
}
