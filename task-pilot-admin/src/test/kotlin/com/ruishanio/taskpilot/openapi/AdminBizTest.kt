package com.ruishanio.taskpilot.openapi

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.openapi.model.AutoRegisterRequest
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.response.Response
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

/**
 * Admin 开放接口客户端测试。
 *
 * 使用本地轻量 HTTP 假服务验证客户端代理、路径拼接和请求体协议，避免依赖外部 admin 进程。
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
        assertTrue(lastRequestBody.get().contains("\"handleCode\":200"))
    }

    @Test
    @Throws(Exception::class)
    fun registry() {
        val adminBiz = buildClient()
        val registryParam = RegistryRequest(RegistType.EXECUTOR.name, "task-pilot-executor-example", "127.0.0.1:9999")
        val returnT: Response<String> = adminBiz.registry(registryParam)
        assertTrue(returnT.isSuccess)
        assertTrue(lastRequestBody.get().contains("\"registryGroup\":\"EXECUTOR\""))
    }

    @Test
    @Throws(Exception::class)
    fun registryRemove() {
        val adminBiz = buildClient()
        val registryParam = RegistryRequest(RegistType.EXECUTOR.name, "task-pilot-executor-example", "127.0.0.1:9999")
        val returnT: Response<String> = adminBiz.registryRemove(registryParam)
        assertTrue(returnT.isSuccess)
        assertTrue(lastRequestBody.get().contains("\"registryKey\":\"task-pilot-executor-example\""))
    }

    @Test
    @Throws(Exception::class)
    fun autoRegisterShouldSendEnumNamesInRequestBody() {
        val adminBiz = buildClient()
        val request =
            AutoRegisterRequest().apply {
                appname = "demo-app"
                title = "示例执行器"
                tasks.add(
                    AutoRegisterRequest.Task().apply {
                        executorHandler = "demoHandler"
                        jobDesc = "枚举任务"
                        scheduleType = ScheduleTypeEnum.CRON
                        scheduleConf = "0/10 * * * * ?"
                        misfireStrategy = MisfireStrategyEnum.FIRE_ONCE_NOW
                        executorRouteStrategy = ExecutorRouteStrategyEnum.SHARDING_BROADCAST
                        executorBlockStrategy = ExecutorBlockStrategyEnum.COVER_EARLY
                    }
                )
            }

        val returnT: Response<String> = adminBiz.autoRegister(request)
        assertTrue(returnT.isSuccess)
        val requestBody = lastRequestBody.get()
        assertTrue(requestBody.contains("\"scheduleType\":\"CRON\""))
        assertTrue(requestBody.contains("\"misfireStrategy\":\"FIRE_ONCE_NOW\""))
        assertTrue(requestBody.contains("\"executorRouteStrategy\":\"SHARDING_BROADCAST\""))
        assertTrue(requestBody.contains("\"executorBlockStrategy\":\"COVER_EARLY\""))
    }

    @Test
    @Throws(Exception::class)
    fun jobManage() {
        logger.info("jobAdd、jobUpdate、jobRemove、jobStart、jobStop")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AdminBizTest::class.java)
        private const val accessToken = "default_token"
        private lateinit var server: HttpServer
        private lateinit var addressUrl: String
        private val lastRequestBody = AtomicReference("")

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = HttpServer.create(InetSocketAddress(0), 0).apply {
                createContext("/task-pilot-admin/api/executor/callback") { exchange ->
                    replyWithSuccess(exchange)
                }
                createContext("/task-pilot-admin/api/executor/registry") { exchange ->
                    replyWithSuccess(exchange)
                }
                createContext("/task-pilot-admin/api/executor/registryRemove") { exchange ->
                    replyWithSuccess(exchange)
                }
                createContext("/task-pilot-admin/api/executor/autoRegister") { exchange ->
                    replyWithSuccess(exchange)
                }
                start()
            }
            addressUrl = "http://127.0.0.1:${server.address.port}/task-pilot-admin"
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
        }

        /**
         * 统一记录请求体并返回标准成功响应，重点验证客户端路径与序列化行为。
         */
        private fun replyWithSuccess(exchange: HttpExchange) {
            lastRequestBody.set(exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8))
            val responseBody = GsonTool.toJson(Response.ofSuccess("ok"))
            exchange.sendResponseHeaders(200, responseBody.toByteArray(StandardCharsets.UTF_8).size.toLong())
            exchange.responseBody.use { it.write(responseBody.toByteArray(StandardCharsets.UTF_8)) }
        }
    }
}
