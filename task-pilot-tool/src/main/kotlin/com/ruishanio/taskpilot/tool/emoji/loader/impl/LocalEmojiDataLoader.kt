package com.ruishanio.taskpilot.tool.emoji.loader.impl

import com.ruishanio.taskpilot.tool.emoji.loader.EmojiDataLoader
import com.ruishanio.taskpilot.tool.emoji.model.Emoji
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import org.slf4j.LoggerFactory

/**
 * 本地 Emoji 数据加载器。
 * 继续从内置 JSON 资源加载，避免引入网络或外部配置依赖。
 */
class LocalEmojiDataLoader : EmojiDataLoader() {
    override fun loadEmojiData(): kotlin.collections.List<Emoji>? {
        return try {
            val resourcePath = LocalEmojiDataLoader::class.java.getResource(PATH)?.path ?: return null
            val emojiJson = FileTool.readString(resourcePath)

            @Suppress("UNCHECKED_CAST")
            val emojiArr = GsonTool.fromJson<List<Any>?>(emojiJson, List::class.java)
            if (emojiArr.isNullOrEmpty()) {
                return null
            }

            val emojis = ArrayList<Emoji>()
            for (emojiItem in emojiArr) {
                if (emojiItem is Map<*, *>) {
                    val unicode = emojiItem["unicode"].toString()
                    val aliases =
                        if (emojiItem["aliases"] is kotlin.collections.List<*>) {
                            (emojiItem["aliases"] as kotlin.collections.List<*>).map { it.toString() }
                        } else {
                            emptyList()
                        }
                    val tags =
                        if (emojiItem["tags"] is kotlin.collections.List<*>) {
                            (emojiItem["tags"] as kotlin.collections.List<*>).map { it.toString() }
                        } else {
                            emptyList()
                        }
                    val supportsFitzpatrick =
                        if (emojiItem.containsKey("supports_fitzpatrick")) {
                            emojiItem["supports_fitzpatrick"].toString().toBoolean()
                        } else {
                            false
                        }

                    emojis.add(Emoji(unicode, aliases, tags, supportsFitzpatrick))
                }
            }
            emojis
        } catch (e: Exception) {
            logger.error("加载本地 Emoji 数据时发生异常。", e)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalEmojiDataLoader::class.java)
        private const val PATH: String = "/task-pilot-tool/emoji/task-pilot-tool-emoji.json"
    }
}
