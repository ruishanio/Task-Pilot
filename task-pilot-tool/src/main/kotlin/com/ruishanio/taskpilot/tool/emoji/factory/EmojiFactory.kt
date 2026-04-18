package com.ruishanio.taskpilot.tool.emoji.factory

import com.ruishanio.taskpilot.tool.emoji.exception.TaskPilotEmojiException
import com.ruishanio.taskpilot.tool.emoji.loader.EmojiDataLoader
import com.ruishanio.taskpilot.tool.emoji.loader.EmojiTrie
import com.ruishanio.taskpilot.tool.emoji.loader.impl.LocalEmojiDataLoader
import com.ruishanio.taskpilot.tool.emoji.model.AliasCandidate
import com.ruishanio.taskpilot.tool.emoji.model.Emoji
import com.ruishanio.taskpilot.tool.emoji.model.UnicodeCandidate
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.regex.Pattern

/**
 * Emoji 工厂。
 * 启动时一次性构建 alias 和 trie 索引，避免每次编码解码都重复装载数据。
 */
object EmojiFactory {
    private lateinit var allEmojis: kotlin.collections.List<Emoji>
    private lateinit var emojiTrie: EmojiTrie
    private lateinit var emojisByAlias: MutableMap<String, Emoji>
    private lateinit var emojisByTag: MutableMap<String, MutableSet<Emoji>>
    private var emojiLoader: EmojiDataLoader = LocalEmojiDataLoader()

    private val aliasCandidatePattern: Pattern = Pattern.compile("(?<=:)\\+?(\\w|\\||\\-)+(?=:)")

    init {
        loadEmoji()
    }
    fun setEmojiLoader(emojiLoader: EmojiDataLoader) {
        this.emojiLoader = emojiLoader
    }
    fun loadEmoji() {
        val emojis = emojiLoader.loadEmojiData()
        if (emojis.isNullOrEmpty()) {
            throw TaskPilotEmojiException("emoji loader fail")
        }

        allEmojis = emojis
        emojiTrie = EmojiTrie(allEmojis)
        emojisByAlias = HashMap()
        emojisByTag = HashMap()

        for (emoji in allEmojis) {
            for (alias in emoji.getAliases()) {
                emojisByAlias[alias] = emoji
            }
            for (tag in emoji.getTags()) {
                if (emojisByTag[tag] == null) {
                    emojisByTag[tag] = HashSet()
                }
                emojisByTag[tag]!!.add(emoji)
            }
        }
    }
    fun getForAlias(alias: String?): Emoji? {
        if (alias == null) {
            return null
        }
        return emojisByAlias[trimAlias(alias)]
    }
    fun getForTag(tag: String?): Set<Emoji>? {
        if (tag == null) {
            return null
        }
        return emojisByTag[tag]
    }
    fun getAllTags(): Set<String> = emojisByTag.keys
    fun getByUnicode(unicode: String?): Emoji? {
        if (unicode == null) {
            return null
        }
        return emojiTrie.getEmoji(unicode)
    }
    fun getAll(): kotlin.collections.List<Emoji> = allEmojis
    fun getAliasCandidates(input: String): kotlin.collections.List<AliasCandidate> {
        val candidates = ArrayList<AliasCandidate>()
        var matcher = aliasCandidatePattern.matcher(input)
        matcher = matcher.useTransparentBounds(true)
        while (matcher.find()) {
            val match = matcher.group()
            if (!match.contains("|")) {
                candidates.add(AliasCandidate(match, match, null))
            } else {
                val splitted = match.split("\\|".toRegex())
                if (splitted.size >= 2) {
                    candidates.add(AliasCandidate(match, splitted[0], splitted[1]))
                } else {
                    candidates.add(AliasCandidate(match, match, null))
                }
            }
        }
        return candidates
    }
    fun getUnicodeCandidates(input: String): kotlin.collections.List<UnicodeCandidate> {
        val inputCharArray = input.toCharArray()
        val candidates = ArrayList<UnicodeCandidate>()
        var i = 0
        while (i < inputCharArray.size) {
            val next = getNextUnicodeCandidate(inputCharArray, i) ?: break
            candidates.add(next)
            i = next.getFitzpatrickEndIndex()
        }
        return candidates
    }

    /**
     * 继续按当前位置向后找“第一个能最长匹配成功的 emoji”，避免前缀 emoji 截断组合 emoji。
     */
    fun getNextUnicodeCandidate(
        chars: CharArray,
        start: Int,
    ): UnicodeCandidate? {
        for (i in start until chars.size) {
            val emojiEnd = getFirstEmojiEndPos(chars, i)
            if (emojiEnd != -1) {
                val emoji = getByUnicode(String(chars, i, emojiEnd - i)) ?: continue
                val fitzpatrickString = if (emojiEnd + 2 <= chars.size) String(chars, emojiEnd, 2) else null
                return UnicodeCandidate(emoji, fitzpatrickString, i)
            }
        }
        return null
    }
    fun getFirstEmojiEndPos(
        text: CharArray,
        startPos: Int,
    ): Int {
        var best = -1
        for (j in startPos + 1..text.size) {
            val status = emojiTrie.isEmoji(Arrays.copyOfRange(text, startPos, j))
            if (status.exactMatch()) {
                best = j
            } else if (status.impossibleMatch()) {
                return best
            }
        }
        return best
    }

    private fun trimAlias(alias: String): String {
        var result = alias
        if (result.startsWith(":")) {
            result = result.substring(1, result.length)
        }
        if (result.endsWith(":")) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }
}
