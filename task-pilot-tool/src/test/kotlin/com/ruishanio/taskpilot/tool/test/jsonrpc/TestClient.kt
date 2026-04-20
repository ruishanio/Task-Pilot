package com.ruishanio.taskpilot.tool.test.jsonrpc

import com.ruishanio.taskpilot.tool.jsonrpc.JsonRpcClient
import com.ruishanio.taskpilot.tool.jsonrpc.JsonRpcServer
import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.UserService
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.impl.UserServiceImpl
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.ResultDTO
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.UserDTO
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.OutputStream
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * JsonRpc 客户端代理与手工调用验证。
 *
 * 测试内自建本地 HttpServer，避免依赖开发者机器上额外启动 8080 服务。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestClient {
    private val service = "userService"
    private lateinit var url: String
    private lateinit var server: HttpServer

    /**
     * 服务端逻辑直接复用测试实现，确保代理调用和手工调用走的是同一份协议分发代码。
     */
    private val jsonRpcServer =
        JsonRpcServer
            .newServer()
            .register(service, UserServiceImpl())

    @BeforeAll
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/jsonrpc") { exchange ->
            if (!exchange.requestMethod.equals("POST", true)) {
                writeResponse(exchange, 405, "not support method!")
                return@createContext
            }

            val requestBody =
                exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    reader.readText()
                }
            val responseBody = jsonRpcServer.invoke(requestBody)
            writeResponse(exchange, 200, responseBody)
        }
        server.start()
        url = "http://127.0.0.1:${server.address.port}/jsonrpc"
    }

    @AfterAll
    fun stopServer() {
        server.stop(0)
    }

    private fun buildClient(): JsonRpcClient =
        JsonRpcClient
            .newClient()
            .url(url)
            .timeout(3 * 1000)

    @Test
    fun loadUserTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.loadUser("zhangsan")
        assertNotNull(result)
        assertEquals("zhangsan(success)", result?.name)

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                service,
                "loadUser",
                arrayOf("zhangsan"),
                UserDTO::class.java,
            )
        assertNotNull(result2)
        assertEquals("zhangsan(success)", result2?.name)
    }

    @Test
    fun queryUserByAgeTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.queryUserByAge()
        assertEquals(3, result?.size)
        assertTrue(result.orEmpty().all { it.name?.startsWith("user(success)") == true })

        val jsonRpcClient = buildClient()
        @Suppress("UNCHECKED_CAST")
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "queryUserByAge",
                null,
                List::class.java,
                arrayOf<Type>(UserDTO::class.java),
            ) as List<UserDTO>?
        assertEquals(3, result2?.size)
        assertTrue(result2.orEmpty().all { it.name?.startsWith("user(success)") == true })
    }

    @Test
    fun createUserTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.createUser(UserDTO("jack", 28))
        assertNotNull(result)
        assertTrue(result?.success == true)
        assertEquals("createUser success", result?.message)

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "createUser",
                arrayOf(UserDTO("jack", 28)),
                ResultDTO::class.java,
            )
        assertNotNull(result2)
        assertTrue(result2?.success == true)
        assertEquals("createUser success", result2?.message)
    }

    @Test
    fun updateUserTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.updateUser("jack", 28)
        assertNotNull(result)
        assertTrue(result?.success == true)
        assertEquals("updateUser success", result?.message)

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "updateUser",
                arrayOf("jack", 28),
                ResultDTO::class.java,
            )
        assertNotNull(result2)
        assertTrue(result2?.success == true)
        assertEquals("updateUser success", result2?.message)
    }

    @Test
    fun refreshTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        assertDoesNotThrow { userService.refresh() }

        val jsonRpcClient = buildClient()
        val result2: Any? =
            jsonRpcClient.invoke(
                "userService",
                "refresh",
                null,
                null as Class<Any>?,
            )
        assertNull(result2)
    }

    @Test
    fun loadTest() {
        val userService = buildClient().header("token", "12345678").proxy(service, UserService::class.java)

        val result: Response<UserDTO>? = userService.load("jack")
        assertNotNull(result)
        assertTrue(result?.isSuccess == true)
        assertEquals("jack(success)", result?.data?.name)

        val jsonRpcClient = buildClient()
        @Suppress("UNCHECKED_CAST")
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "load",
                arrayOf("jack"),
                Response::class.java,
                arrayOf<Type>(UserDTO::class.java),
            ) as Response<UserDTO>?
        assertNotNull(result2)
        assertTrue(result2?.isSuccess == true)
        assertEquals("jack(success)", result2?.data?.name)
    }

    @Test
    fun repeatedLoadSmokeTest() {
        val userService = buildClient().header("token", "12345678").proxy(service, UserService::class.java)

        // 历史压测用例不适合自动化回归，这里收敛成小批量烟雾测试验证代理调用稳定性。
        repeat(20) { index ->
            val response = userService.load("jack-$index")
            assertNotNull(response)
            assertTrue(response?.isSuccess == true)
            assertEquals("jack-$index(success)", response?.data?.name)
        }
    }

    /**
     * 统一按 UTF-8 写回，避免本地默认编码导致 JSON-RPC 响应体被错误解析。
     */
    private fun writeResponse(exchange: HttpExchange, statusCode: Int, body: String) {
        val responseBytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders["Content-Type"] = listOf("application/json; charset=UTF-8")
        exchange.sendResponseHeaders(statusCode, responseBytes.size.toLong())
        val outputStream: OutputStream = exchange.responseBody
        outputStream.write(responseBytes)
        outputStream.close()
    }
}
