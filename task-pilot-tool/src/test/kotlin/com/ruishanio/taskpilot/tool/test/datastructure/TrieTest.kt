package com.ruishanio.taskpilot.tool.test.datastructure

import com.ruishanio.taskpilot.tool.datastructure.Trie
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Trie 基础前缀树行为验证。
 */
class TrieTest {
    @Test
    fun test() {
        val trie = Trie()

        trie.insert("apple")
        trie.insert("app")
        trie.insert("application")
        trie.insert("apply")

        logger.info("Search 'app': {}", trie.search("app"))
        logger.info("Search 'apple': {}", trie.search("apple"))
        logger.info("Search 'appl': {}", trie.search("appl"))

        logger.info("Starts with 'app': {}", trie.startsWith("app"))
        logger.info("Starts with 'appli': {}", trie.startsWith("appli"))
        logger.info("Starts with 'ban': {}", trie.startsWith("ban"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TrieTest::class.java)
    }
}
