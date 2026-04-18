package com.ruishanio.taskpilot.tool.http

import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.UnknownHostException
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

/**
 * IP 与端口工具。
 * 继续保留“先扫网卡，再回退到 localhost”的地址发现策略，不在迁移时引入更激进的探测逻辑。
 */
object IPTool {
    private val ipv4AddressPattern = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$")
    private val ipv4IpPattern = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$")
    private val localIpPattern = Pattern.compile("127(\\.\\d{1,3}){3}$")

    private const val LOCALHOST_KEY = "localhost"
    private const val LOCALHOST_VALUE = "127.0.0.1"
    private const val ANYHOST_VALUE = "0.0.0.0"
    private const val MIN_PORT = 1
    private const val MAX_PORT = 65535
    private const val RANDOM_PORT_START = 30000
    private const val RANDOM_PORT_RANGE = 10000

    @Volatile
    private var localAddress: InetAddress? = null

    /** 检查端口是否已被系统占用。 */
    fun isPortInUsed(port: Int): Boolean {
        try {
            ServerSocket(port).use { return false }
        } catch (_: IOException) {
        }
        return true
    }

    /** 端口是否处于有效范围内。 */
    fun isValidPort(port: Int): Boolean = port in MIN_PORT..MAX_PORT

    /** 获取一个随机端口，范围保持在历史的 `[30000, 39999]`。 */
    fun getRandomPort(): Int = RANDOM_PORT_START + ThreadLocalRandom.current().nextInt(RANDOM_PORT_RANGE)

    /**
     * 获取一个随机且可用的端口。
     * 继续显式同步，避免多线程同时初始化时撞端口。
     */
    @Synchronized
    fun getAvailablePort(): Int = getAvailablePort(getRandomPort())

    /**
     * 从指定端口开始向两侧搜索可用端口，保持原有查找顺序不变。
     */
    @Synchronized
    fun getAvailablePort(port: Int): Int {
        var portTmp = port
        while (portTmp <= MAX_PORT) {
            if (isPortAvailable(portTmp)) {
                return portTmp
            }
            portTmp++
        }
        portTmp = port - 1
        while (portTmp >= MIN_PORT) {
            if (isPortAvailable(portTmp)) {
                return portTmp
            }
            portTmp--
        }
        throw RuntimeException("no available port.")
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }

    /** 判断 host 是否为 localhost。 */
    fun isLocalHost(host: String?): Boolean =
        host != null && (localIpPattern.matcher(host).matches() || host.equals(LOCALHOST_KEY, ignoreCase = true))

    /** 判断 host 是否为 any-host。 */
    fun isAnyHost(host: String?): Boolean = ANYHOST_VALUE == host

    /** 判断 host 是否是可对外使用的本地地址。 */
    fun isValidLocalHost(host: String?): Boolean {
        return host != null &&
            host.isNotEmpty() &&
            !host.equals(LOCALHOST_KEY, ignoreCase = true) &&
            host != ANYHOST_VALUE &&
            !host.startsWith("127.")
    }

    /** `InetSocketAddress` 转 `host:port` 字符串。 */
    fun toAddressString(socketAddress: InetSocketAddress): String =
        socketAddress.address.hostAddress + ":" + socketAddress.port

    /** `host + port` 转地址字符串。 */
    fun toAddressString(host: String?, port: Int): String = toAddressString(toSocketAddress(host, port))

    /** 地址字符串转 `InetSocketAddress`。 */
    fun toSocketAddress(address: String): InetSocketAddress {
        val i = address.indexOf(':')
        val host: String
        val port: Int
        if (i > -1) {
            host = address.substring(0, i)
            port = address.substring(i + 1).toInt()
        } else {
            host = address
            port = 0
        }
        return InetSocketAddress(host, port)
    }

    /** `host + port` 转 `InetSocketAddress`。 */
    fun toSocketAddress(host: String?, port: Int): InetSocketAddress = InetSocketAddress(host, port)

    /** 判断字符串是否为合法 IPv4 地址字符串。 */
    fun isValidV4Address(address: String): Boolean = ipv4AddressPattern.matcher(address).matches()

    /** 判断 `InetAddress` 是否为可用 IPv4 地址。 */
    fun isValidV4Address(address: InetAddress?): Boolean {
        if (address == null || address.isLoopbackAddress) {
            return false
        }

        val name = address.hostAddress
        return name != null &&
            ipv4IpPattern.matcher(name).matches() &&
            name != ANYHOST_VALUE &&
            name != LOCALHOST_VALUE
    }

    private fun isPreferIPV6Address(): Boolean = java.lang.Boolean.getBoolean("java.net.preferIPv6Addresses")

    /** 获取本机 IP 字符串，找不到时保持返回 `null`。 */
    fun getIp(): String? = getLocalAddress()?.hostAddress

    /**
     * 获取本机可用地址，并做结果缓存，避免频繁重复探测网卡。
     */
    fun getLocalAddress(): InetAddress? {
        localAddress?.let { return it }
        val found = getLocalAddress0()
        if (found != null) {
            localAddress = found
        }
        return found
    }

    /**
     * 先遍历网卡，再回退到 `InetAddress.getLocalHost()`。
     * 历史上这里已经吸收异常并打日志，迁移后保持同样的容错模式。
     */
    private fun getLocalAddress0(): InetAddress? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        val network = interfaces.nextElement()
                        if (network.isLoopback || network.isVirtual || !network.isUp) {
                            continue
                        }
                        val addresses = network.inetAddresses
                        while (addresses.hasMoreElements()) {
                            try {
                                val addressItem = toValidAddress(addresses.nextElement())
                                if (addressItem != null) {
                                    try {
                                        if (addressItem.isReachable(100)) {
                                            return addressItem
                                        }
                                    } catch (_: IOException) {
                                    }
                                }
                            } catch (e: Throwable) {
                                logger.error("遍历网卡地址时发生异常。", e)
                            }
                        }
                    } catch (e: Throwable) {
                        logger.error("遍历网卡信息时发生异常。", e)
                    }
                }
            }
        } catch (e: Throwable) {
            logger.warn("获取本机可用地址时发生异常。", e)
        }

        try {
            val fallback = InetAddress.getLocalHost()
            val addressItem = toValidAddress(fallback)
            if (addressItem != null) {
                return addressItem
            }
        } catch (e: Throwable) {
            logger.warn("通过 InetAddress.getLocalHost 获取本机地址时发生异常。", e)
        }

        return null
    }

    private fun toValidAddress(address: InetAddress): InetAddress? {
        if (address is Inet6Address) {
            if (isPreferIPV6Address()) {
                return normalizeV6Address(address)
            }
        }
        if (isValidV4Address(address)) {
            return address
        }
        return null
    }

    /**
     * IPv6 地址归一化时继续只把 scope 名转换成 scope id，保持原有返回格式。
     */
    private fun normalizeV6Address(address: Inet6Address): InetAddress {
        val addr = address.hostAddress
        val i = addr.lastIndexOf('%')
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.scopeId)
            } catch (e: UnknownHostException) {
                logger.debug("无法解析 IPv6 地址，address={}", addr, e)
            }
        }
        return address
    }

    private val logger = LoggerFactory.getLogger(IPTool::class.java)
}
