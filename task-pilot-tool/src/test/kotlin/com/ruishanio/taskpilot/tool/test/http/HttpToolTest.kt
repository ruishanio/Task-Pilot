package com.ruishanio.taskpilot.tool.test.http

import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.http.client.HttpClientMethod
import com.ruishanio.taskpilot.tool.http.client.HttpClientService
import com.ruishanio.taskpilot.tool.http.http.HttpRequest
import com.ruishanio.taskpilot.tool.http.http.HttpResponse
import com.ruishanio.taskpilot.tool.http.http.enums.ContentType
import com.ruishanio.taskpilot.tool.http.http.enums.Method
import com.ruishanio.taskpilot.tool.http.http.iface.HttpInterceptor
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URL

/**
 * 覆盖 HttpTool 的构建式请求、代理客户端和响应反序列化入口。
 * 这组测试大多依赖外网，仅保证 Kotlin 迁移后 API 形态与原来一致。
 */
class HttpToolTest {
    @Test
    fun test01() {
        val httpResponse =
            HttpTool.createGet("https://news.baidu.com/widget")
                .form("ajax", "json")
                .form("id", "ad")
                .execute()

        logger.info("statusCode: {}", httpResponse.statusCode())
        logger.info("response: {}", httpResponse.response())
    }

    @Test
    fun test011() {
        logger.info("response 01: \n{}", HttpTool.createGet("https://www.baidu.com").execute().response())
        logger.info("response 02: \n{}", HttpTool.createGet("https://www.baidu.com").headerDefault().execute().response())
    }

    @Test
    fun test02() {
        val httpResponse = HttpTool.createPost("https://news.baidu.com/widget?ajax=json&id=ad").execute()
        logger.info("statusCode: {}", httpResponse.statusCode())
        logger.info("response: {}", httpResponse.response())
    }

    @Test
    fun test03() {
        val httpRequest =
            HttpTool.createRequest()
                .url("https://news.baidu.com/widget?ajax=json&id=ad")
                .method(Method.GET)
                .contentType(ContentType.JSON)
                .header("header", "value")
                .cookie("cookie", "value")
                .connectTimeout(10000)
                .readTimeout(10000)
                .useCaches(false)
                .body("body")
                .form("form", "value")
                .auth("auth999")
                .interceptor(
                    object : HttpInterceptor {
                        override fun before(httpRequest: HttpRequest) {
                            logger.info("before, url = {}", httpRequest.getUrl())
                        }

                        override fun after(httpRequest: HttpRequest, httpResponse: HttpResponse) {
                            logger.info("after, response = {}", httpResponse.response())
                        }
                    }
                )

        val httpResponse = httpRequest.execute()
        logger.info("statusCode: {}", httpResponse.statusCode())
        logger.info("response: {}", httpResponse.response())
    }

    @Test
    fun test04() {
        val result1 =
            HttpTool.createGet("https://news.baidu.com/widget?ajax=json&id=ad")
                .interceptor(
                    object : HttpInterceptor {
                        override fun before(httpRequest: HttpRequest) {
                            logger.info("before, url = {}", httpRequest.getUrl())
                        }

                        override fun after(httpRequest: HttpRequest, httpResponse: HttpResponse) {
                            logger.info("after, response = {}", httpResponse.response())
                        }
                    }
                )
                .execute()
                .response(RespDTO::class.java)
        logger.info("result1: {}", result1)

        val result2 =
            HttpTool.createGet("https://news.baidu.com/widget?ajax=json&id=ad")
                .interceptor(
                    object : HttpInterceptor {
                        override fun before(httpRequest: HttpRequest) {
                            logger.info("before, url = {}", httpRequest.getUrl())
                        }

                        override fun after(httpRequest: HttpRequest, httpResponse: HttpResponse) {
                            logger.info("after, response = {}", httpResponse.response())
                        }
                    }
                )
                .execute()
                .response(null as Class<RespDTO>?)
        logger.info("result2: {}", result2)
    }

    @Test
    fun test05() {
        val httpResponse =
            HttpTool.createGet("https://news.baidu.com/widget?ajax=json&id=ad")
                .cookie("cookie1", "value1")
                .execute()

        logger.info("result2: {}", httpResponse)
        logger.info("cookie: {}", httpResponse.cookies())
        logger.info("cookie3: {}", httpResponse.cookie("cookie3"))
        logger.info("cookie1: {}", httpResponse.cookie("cookie1"))
    }

    @Test
    fun test06() {
        val result =
            HttpTool.createPost("https://news.baidu.com/widget?ajax=json&id=ad")
                .execute()
                .response()

        logger.info("result2: {}", result)
    }

    @Test
    fun test07() {
        val result =
            HttpTool.createGet("https://news.baidu.com/widget?ajax=json&id=ad")
                .execute()
                .response(RespDTO::class.java)

        logger.info("result2: {}", result)
    }

    @Test
    fun test08() {
        val result =
            HttpTool.createPost("https://news.baidu.com/widget?ajax=json&id=ad")
                .request(RespDTO("jack", 18))
                .execute()
                .response(RespDTO::class.java)

        logger.info("result2: {}", result)
    }

    @Test
    fun test09() {
        val json = GsonTool.toJson(Response.ofSuccess(RespDTO("jack", 18)))

        // 继续通过手工构造响应对象覆盖泛型反序列化分支。
        val httpResponse =
            HttpResponse().apply {
                statusCode = 200
                response = json
            }

        @Suppress("UNCHECKED_CAST")
        val result = httpResponse.response(Response::class.java, RespDTO::class.java) as Response<RespDTO>?
        @Suppress("UNCHECKED_CAST")
        val result2 = httpResponse.response(Response::class.java) as Response<RespDTO>?

        logger.info("statusCode: {}", httpResponse.statusCode())
        logger.info("response: {}", httpResponse.response())
        logger.info("result: {}", result)
        logger.info("result2: {}", result2)
    }

    @Test
    fun test10() {
        val demoService =
            HttpTool.createClient()
                .url("https://news.baidu.com/widget?ajax=json&id=ad")
                .proxy(DemoService::class.java)
        val result = demoService.widget()
        logger.info("result2: {}", result)
    }

    @Test
    fun test11() {
        val demoService =
            HttpTool.createClient()
                .header("header1", "value1")
                .cookie("cookie1", "value1")
                .timeout(10000)
                .auth("auth999")
                .url("https://news.baidu.com/widget?ajax=json&id=ad")
                .proxy(DemoService::class.java)
        val result = demoService.widget()
        logger.info("result2: {}", result)
    }

    @Test
    fun test12() {
        val demoService = HttpTool.createClient().proxy(DemoService2::class.java)
        val result = demoService.widget()
        logger.info("result2: {}", result)

        demoService.widget22()
    }

    @Test
    fun test13() {
        println("isHttps : ${HttpTool.isHttps("https://news.baidu.com/widget?ajax=json&id=ad")}")
        println("isHttps : ${HttpTool.isHttp("https://news.baidu.com/widget?ajax=json&id=ad")}")
    }

    @Test
    fun test14() {
        println("parseUrlParam : ${HttpTool.parseUrlParam("https://news.baidu.com/widget?ajax=json&id=ad")}")
        println("generateUrlParam : ${HttpTool.generateUrlParam(MapTool.newMap("k1", "v1", "k2", "v2"))}")
    }

    @Test
    fun test15() {
        val createRequestUrlMethod = HttpRequest::class.java.getDeclaredMethod("createRequestUrl", String::class.java)
        createRequestUrlMethod.isAccessible = true

        val rawUrl = "https://example.com/search?q=a b&name=task pilot"
        val parsedUrl = createRequestUrlMethod.invoke(HttpRequest(), rawUrl) as URL

        Assertions.assertEquals(rawUrl, parsedUrl.toExternalForm())
    }

    /**
     * 保留与历史 JSON 字段完全相同的命名，避免测试无意中把 snake_case 改成 camelCase。
     */
    class RespDTO(
        var request_id: String? = null,
        var timestamp: Long = 0
    ) {
        override fun toString(): String = "Resp{request_id='$request_id', timestamp=$timestamp}"
    }

    interface DemoService {
        fun widget(): RespDTO?
    }

    /**
     * 注解位点保持在接口和函数上，确保代理解析还能按旧规则读取声明信息。
     */
    @HttpClientService(url = "https://news.baidu.com/widget?ajax=json&id=ad")
    interface DemoService2 {
        @HttpClientMethod(timeout = 10000)
        fun widget(): RespDTO?

        fun widget22()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpToolTest::class.java)
    }
}
