package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.MapTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.HashMap

/**
 * MapTool 常用访问与建图能力验证。
 */
class MapToolTest {
    @Test
    fun test() {
        val map = HashMap<String, Int>()
        map["k1"] = 1
        map["k2"] = 2
        map["k3"] = 3

        logger.info("{}", MapTool.isNotEmpty(map))
        logger.info("{}", MapTool.getInteger(map, "k1"))
    }

    @Test
    fun newTest() {
        println(MapTool.newMap<String, Int>())
        println(MapTool.newMap("k1", 1, "k2", 2))
        println(MapTool.newMap("k1", 1, "k2", 2, "k3", 3))
        println(MapTool.newMap("k1", 1, "k2", 2, "k3", 3, "k4", 4))
        println(MapTool.newMap("k1", 1, "k2", 2, "k3", 3, "k4", 4, "k5", 5))
        println(hashMapOf("k1" to 1, "k2" to 2, "k3" to 3))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StringToolTest::class.java)
    }
}
