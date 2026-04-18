package com.ruishanio.taskpilot.tool.emoji.loader

import com.ruishanio.taskpilot.tool.emoji.model.Emoji
import java.util.HashMap

/**
 * Emoji Trie。
 * 继续用最长匹配策略识别 emoji，避免短 emoji 抢占长组合 emoji。
 */
class EmojiTrie(
    emojis: Collection<Emoji>,
) {
    private val root = Node()

    init {
        for (emoji in emojis) {
            var tree = root
            for (c in emoji.getUnicode().toCharArray()) {
                if (!tree.hasChild(c)) {
                    tree.addChild(c)
                }
                tree = tree.getChild(c)!!
            }
            tree.setEmoji(emoji)
        }
    }

    fun isEmoji(sequence: CharArray?): Matches {
        if (sequence == null) {
            return Matches.POSSIBLY
        }

        var tree = root
        for (c in sequence) {
            if (!tree.hasChild(c)) {
                return Matches.IMPOSSIBLE
            }
            tree = tree.getChild(c)!!
        }
        return if (tree.isEndOfEmoji()) Matches.EXACTLY else Matches.POSSIBLY
    }

    fun getEmoji(unicode: String): Emoji? {
        var tree = root
        for (c in unicode.toCharArray()) {
            if (!tree.hasChild(c)) {
                return null
            }
            tree = tree.getChild(c)!!
        }
        return tree.getEmoji()
    }

    enum class Matches {
        EXACTLY,
        POSSIBLY,
        IMPOSSIBLE,
        ;

        fun exactMatch(): Boolean = this == EXACTLY

        fun impossibleMatch(): Boolean = this == IMPOSSIBLE
    }

    private class Node {
        private val children: MutableMap<Char, Node> = HashMap()
        private var emoji: Emoji? = null

        fun setEmoji(emoji: Emoji) {
            this.emoji = emoji
        }

        fun getEmoji(): Emoji? = emoji

        fun hasChild(child: Char): Boolean = children.containsKey(child)

        fun addChild(child: Char) {
            children[child] = Node()
        }

        fun getChild(child: Char): Node? = children[child]

        fun isEndOfEmoji(): Boolean = emoji != null
    }
}
