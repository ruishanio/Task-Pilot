package com.ruishanio.taskpilot.tool.test.serializer

import com.ruishanio.taskpilot.tool.serializer.Serializer
import com.ruishanio.taskpilot.tool.serializer.SerializerEnum
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.Serializable

/**
 * Serializer 序列化与反序列化验证。
 */
class SerializerTest {
    @Test
    fun test() {
        val serializer: Serializer = SerializerEnum.JAVA.serializer

        val demoUser = DemoUser("jack", 18)
        val bytes = serializer.serialize(demoUser)
        logger.info("demoUser: {}", demoUser)

        val demoUser2: DemoUser = serializer.deserialize(bytes)
        logger.info("demoUser2: {}", demoUser2)
    }

    /**
     * 序列化测试对象保留可变属性，兼容 Java 序列化路径。
     */
    data class DemoUser(
        var name: String? = null,
        var age: Int? = null,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 42L
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SerializerTest::class.java)
    }
}
