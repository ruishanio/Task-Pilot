package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.CollectionTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.ArrayList

/**
 * CollectionTool 集合操作验证。
 */
class CollectionToolTest {
    @Test
    fun isEmptyTest() {
        val list = arrayListOf(1, 2, 3)

        logger.info("{}", CollectionTool.isEmpty(list))
        logger.info("{}", CollectionTool.isNotEmpty(list))
    }

    @Test
    fun containsTest() {
        val list = arrayListOf(1, 2, 3)

        logger.info("{}", CollectionTool.contains(list, 3))
        logger.info("{}", CollectionTool.contains(list, 4))
    }

    @Test
    fun operateTest() {
        val a = arrayListOf(1, 2, 3, 3, 4, 5)
        val b = arrayListOf(3, 4, 4, 5, 6, 7)

        logger.info("{}", CollectionTool.union(a, b))
        logger.info("{}", CollectionTool.intersection(a, b))
        logger.info("{}", CollectionTool.disjunction(a, b))
        logger.info("{}", CollectionTool.subtract(a, b))
        logger.info("{}", CollectionTool.subtract(b, a))
    }

    @Test
    fun newTest() {
        logger.info("list = {}", CollectionTool.newArrayList<Any>())
        logger.info("list = {}", CollectionTool.newArrayList(1, 2, 3))
    }

    @Test
    fun splitTest() {
        val list = CollectionTool.newArrayList("1", "2", "3", "4", "5", "6", "7")

        logger.info("list = {}", list)
        logger.info("list = {}", CollectionTool.split(list, 3))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CollectionToolTest::class.java)
    }
}
