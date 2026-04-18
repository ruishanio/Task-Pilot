package com.ruishanio.taskpilot.tool.datastructure

import java.util.HashMap

/**
 * Trie 树。
 * 继续用最朴素的字符节点结构，避免在前缀匹配场景里引入压缩路径等额外复杂度。
 */
class Trie {
    private val root = TrieNode()

    fun insert(word: String) {
        var current = root
        for (ch in word.toCharArray()) {
            current = current.children.computeIfAbsent(ch) { TrieNode() }
        }
        current.isEndOfWord = true
    }

    fun search(word: String): Boolean {
        var current = root
        for (ch in word.toCharArray()) {
            current = current.children[ch] ?: return false
        }
        return current.isEndOfWord
    }

    fun startsWith(prefix: String): Boolean {
        var current = root
        for (ch in prefix.toCharArray()) {
            current = current.children[ch] ?: return false
        }
        return true
    }

    /**
     * 节点仍保留 children + 结束标记两部分状态，方便后续需要时继续扩展词频或附加值。
     */
    private class TrieNode {
        var children: MutableMap<Char, TrieNode> = HashMap()
        var isEndOfWord: Boolean = false
    }
}
