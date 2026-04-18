package com.ruishanio.taskpilot.openapi

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.response.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Executor 开放接口客户端测试。
 * 依赖本地 executor 服务，仅用于验证代理接口和请求模型形态。
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
        assertNull(retval.data)
        assertEquals(200, retval.code)
        assertNull(retval.msg)
    }

    @Test
    fun idleBeat() {
        val executorBiz = buildClient()
        val retval: Response<String> = executorBiz.idleBeat(IdleBeatRequest(0))

        assertNotNull(retval)
        assertNull(retval.data)
        assertEquals(500, retval.code)
        assertEquals("job thread is running or has trigger queue.", retval.msg)
    }

    @Test
    fun run() {
        val executorBiz = buildClient()
        val triggerParam =
            TriggerRequest().apply {
                jobId = 1
                executorHandler = "demoJobHandler"
                executorParams = null
                executorBlockStrategy = ExecutorBlockStrategyEnum.COVER_EARLY.name
                glueType = GlueTypeEnum.BEAN.name
                glueSource = null
                glueUpdatetime = System.currentTimeMillis()
                logId = 1
                logDateTime = System.currentTimeMillis()
            }

        val retval: Response<String> = executorBiz.run(triggerParam)
        assertNotNull(retval)
    }

    @Test
    fun kill() {
        val executorBiz = buildClient()
        val retval: Response<String> = executorBiz.kill(KillRequest(0))

        assertNotNull(retval)
        assertNull(retval.data)
        assertEquals(200, retval.code)
        assertNull(retval.msg)
    }

    @Test
    fun log() {
        val executorBiz = buildClient()
        val retval: Response<LogResult> = executorBiz.log(LogRequest(0L, 0L, 0))
        assertNotNull(retval)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExecutorBizTest::class.java)
        private const val addressUrl = "http://127.0.0.1:9999/"
        private const val accessToken = "default_token"
    }
}
