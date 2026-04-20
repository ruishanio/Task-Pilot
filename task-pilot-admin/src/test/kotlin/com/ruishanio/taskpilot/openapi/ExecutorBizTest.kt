package com.ruishanio.taskpilot.openapi

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.response.Response
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Executor 开放接口客户端测试。
 *
 * 使用本地轻量 HTTP 假服务验证执行器客户端协议，避免测试依赖真实 executor 进程。
 */
class ExecutorBizTest {
    private fun buildClient(): ExecutorBiz =
        HttpTool.createClient()
            .url(addressUrl)
            .timeout(3 * 1000)
            .header(Const.TASK_PILOT_ACCESS_TOKEN, accessToken)
            .proxy(ExecutorBiz::class.java)

    @Test
    @Throws(Exception::class)
    fun beat() {
        val executorBiz = buildClient()
        val retval: Response<String> = executorBiz.beat()

        assertNotNull(retval)
        assertEquals("ok", retval.data)
        assertEquals(200, retval.code)
        assertNull(retval.msg)
    }

    @Test
    fun idle() {
        val executorBiz = buildClient()
        val retval: Response<String> = executorBiz.idle(IdleBeatRequest(0))

        assertNotNull(retval)
        assertEquals("busy", retval.data)
        assertEquals(200, retval.code)
        assertNull(retval.msg)
    }

    @Test
    fun run() {
        val executorBiz = buildClient()
        val triggerParam =
            TriggerRequest().apply {
                taskId = 1
                executorHandler = "demoTaskHandler"
                executorParam = null
                executorBlockStrategy = ExecutorBlockStrategyEnum.COVER_EARLY
                glueType = GlueTypeEnum.BEAN.name
                glueSource = null
                glueUpdateTime = System.currentTimeMillis()
                logId = 1
                logDateTime = System.currentTimeMillis()
            }

        val retval: Response<String> = executorBiz.run(triggerParam)
        assertNotNull(retval)
        assertTrue(lastRequestBody.get().contains("\"executorBlockStrategy\":\"COVER_EARLY\""))
    }

    @Test
    fun kill() {
        val executorBiz = buildClient()
        val retval: Response<String> = executorBiz.kill(KillRequest(0))

        assertNotNull(retval)
        assertEquals("killed", retval.data)
        assertEquals(200, retval.code)
        assertNull(retval.msg)
    }

    @Test
    fun log() {
        val executorBiz = buildClient()
        val retval: Response<LogResult> = executorBiz.log(LogRequest(0L, 0L, 0))
        assertNotNull(retval)
        assertEquals(1, retval.data?.fromLineNum)
        assertEquals(3, retval.data?.toLineNum)
        assertEquals("demo log", retval.data?.logContent)
        assertEquals(true, retval.data?.isEnd)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExecutorBizTest::class.java)
        private const val accessToken = "default_token"
        private lateinit var server: HttpServer
        private lateinit var addressUrl: String
        private val lastRequestBody = AtomicReference("")

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = HttpServer.create(InetSocketAddress(0), 0).apply {
                createContext("/beat") { exchange ->
                    reply(exchange, Response(200, null, "ok"))
                }
                createContext("/idle") { exchange ->
                    reply(exchange, Response(200, null, "busy"))
                }
                createContext("/run") { exchange ->
                    reply(exchange, Response(200, null, "triggered"))
                }
                createContext("/kill") { exchange ->
                    reply(exchange, Response(200, null, "killed"))
                }
                createContext("/log") { exchange ->
                    reply(exchange, Response(200, null, LogResult(1, 3, "demo log", true)))
                }
                start()
            }
            addressUrl = "http://127.0.0.1:${server.address.port}/"
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
        }

        /**
         * 统一回放固定响应，并把请求体落到内存中供断言协议字段。
         */
        private fun reply(exchange: HttpExchange, response: Response<*>) {
            lastRequestBody.set(exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8))
            val responseBody = GsonTool.toJson(response)
            exchange.sendResponseHeaders(200, responseBody.toByteArray(StandardCharsets.UTF_8).size.toLong())
            exchange.responseBody.use { it.write(responseBody.toByteArray(StandardCharsets.UTF_8)) }
        }
    }
}
