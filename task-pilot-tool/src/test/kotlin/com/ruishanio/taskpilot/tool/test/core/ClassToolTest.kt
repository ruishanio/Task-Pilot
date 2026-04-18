package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.ClassTool
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.List

/**
 * ClassTool 类型解析与判定验证。
 */
class ClassToolTest {
    @Test
    fun getClassName() {
        logger.info("{}", ClassTool.getClass(ClassToolTest()))

        logger.info("{}", ClassTool.getClassName(ClassToolTest(), false))
        logger.info("{}", ClassTool.getClassName(ClassToolTest(), true))

        logger.info("{}", ClassTool.getClassName(ClassToolTest::class.java, false))
        logger.info("{}", ClassTool.getClassName(ClassToolTest::class.java, true))
    }

    @Test
    fun getPackageName() {
        logger.info("getPackageName = {}", ClassTool.getPackageName(ReflectionToolTest::class.java))
        logger.info("getPackageName = {}", ClassTool.getPackageName(ReflectionToolTest::class.java.name))
    }

    @Test
    fun isAssignable() {
        logger.info("{}", ClassTool.isAssignable(Number::class.java, Int::class.java))
        logger.info("{}", ClassTool.isAssignable(Int::class.java, Int::class.java))
        logger.info("{}", ClassTool.isAssignable(List::class.java, ArrayList::class.java))
        logger.info("{}", ClassTool.isAssignable(Any::class.java, String::class.java))
    }

    @Test
    fun resolveClass() {
        logger.info("{}", ClassTool.resolveClass("int"))
        logger.info("{}", ClassTool.resolveClass("java.lang.Integer"))
        logger.info("{}", ClassTool.resolveClass("com.ruishanio.taskpilot.tool.test.core.ClassToolTest"))
    }

    @Test
    fun isPrimitive() {
        Assertions.assertTrue(ClassTool.isPrimitive(Int::class.javaPrimitiveType))
        Assertions.assertTrue(ClassTool.isPrimitiveWrapper(Int::class.javaObjectType))
        Assertions.assertTrue(ClassTool.isPrimitiveOrWrapper(Int::class.javaObjectType))
        Assertions.assertTrue(ClassTool.isPrimitiveOrWrapperOrString(String::class.java))

        Assertions.assertFalse(ClassTool.isPrimitive(ClassToolTest::class.java))
        Assertions.assertFalse(ClassTool.isPrimitiveWrapper(ClassToolTest::class.java))
        Assertions.assertFalse(ClassTool.isPrimitiveOrWrapper(ClassToolTest::class.java))
        Assertions.assertFalse(ClassTool.isPrimitiveOrWrapperOrString(ClassToolTest::class.java))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClassToolTest::class.java)
    }
}
