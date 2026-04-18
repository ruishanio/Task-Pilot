package com.ruishanio.taskpilot.tool.test.json

import com.google.gson.JsonElement
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * 覆盖 GsonTool 的基础序列化和泛型反序列化能力。
 */
class GsonToolTest {
    @Test
    fun testJson() {
        val result =
            linkedMapOf<String, Any>(
                "int" to 200,
                "str" to "success",
                "arr" to listOf("111", "222"),
                "float" to 1.11f
            )

        val json = GsonTool.toJson(result)
        logger.info(json)

        val objectMap = GsonTool.fromJson(json, Map::class.java)
        logger.info(objectMap.toString())
    }

    @Test
    fun testJson2() {
        val result =
            linkedMapOf<String, Any>(
                "int" to 200,
                "str" to "success",
                "arr" to listOf("111", "222"),
                "float" to 1.11f
            )

        val json = GsonTool.toJsonPretty(result)
        logger.info(json)

        val objectMap = GsonTool.fromJson(json, Map::class.java)
        logger.info(objectMap.toString())
    }

    @Test
    fun testFromJsonList() {
        val demo = Demo("abc", 100)
        val json = GsonTool.toJson(demo)
        logger.info(json)

        val demo2 = GsonTool.fromJson(json, Demo::class.java)
        logger.info(demo2.toString())

        assertEquals(json, GsonTool.toJson(demo2))
    }

    @Test
    fun testType() {
        val originData = Response.ofSuccess<DemoResult>()
        originData.data = DemoResult(listOf(Demo("abc", 100), Demo("def", 200)))

        val data1: JsonElement = GsonTool.toJsonElement(originData)
        val json = GsonTool.toJson(data1)
        logger.info("data1 = {}", json)

        val data2: Response<DemoResult> = GsonTool.fromJsonElement(data1, Response::class.java, DemoResult::class.java)
        logger.info("data2 = {}", data2)

        assertEquals(json, GsonTool.toJson(data2))
    }

    @Test
    fun testType2() {
        val originData = Response.ofSuccess<List<Demo>>()
        originData.data = listOf(Demo("abc", 100), Demo("def", 200))

        val data1: JsonElement = GsonTool.toJsonElement(originData)
        val json = GsonTool.toJson(data1)
        logger.info("data1 = {}", json)

        @Suppress("UNCHECKED_CAST")
        val data2 = GsonTool.fromJsonElement(data1, Response::class.java, List::class.java) as Response<List<Demo>>
        logger.info("data2 = {}", data2)

        assertEquals(json, GsonTool.toJson(data2))
    }

    /**
     * 继续保留可变属性和无参构造，避免 Gson 在测试里因为 Kotlin 默认不可变模型而偏离真实使用方式。
     */
    class DemoResult(
        var demoList: List<Demo>? = null
    )

    class Demo(
        var arg1: String? = null,
        var arg2: Int = 0
    ) {
        override fun toString(): String = "Demo{arg1='$arg1', arg2=$arg2}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GsonToolTest::class.java)
    }
}
