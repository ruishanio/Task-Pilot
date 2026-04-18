package com.ruishanio.taskpilot.tool.test.jsonrpc

import com.ruishanio.taskpilot.tool.io.IOTool
import com.ruishanio.taskpilot.tool.jsonrpc.JsonRpcServer
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.impl.UserServiceImpl
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * JsonRpc 本地测试服务端。
 */
class TestServer

private val jsonRpcServer =
    JsonRpcServer
        .newServer()
        .register("userService", UserServiceImpl())

fun main(args: Array<String>) {
    val server = HttpServer.create(InetSocketAddress(8080), 0)

    server.createContext("/jsonrpc") { httpExchange ->
        if (!(httpExchange.requestMethod.equals("POST", true) && httpExchange.requestURI.path == "/jsonrpc")) {
            writeResponse(httpExchange, "not support method!")
            return@createContext
        }

        val requestBody = IOTool.readString(httpExchange.requestBody, Charset.defaultCharset())
        println("\n\n requestBody = $requestBody")

        val jsonRpcResponse = jsonRpcServer.invoke(requestBody)
        println("jsonRpcResponse = $jsonRpcResponse")

        writeResponse(httpExchange, jsonRpcResponse)
    }

    server.createContext("/") { httpExchange -> writeResponse(httpExchange, "Hello World.") }
    server.start()
    println("Server is running on port 8080")
}

/**
 * 将响应按 UTF-8 写回 http client。
 */
private fun writeResponse(httpExchange: HttpExchange, response: String) {
    val responseBytes = response.toByteArray(StandardCharsets.UTF_8)
    httpExchange.responseHeaders["Content-Type"] = listOf("application/json; charset=UTF-8")
    httpExchange.sendResponseHeaders(200, responseBytes.size.toLong())
    val outputStream: OutputStream = httpExchange.responseBody
    outputStream.write(responseBytes)
    outputStream.close()
}
