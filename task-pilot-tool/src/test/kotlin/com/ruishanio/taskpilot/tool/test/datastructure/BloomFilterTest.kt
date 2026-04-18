package com.ruishanio.taskpilot.tool.test.datastructure

import com.ruishanio.taskpilot.tool.datastructure.BloomFilter
import com.ruishanio.taskpilot.tool.datastructure.bloomfilter.Funnels
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * BloomFilter 误判率与基本命中验证。
 */
class BloomFilterTest {
    @Test
    fun test1() {
        val bloomFilter = BloomFilter.create(1000000, 0.01)

        bloomFilter.put("item1")
        logger.info("mightContain: {}", bloomFilter.mightContain("item1"))
        logger.info("mightContain: {}", bloomFilter.mightContain("item2"))
    }

    @Test
    fun test2() {
        val bloomFilter = BloomFilter.create(Funnels.LONG, 1000000, 0.01)

        bloomFilter.put(999L)
        logger.info("mightContain: {}", bloomFilter.mightContain(999L))
        logger.info("mightContain: {}", bloomFilter.mightContain(666L))
    }

    @Test
    fun test3() {
        val bloomFilter = BloomFilter.create(10000, 0.01)

        val itemCount = 10000
        for (index in 0 until itemCount) {
            bloomFilter.put("item$index")
        }

        for (index in 0 until 100 step 10) {
            Assertions.assertTrue(bloomFilter.mightContain("item$index"))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BloomFilterTest::class.java)
    }
}
