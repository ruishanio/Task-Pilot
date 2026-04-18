package com.ruishanio.taskpilot.openapi

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.response.Response
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Admin 开放接口客户端测试。
 * 依赖本地 admin 服务，仅用于校验客户端代理与模型仍可编译。
 */
class AdminBizTest {
    private fun buildClient(): AdminBiz {
        return HttpTool.createClient()
            .url(addressUrl)
            .timeout(3 * 1000)
            .header(Const.TASK_PILOT_ACCESS_TOKEN, accessToken)
            .proxy(AdminBiz::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun callback() {
        val adminBiz = buildClient()
        val param =
            CallbackRequest().apply {
                logId = 1
                handleCode = TaskPilotContext.HANDLE_CODE_SUCCESS
            }

        val returnT: Response<String> = adminBiz.callback(listOf(param))
        assertTrue(returnT.isSuccess)
    }

    @Test
    @Throws(Exception::class)
    fun registry() {
        val adminBiz = buildClient()
        val registryParam = RegistryRequest(RegistType.EXECUTOR.name, "task-pilot-executor-example", "127.0.0.1:9999")
        val returnT: Response<String> = adminBiz.registry(registryParam)
        assertTrue(returnT.isSuccess)
    }

    @Test
    @Throws(Exception::class)
    fun registryRemove() {
        val adminBiz = buildClient()
        val registryParam = RegistryRequest(RegistType.EXECUTOR.name, "task-pilot-executor-example", "127.0.0.1:9999")
        val returnT: Response<String> = adminBiz.registryRemove(registryParam)
        assertTrue(returnT.isSuccess)
    }

    @Test
    @Throws(Exception::class)
    fun jobManage() {
        logger.info("jobAdd、jobUpdate、jobRemove、jobStart、jobStop")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AdminBizTest::class.java)
        private const val addressUrl = "http://127.0.0.1:8080/task-pilot-admin"
        private const val accessToken = "default_token"
    }
}
