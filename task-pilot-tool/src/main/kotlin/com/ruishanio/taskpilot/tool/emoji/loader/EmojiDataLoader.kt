package com.ruishanio.taskpilot.tool.emoji.loader

import com.ruishanio.taskpilot.tool.emoji.model.Emoji

/**
 * Emoji 数据加载器。
 */
abstract class EmojiDataLoader {
    abstract fun loadEmojiData(): kotlin.collections.List<Emoji>?
}
