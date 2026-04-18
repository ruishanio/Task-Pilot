package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.core.StringTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * StringTool 字符串处理行为验证。
 */
class StringToolTest {
    @Test
    fun test() {
        logger.info("{}", StringTool.isEmpty("  "))
        logger.info("{}", StringTool.isBlank("  "))
    }

    @Test
    fun isNumericTest() {
        logger.info("isNumeric: {}", StringTool.isNumeric(null))
        logger.info("isNumeric: {}", StringTool.isNumeric(""))
        logger.info("isNumeric: {}", StringTool.isNumeric("  "))
        logger.info("isNumeric: {}", StringTool.isNumeric("123"))
        logger.info("isNumeric: {}", StringTool.isNumeric("12 3"))
        logger.info("isNumeric: {}", StringTool.isNumeric("ab2c"))
        logger.info("isNumeric: {}", StringTool.isNumeric("12-3"))
        logger.info("isNumeric: {}", StringTool.isNumeric("12.3"))
    }

    @Test
    fun underlineToCamelCaseTest() {
        val text = "aaa_bbb"
        logger.info("text = {}", text)
        logger.info("result = {}", StringTool.underlineToCamelCase(text))
    }

    @Test
    fun camelCaseToUnderlineTest() {
        val text = "StringToolTest"
        logger.info("text = {}", text)
        logger.info("lowerCaseFirst = {}", StringTool.lowerCaseFirst(text))
    }

    @Test
    fun substring() {
        val text = "123456789"
        logger.info("text = {}", text)

        logger.info("substring = {}", StringTool.substring(null, 2))
        logger.info("substring = {}", StringTool.substring("", 2))
        logger.info("substring = {}", StringTool.substring("abc", 0))
        logger.info("substring = {}", StringTool.substring("abc", 2))
        logger.info("substring = {}", StringTool.substring("abc", 4))
        logger.info("substring = {}", StringTool.substring("abc", -2))

        logger.info("substring2 = {}", StringTool.substring(null, 1, 2))
        logger.info("substring2 = {}", StringTool.substring("", 1, 2))
        logger.info("substring2 = {}", StringTool.substring("abc", 1, 2))
        logger.info("substring2 = {}", StringTool.substring("abc", -1, 2))
        logger.info("substring2 = {}", StringTool.substring("abc", 1, 0))
        logger.info("substring2 = {}", StringTool.substring("abc", 1, 5))
        logger.info("substring2 = {}", StringTool.substring("abc", 2, 1))
    }

    @Test
    fun join() {
        logger.info("split = {}", StringTool.split("a,b,c", ","))
        logger.info("split = {}", StringTool.split("a, b ,c", ","))
        logger.info("split = {}", StringTool.split("a,,c, ", ","))

        logger.info("join = {}", StringTool.join(listOf("a", "b", "c"), ","))
        logger.info("join = {}", StringTool.join(listOf("a", "b", " c "), ","))
        logger.info("join = {}", StringTool.join(listOf("a", "b", ""), ","))
        logger.info("join = {}", StringTool.join(listOf("a", null, "c"), ","))
    }

    @Test
    fun format() {
        logger.info("format = {}", StringTool.format("hello,{0}!", "world"))
        logger.info("format = {}", StringTool.format("hello,{0}!", null))
        logger.info("format = {}", StringTool.format("hello,{0}!"))
        logger.info("format = {}", StringTool.format("hello,{0}!", "world", "world"))

        logger.info("format = {}", StringTool.format("Hello {0}, welcome {1}!"))
        logger.info("format = {}", StringTool.format("Hello {0}, welcome {1}!", null))
        logger.info("format = {}", StringTool.format("Hello {0}, welcome {1}!", null, null))
        logger.info("format = {}", StringTool.format("Hello {0}, welcome {1}!", "Alice"))
        logger.info("format = {}", StringTool.format("Hello {0}, welcome {1}!", "Alice", "Jack"))
        logger.info("format = {}", StringTool.format("Hello {0}, welcome {1}!", "Alice", "Jack", "Lucy"))

        logger.info("format = {}", StringTool.format("Hello {0}, you have {1} messages", "Alice", 5))
        logger.info("format = {}", StringTool.format("{1} messages for {0}", "Alice", 5))
        logger.info("format = {}", StringTool.format("Hello {0}, welcome {0}!", "Alice"))

        logger.info("format = {}", StringTool.format("Balance: {0,number}", 1234.56))
        logger.info("format = {}", StringTool.format("Price: {0,number,currency}", 1234.56))
        logger.info("format = {}", StringTool.format("Success rate: {0,number,percent}", 0.85))
        logger.info("format = {}", StringTool.format("Account: {0,number,#,##0.00}", 1234.5))
    }

    @Test
    fun formatWithMap() {
        logger.info(
            "format = {}",
            StringTool.formatWithMap("{name} is {age} years old", MapTool.newMap("name", "jack", "age", 18)),
        )
        logger.info("format = {}", StringTool.formatWithMap("{name} is {age} years old", null))
        logger.info(
            "format = {}",
            StringTool.formatWithMap("{name} is {age} years old", MapTool.newMap("name", "jack")),
        )
        logger.info(
            "format = {}",
            StringTool.formatWithMap(
                "{name} is {age} years old",
                MapTool.newMap<String, Any?>("name", "jack", "age", null),
            ),
        )
    }

    @Test
    fun replace() {
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", "jack", "lucy"))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you, jack", "jack", "lucy"))
        logger.info("replace = {}", StringTool.replace("", "jack", "lucy"))
        logger.info("replace = {}", StringTool.replace(null, "jack", "lucy"))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", null, "jack"))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", "", "jack"))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", " ", "-"))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", "jack", null))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", "jack", ""))
        logger.info("replace = {}", StringTool.replace("hello jack, how are you", "jack", " "))
    }

    @Test
    fun remove() {
        logger.info("removePrefix = {}", StringTool.removePrefix("hello,world", "hello"))
        logger.info("removePrefix = {}", StringTool.removePrefix("hello,world", "world"))
        logger.info("removePrefix = {}", StringTool.removePrefix("hello,world", "hello,world"))
        logger.info("removePrefix = {}", StringTool.removePrefix("hello,world", ""))
        logger.info("removePrefix = {}", StringTool.removePrefix("hello,world", null))
        logger.info("removePrefix = {}", StringTool.removePrefix("", "world"))
        logger.info("removePrefix = {}", StringTool.removePrefix(null, "world"))

        logger.info("removeSuffix = {}", StringTool.removeSuffix("hello,world", "hello"))
        logger.info("removeSuffix = {}", StringTool.removeSuffix("hello,world", "world"))
        logger.info("removeSuffix = {}", StringTool.removeSuffix("hello,world", "hello,world"))
        logger.info("removeSuffix = {}", StringTool.removeSuffix("hello,world", ""))
        logger.info("removeSuffix = {}", StringTool.removeSuffix("hello,world", null))
        logger.info("removeSuffix = {}", StringTool.removeSuffix("", "world"))
        logger.info("removeSuffix = {}", StringTool.removeSuffix(null, "world"))
    }

    @Test
    fun equals() {
        logger.info("equals = {}", StringTool.equals("hello", "hello"))
        logger.info("equals = {}", StringTool.equals("hello", "world"))
        logger.info("equals = {}", StringTool.equals(null, null))
        logger.info("equals = {}", StringTool.equals(null, "world"))
        logger.info("equals = {}", StringTool.equals("hello", null))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StringToolTest::class.java)
    }
}
