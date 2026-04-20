package com.ruishanio.taskpilot.executor.jobhandler

import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilotRegister
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.http.http.enums.ContentType
import com.ruishanio.taskpilot.tool.http.http.enums.Method
import com.ruishanio.taskpilot.tool.json.GsonTool
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * TaskPilot 开发示例（Bean 模式）。
 *
 * 开发步骤：
 * 1、任务开发：在 Spring Bean 实例中开发 Job 方法；
 * 2、注解配置：为 Job 方法添加 @TaskPilot 注解，value 对应调度中心中任务的 JobHandler；
 * 3、执行日志：通过 TaskPilotHelper.log 打印执行日志；
 * 4、任务结果：默认任务结果为成功；如需失败，可调用 TaskPilotHelper.handleFail/handleSuccess。
 */
@Component
class SampleTaskPilotJob {
    /**
     * 1、简单任务示例（Bean 模式）。
     */
    @TaskPilot("demoJobHandler")
    fun demoJobHandler() {
        TaskPilotHelper.log("TASK-PILOT, Hello World.")
        repeat(5) {
            TaskPilotHelper.log("beat at:$it")
            TimeUnit.SECONDS.sleep(2)
        }
    }

    /**
     * 2、分片广播任务。
     */
    @TaskPilotRegister(taskDesc = "分片广播任务", conf = "0/3 * * * * ?")
    @TaskPilot("shardingJobHandler")
    fun shardingJobHandler() {
        val shardIndex = TaskPilotHelper.getShardIndex()
        val shardTotal = TaskPilotHelper.getShardTotal()
        TaskPilotHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal)

        repeat(shardTotal) { index ->
            if (index == shardIndex) {
                TaskPilotHelper.log("第 {} 片, 命中分片开始处理", index)
            } else {
                TaskPilotHelper.log("第 {} 片, 忽略", index)
            }
        }
    }

    /**
     * 3、命令行任务。
     *
     * 参数示例："ls -a" 或者 "pwd"。
     */
    @TaskPilot("commandJobHandler")
    fun commandJobHandler() {
        val command = TaskPilotHelper.getJobParam()
        if (command.isNullOrBlank()) {
            TaskPilotHelper.handleFail("command empty.")
            return
        }

        var exitValue = -1
        try {
            val process = ProcessBuilder()
                .command(command.split(" "))
                .redirectErrorStream(true)
                .start()

            BufferedReader(InputStreamReader(BufferedInputStream(process.inputStream))).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    TaskPilotHelper.log(line)
                }
            }

            process.waitFor()
            exitValue = process.exitValue()
        } catch (e: Exception) {
            TaskPilotHelper.log(e)
        }

        if (exitValue != 0) {
            TaskPilotHelper.handleFail("command exit value($exitValue) is failed")
        }
    }

    /**
     * 4、跨平台 Http 任务。
     *
     * 参数示例：
     * <pre>
     * {
     *     "url": "http://www.baidu.com",
     *     "method": "POST",
     *     "contentType": "application/json"
     * }
     * </pre>
     */
    @TaskPilot("httpJobHandler")
    fun httpJobHandler() {
        val param = TaskPilotHelper.getJobParam()
        if (param.isNullOrBlank()) {
            TaskPilotHelper.log("param[$param] invalid.")
            TaskPilotHelper.handleFail()
            return
        }

        val httpJobParam: HttpJobParam? = try {
            GsonTool.fromJson(param, HttpJobParam::class.java)
        } catch (e: Exception) {
            TaskPilotHelper.log(RuntimeException("HttpJobParam parse error", e))
            TaskPilotHelper.handleFail()
            return
        }

        if (httpJobParam == null) {
            TaskPilotHelper.log("param parse fail.")
            TaskPilotHelper.handleFail()
            return
        }
        if (StringTool.isBlank(httpJobParam.url)) {
            TaskPilotHelper.log("url[${httpJobParam.url}] invalid.")
            TaskPilotHelper.handleFail()
            return
        }
        if (!isValidDomain(httpJobParam.url)) {
            TaskPilotHelper.log("url[${httpJobParam.url}] not allowed.")
            TaskPilotHelper.handleFail()
            return
        }

        val method = resolveMethod(httpJobParam.method)
        if (method == null) {
            TaskPilotHelper.log("method[${httpJobParam.method}] invalid.")
            TaskPilotHelper.handleFail()
            return
        }

        val contentType = resolveContentType(httpJobParam.contentType)
        if (httpJobParam.timeout <= 0) {
            httpJobParam.timeout = 3000
        }

        try {
            val httpResponse = HttpTool.createRequest()
                .url(httpJobParam.url)
                .method(method)
                .contentType(contentType)
                .header(httpJobParam.headers)
                .cookie(httpJobParam.cookies)
                .body(httpJobParam.data)
                .form(httpJobParam.form)
                .auth(httpJobParam.auth)
                .execute()

            TaskPilotHelper.log("StatusCode: ${httpResponse.statusCode()}")
            TaskPilotHelper.log("Response: <br>${httpResponse.response()}")
        } catch (e: Exception) {
            TaskPilotHelper.log(e)
            TaskPilotHelper.handleFail()
        }
    }

    /**
     * 仅允许白名单域名，避免示例任务被直接用于任意地址探测。
     */
    private fun isValidDomain(url: String?): Boolean {
        if (url == null || DOMAIN_WHITE_LIST.isEmpty()) {
            return false
        }
        return DOMAIN_WHITE_LIST.any(url::startsWith)
    }

    /**
     * 将字符串方法名收敛为框架支持的枚举，非法值直接返回 null 交给上层失败处理。
     */
    private fun resolveMethod(method: String?): Method? {
        if (StringTool.isBlank(method)) {
            return Method.POST
        }
        return try {
            Method.valueOf(method!!.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Content-Type 使用白名单匹配；未命中时回退为 JSON，保持与原示例一致。
     */
    private fun resolveContentType(contentType: String?): ContentType =
        ContentType.values().firstOrNull { it.value == contentType } ?: ContentType.JSON

    /**
     * 5、生命周期任务示例：任务初始化与销毁时，支持自定义相关逻辑。
     */
    @TaskPilot(value = "demoJobHandler2", init = "init", destroy = "destroy")
    fun demoJobHandler2() {
        TaskPilotHelper.log("TASK-PILOT, Hello World.")
    }

    fun init() {
        logger.info("init")
    }

    fun destroy() {
        logger.info("destroy")
    }

    /**
     * http job param。
     */
    private class HttpJobParam {
        var url: String? = null
        var method: String? = null
        var contentType: String? = null
        var headers: Map<String, String>? = null
        var cookies: Map<String, String>? = null
        var timeout: Int = 0
        var data: String? = null
        var form: Map<String, String>? = null
        var auth: String? = null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SampleTaskPilotJob::class.java)

        private val DOMAIN_WHITE_LIST = setOf(
            "http://www.baidu.com",
            "http://cn.bing.com"
        )
    }
}
