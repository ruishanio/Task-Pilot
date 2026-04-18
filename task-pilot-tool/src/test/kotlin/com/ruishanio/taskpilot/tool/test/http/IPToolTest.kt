package com.ruishanio.taskpilot.tool.test.http

import com.ruishanio.taskpilot.tool.http.IPTool
import java.net.InetSocketAddress
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * 仅覆盖 IPTool 的公开入口，保证 Kotlin 迁移后静态方法仍可直接调用。
 */
class IPToolTest {
    @Test
    fun isPortInUsed() {
        val port = 8080
        logger.info("port = {}, result = {}", port, IPTool.isPortInUsed(port))
    }

    @Test
    fun isInvalidPort() {
        val port = 8080
        logger.info("port = {}, result = {}", port, IPTool.isValidPort(port))
    }

    @Test
    fun getRandomPort() {
        logger.info("result = {}", IPTool.getRandomPort())
    }

    @Test
    fun getAvailablePort() {
        logger.info("result = {}", IPTool.getAvailablePort())
    }

    @Test
    fun getAvailablePort2() {
        logger.info("result = {}", IPTool.getAvailablePort(8080))
    }

    @Test
    fun isLocalHost() {
        logger.info("result = {}", IPTool.isLocalHost("127.0.0.3"))
    }

    @Test
    fun isAnyHost() {
        logger.info("result = {}", IPTool.isAnyHost("127.0.0.3"))
    }

    @Test
    fun isValidLocalHost() {
        val host = "127.0.0.3"
        logger.info("host = {}, result = {}", host, IPTool.isValidLocalHost(host))
    }

    @Test
    fun toSocketAddressString() {
        logger.info("result = {}", IPTool.toAddressString(InetSocketAddress("localhost", 8089)))
    }

    @Test
    fun toSocketAddressString2() {
        logger.info("result = {}", IPTool.toAddressString("localhost", 8089))
    }

    @Test
    fun toSocketAddress() {
        logger.info("result = {}", IPTool.toSocketAddress("127.0.0.3:9999"))
    }

    @Test
    fun toSocketAddress2() {
        logger.info("result = {}", IPTool.toSocketAddress("127.0.0.3", 9999))
    }

    @Test
    fun isValidV4Address() {
        logger.info("result = {}", IPTool.isValidV4Address("127.0.0.3:9999"))
    }

    @Test
    fun isValidV4Address2() {
        logger.info(
            "result = {}",
            IPTool.isValidV4Address(InetSocketAddress("127.0.0.3", 9999).address)
        )
    }

    @Test
    fun getIp() {
        logger.info("result = {}", IPTool.getIp())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IPToolTest::class.java)
    }
}
