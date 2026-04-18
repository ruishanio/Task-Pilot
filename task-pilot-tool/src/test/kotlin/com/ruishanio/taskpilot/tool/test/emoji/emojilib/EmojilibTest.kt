package com.ruishanio.taskpilot.tool.test.emoji.emojilib

import com.ruishanio.taskpilot.tool.emoji.model.Emoji
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * emojilib 数据源解析验证。
 */
class EmojilibTest {
    @Test
    @Suppress("UNCHECKED_CAST")
    fun parseEmojiList() {
        val emojiJson =
            FileTool.readString(
                EmojilibTest::class.java.getResource("/task-pilot-tool/emoji/emojilib/emojis.json")!!.path,
            )
        val emojiArr = GsonTool.fromJson(emojiJson, Map::class.java) as Map<String, Map<String, Any?>>

        val emojiList = LinkedHashSet<Emoji>()
        for ((alias, value) in emojiArr) {
            val unicode = value["char"].toString()
            val supportsFitzpatrick = value["fitzpatrick_scale"]?.toString()?.toBooleanStrictOrNull() ?: false
            val tags = (value["keywords"] as? List<String>) ?: emptyList()
            emojiList.add(Emoji(unicode, listOf(alias), tags, supportsFitzpatrick))
        }
        logger.info("emojiList:{}", emojiList.size)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmojilibTest::class.java)
    }
}
