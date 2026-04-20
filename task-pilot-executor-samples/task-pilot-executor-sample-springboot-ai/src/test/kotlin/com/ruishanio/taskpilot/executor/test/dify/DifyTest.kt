package com.ruishanio.taskpilot.executor.test.dify

import com.ruishanio.taskpilot.core.executor.impl.TaskPilotSpringExecutor
import io.github.imfangs.dify.client.DifyClientFactory
import io.github.imfangs.dify.client.DifyWorkflowClient
import io.github.imfangs.dify.client.enums.ResponseMode
import io.github.imfangs.dify.client.model.workflow.WorkflowRunRequest
import io.github.imfangs.dify.client.model.workflow.WorkflowRunResponse
import kotlin.collections.Map
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Dify 集成样例测试，仅验证调用脚手架能在 Kotlin 下继续编译。
 * 该测试依赖外部 Dify 地址和有效凭证，默认自动化执行会受到网络与环境配置影响。
 */
@SpringBootTest
@Disabled("依赖外部 Dify 服务与真实凭证，保留为手动联调样例，不纳入默认自动化回归链路")
class DifyTest {
    @field:MockitoBean
    private lateinit var taskPilotSpringExecutor: TaskPilotSpringExecutor

    @Test
    @Throws(Exception::class)
    fun test() {
        val baseUrl = "https://xx.ai"
        val apiKey = "xx"
        val user = "zhangsan"
        val inputs: Map<String, Any> =
            mapOf(
                "input" to "请写一个java程序，实现一个方法，输入一个字符串，返回字符串的长度。"
            )

        val request =
            WorkflowRunRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.BLOCKING)
                .user(user)
                .build()

        val workflowClient: DifyWorkflowClient = DifyClientFactory.createWorkflowClient(baseUrl, apiKey)
        val response: WorkflowRunResponse = workflowClient.runWorkflow(request)

        logger.info("input: {}", inputs)
        logger.info("output: {}", response.data.outputs)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DifyTest::class.java)
    }
}
