@file:Suppress("DEPRECATION")

package com.ruishanio.taskpilot.core.server

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.impl.ExecutorBizImpl
import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.core.thread.ExecutorRegistryThread
import com.ruishanio.taskpilot.tool.error.ThrowableTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 执行器嵌入式服务。
 */
class EmbedServer {
    private var executorBiz: ExecutorBiz? = null
    private var thread: Thread? = null

    fun start(address: String?, port: Int, appname: String?, accessToken: String?) {
        executorBiz = ExecutorBizImpl()
        thread =
            Thread {
                val bossGroup = NioEventLoopGroup()
                val workerGroup = NioEventLoopGroup()
                val bizThreadPool =
                    ThreadPoolExecutor(
                        0,
                        200,
                        60L,
                        TimeUnit.SECONDS,
                        LinkedBlockingQueue(2000),
                        ThreadFactory { runnable ->
                            Thread(runnable, "task-pilot, EmbedServer bizThreadPool-${runnable.hashCode()}")
                        },
                        RejectedExecutionHandler { _, _ ->
                            throw RuntimeException("task-pilot, EmbedServer bizThreadPool is EXHAUSTED!")
                        }
                    )
                try {
                    val bootstrap = ServerBootstrap()
                    bootstrap
                        .group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel::class.java)
                        .childHandler(
                            object : ChannelInitializer<SocketChannel>() {
                                override fun initChannel(channel: SocketChannel) {
                                    channel.pipeline()
                                        .addLast(IdleStateHandler(0, 0, 30 * 3L, TimeUnit.SECONDS))
                                        .addLast(HttpServerCodec())
                                        .addLast(HttpObjectAggregator(5 * 1024 * 1024))
                                        .addLast(EmbedHttpServerHandler(executorBiz!!, accessToken, bizThreadPool))
                                }
                            }
                        ).childOption(ChannelOption.SO_KEEPALIVE, true)

                    val future: ChannelFuture = bootstrap.bind(port).sync()
                    logger.info(">>>>>>>>>>> task-pilot 远程服务启动成功，nettype = {}, port = {}", EmbedServer::class.java, port)

                    startRegistry(appname, address)
                    future.channel().closeFuture().sync()
                } catch (_: InterruptedException) {
                    logger.info(">>>>>>>>>>> task-pilot 远程服务已停止。")
                } catch (e: Throwable) {
                    logger.error(">>>>>>>>>>> task-pilot 远程服务运行异常。", e)
                } finally {
                    try {
                        workerGroup.shutdownGracefully()
                        bossGroup.shutdownGracefully()
                    } catch (e: Throwable) {
                        logger.error(">>>>>>>>>>> task-pilot 关闭远程服务资源时发生异常。", e)
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
    }

    @Throws(Exception::class)
    fun stop() {
        if (thread != null && thread!!.isAlive) {
            thread!!.interrupt()
        }
        stopRegistry()
        logger.info(">>>>>>>>>>> task-pilot 远程服务销毁完成。")
    }

    /**
     * Netty HTTP 请求处理器。
     */
    class EmbedHttpServerHandler(
        private val executorBiz: ExecutorBiz,
        private val accessToken: String?,
        private val bizThreadPool: ThreadPoolExecutor
    ) : SimpleChannelInboundHandler<FullHttpRequest>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
            val requestData = msg.content().toString(CharsetUtil.UTF_8)
            val uri = msg.uri()
            val httpMethod = msg.method()
            val keepAlive = HttpUtil.isKeepAlive(msg)
            val accessTokenReq = msg.headers().get(Const.TASK_PILOT_ACCESS_TOKEN)

            bizThreadPool.execute {
                val responseObj = dispatchRequest(httpMethod, uri, requestData, accessTokenReq)
                val responseJson = GsonTool.toJson(responseObj)
                writeResponse(ctx, keepAlive, responseJson)
            }
        }

        private fun dispatchRequest(
            httpMethod: HttpMethod,
            uri: String?,
            requestData: String,
            accessTokenReq: String?
        ): Any {
            val normalizedUri = normalizeUri(uri)
            if (HttpMethod.POST != httpMethod) {
                return Response.ofFail<String>("invalid request, HttpMethod not support.")
            }
            if (normalizedUri.isNullOrBlank()) {
                return Response.ofFail<String>("invalid request, uri-mapping empty.")
            }
            if (!accessToken.isNullOrBlank() && accessToken != accessTokenReq) {
                return Response.ofFail<String>("The access token is wrong.")
            }

            return try {
                when (normalizedUri) {
                    "/beat" -> executorBiz.beat()
                    "/idleBeat" ->
                        executorBiz.idleBeat(GsonTool.fromJson(requestData, IdleBeatRequest::class.java))
                    "/run" ->
                        executorBiz.run(GsonTool.fromJson(requestData, TriggerRequest::class.java))
                    "/kill" ->
                        executorBiz.kill(GsonTool.fromJson(requestData, KillRequest::class.java))
                    "/log" ->
                        executorBiz.log(GsonTool.fromJson(requestData, LogRequest::class.java))
                    else -> Response.ofFail<String>("invalid request, uri-mapping($normalizedUri) not found.")
                }
            } catch (e: Throwable) {
                logger.error(">>>>>>>>>>> task-pilot 处理远程请求时发生异常。", e)
                Response.ofFail<String>("request error:" + ThrowableTool.toString(e))
            }
        }

        /**
         * Netty `uri()` 可能携带查询串，这里先做标准化，避免协议匹配被 `?` 后缀干扰。
         */
        private fun normalizeUri(uri: String?): String? = uri?.substringBefore("?")

        /**
         * 回写 HTTP 响应。
         */
        private fun writeResponse(ctx: ChannelHandlerContext, keepAlive: Boolean, responseJson: String) {
            val response: FullHttpResponse =
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8)
                )
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8")
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            }
            ctx.writeAndFlush(response)
        }

        override fun channelReadComplete(ctx: ChannelHandlerContext) {
            ctx.flush()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            logger.error(">>>>>>>>>>> task-pilot provider netty_http 服务捕获到异常。", cause)
            ctx.close()
        }

        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
            if (evt is IdleStateEvent) {
                ctx.channel().close()
                logger.debug(">>>>>>>>>>> task-pilot provider netty_http 服务关闭空闲连接。")
            } else {
                super.userEventTriggered(ctx, evt)
            }
        }

        companion object {
            private val logger = LoggerFactory.getLogger(EmbedHttpServerHandler::class.java)
        }
    }

    fun startRegistry(appname: String?, address: String?) {
        ExecutorRegistryThread.getInstance().start(appname, address)
    }

    fun stopRegistry() {
        ExecutorRegistryThread.getInstance().toStop()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmbedServer::class.java)
    }
}
