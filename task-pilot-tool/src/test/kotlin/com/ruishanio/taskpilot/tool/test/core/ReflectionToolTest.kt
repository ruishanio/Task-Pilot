package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.ReflectionTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * ReflectionTool 反射辅助能力验证。
 */
class ReflectionToolTest {
    private var test3 = "111"

    fun test2() {
        println("test2")
    }

    @Test
    fun getMethod() {
        logger.info("getMethod: {}", ReflectionTool.getMethod(ReflectionToolTest::class.java, "getMethod"))
        logger.info(
            "getDeclaredMethod: {}",
            ReflectionTool.getDeclaredMethod(ReflectionToolTest::class.java, "getMethod"),
        )

        logger.info(
            "getMethods: {}",
            ReflectionTool.getMethods(ReflectionToolTest::class.java, true).map(Method::getName),
        )
        logger.info(
            "getMethods: {}",
            ReflectionTool.getMethods(ReflectionToolTest::class.java, false).map(Method::getName),
        )
    }

    @Test
    fun getField() {
        logger.info(
            "getFields: {}",
            ReflectionTool.getFields(ReflectionToolTest::class.java, true).map(Field::getName),
        )
        logger.info(
            "getFields: {}",
            ReflectionTool.getFields(ReflectionToolTest::class.java, false).map(Field::getName),
        )

        logger.info("field1 = {}", ReflectionTool.getField(ReflectionToolTest::class.java, "name"))
        logger.info("field1 = {}", ReflectionTool.getDeclaredField(ReflectionToolTest::class.java, "name"))

        val field1 = requireNotNull(ReflectionTool.getField(ReflectionToolTest::class.java, "name"))
        logger.info("isPublicStaticFinal: {}", ReflectionTool.isPublicStaticFinal(field1))

        val field3 = requireNotNull(ReflectionTool.getDeclaredField(ReflectionToolTest::class.java, "test3"))
        logger.info("field3 = {}", field3)
        logger.info("{}", test3)

        field3.setAccessible(true)
        ReflectionTool.makeAccessible(field3)
        ReflectionTool.setFieldValue(field3, this, "222")
        logger.info("{}", test3)
    }

    @Test
    fun test03() {
        val method = requireNotNull(ReflectionTool.getMethod(ReflectionToolTest::class.java, "test2"))
        logger.info("method = {}", method)
        ReflectionTool.makeAccessible(method)
        ReflectionTool.invokeMethod(method, ReflectionToolTest())
    }

    @Test
    fun test04() {
        ReflectionTool.doWithFields(
            ReflectionToolTest::class.java,
            object : ReflectionTool.FieldCallback {
                override fun doWith(field: Field) {
                    logger.info("doWith - field = {}", field)
                    logger.info("doWith - field = {}", field)
                }
            },
        )
    }

    companion object {
        const val name: String = ""

        private val logger = LoggerFactory.getLogger(ReflectionToolTest::class.java)
    }
}
