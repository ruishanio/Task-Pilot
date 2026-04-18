package com.ruishanio.taskpilot.tool.test.jsonrpc

import com.ruishanio.taskpilot.tool.jsonrpc.JsonRpcClient
import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.UserService
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.ResultDTO
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.UserDTO
import org.junit.jupiter.api.Test
import java.lang.reflect.Type

/**
 * JsonRpc 客户端代理与手工调用验证。
 */
class TestClient {
    private val url = "http://localhost:8080/jsonrpc"
    private val service = "userService"

    private fun buildClient(): JsonRpcClient =
        JsonRpcClient
            .newClient()
            .url(url)
            .timeout(3 * 1000)

    @Test
    fun loadUserTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.loadUser("zhangsan")
        println("proxy invoke, result = $result")

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                service,
                "loadUser",
                arrayOf("zhangsan"),
                UserDTO::class.java,
            )
        println("client invoke, result2 = $result2")
    }

    @Test
    fun queryUserByAgeTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.queryUserByAge()
        println("proxy invoke, result = $result")

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "queryUserByAge",
                null,
                List::class.java,
            )
        println("client invoke, result2 = $result2")
    }

    @Test
    fun createUserTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.createUser(UserDTO("jack", 28))
        println("proxy result = $result")

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "createUser",
                arrayOf(UserDTO("jack", 28)),
                ResultDTO::class.java,
            )

        println("client invoke, result2 = $result2")
    }

    @Test
    fun updateUserTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        val result = userService.updateUser("jack", 28)
        println("proxy result = $result")

        val jsonRpcClient = buildClient()
        val result2 =
            jsonRpcClient.invoke(
                "userService",
                "updateUser",
                arrayOf("jack", 28),
                ResultDTO::class.java,
            )
        println("client invoke, result2 = $result2")
    }

    @Test
    fun refreshTest() {
        val userService = buildClient().proxy(service, UserService::class.java)

        userService.refresh()
        println("proxy refresh")

        val jsonRpcClient = buildClient()
        val result2: Any? =
            jsonRpcClient.invoke(
                "userService",
                "refresh",
                null,
                null as Class<Any>?,
            )
        println("client invoke, result2 = $result2")
    }

    @Test
    fun loadTest() {
        val userService = buildClient().header("token", "12345678").proxy(service, UserService::class.java)

        val result: Response<UserDTO>? = userService.load("jack")
        println("proxy result = $result")

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
        println("client invoke, result2 = $result2")
    }

    @Test
    fun test1() {
        val userService = buildClient().header("token", "12345678").proxy(service, UserService::class.java)

        val start = System.currentTimeMillis()
        for (index in 0 until (10000L * 10000L)) {
            if (index % 10000L == 0L) {
                println("$index= ${userService.load("jack")}")
            }
        }
        val cost = System.currentTimeMillis() - start
        println("cost = $cost")
    }
}
