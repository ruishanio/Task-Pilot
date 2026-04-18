package com.ruishanio.taskpilot.tool.test.cache

import com.ruishanio.taskpilot.tool.cache.CacheTool
import com.ruishanio.taskpilot.tool.cache.CacheType
import com.ruishanio.taskpilot.tool.cache.iface.Cache
import com.ruishanio.taskpilot.tool.cache.iface.CacheListener
import com.ruishanio.taskpilot.tool.cache.iface.CacheLoader
import com.ruishanio.taskpilot.tool.core.StringTool
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.text.DateFormat
import java.text.MessageFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * CacheTool 构建链与缓存行为验证。
 */
class CacheToolTest {
    @Test
    fun test01() {
        val cache: Cache<String, String> = CacheTool.newFIFOCache<String, String>(3).build()

        val key = "key01"

        var value = cache.get(key)
        logger.info("first get is null, value = {}", value)
        Assertions.assertNull(value, "init fail")

        cache.put(key, "value01")
        value = cache.get(key)
        logger.info("set, value = {}", value)
        Assertions.assertEquals("value01", value, "set fail")

        cache.remove(key)
        value = cache.get(key)
        logger.info("remove, value = {}", value)
        Assertions.assertNull(value, "remove fail")

        for (index in 0 until 10) {
            cache.put("key$index", index.toString())
        }
        logger.info("size = {}", cache.size())
        logger.info("asMap = {}", cache.asMap())
        Assertions.assertEquals(3, cache.size(), "limit size fail")
    }

    @Test
    fun test02() {
        val cache: Cache<String, String> =
            CacheTool.newLRUCache<String, String>(3)
                .expireAfterAccess(100)
                .build()

        val key = "key01"
        cache.put(key, "value01")

        var value = cache.get(key)
        logger.info("set, value = {}", value)
        Assertions.assertEquals("value01", value, "set fail")

        TimeUnit.MILLISECONDS.sleep(70)
        value = cache.get(key)

        TimeUnit.MILLISECONDS.sleep(70)
        value = cache.get(key)
        logger.info("get when timeout, value = {}", value)
        Assertions.assertNotNull(value, "timeout fail")
    }

    @Test
    fun test03() {
        val cache: Cache<String, String> =
            CacheTool.newLRUCache<String, String>(3)
                .expireAfterWrite(100)
                .build()

        val key = "key01"
        cache.put(key, "value01")

        var value = cache.get(key)
        logger.info("set, value = {}", value)
        Assertions.assertEquals("value01", value, "set fail")

        TimeUnit.MILLISECONDS.sleep(70)
        value = cache.get(key)

        TimeUnit.MILLISECONDS.sleep(70)
        value = cache.get(key)
        logger.info("get when timeout, value = {}", value)
        Assertions.assertNull(value, "timeout fail")
    }

    @Test
    fun test04() {
        val cache: Cache<String, String> =
            CacheTool.newLRUCache<String, String>()
                .expireAfterAccess(5000)
                .pruneInterval(100)
                .build()

        for (index in 0 until 5) {
            TimeUnit.MILLISECONDS.sleep(1000)
            cache.put("key-$index", "value-$index")
        }

        for (index in 0 until 10) {
            TimeUnit.MILLISECONDS.sleep(1000)
            cache.get("key-1")
            logger.info("size = {}", cache.size())
        }
    }

    @Test
    fun test05() {
        val cache: Cache<String, String> =
            CacheTool.newLRUCache<String, String>()
                .loader(
                    object : CacheLoader<String, String>() {
                        override fun load(key: String): String = "value-$key"
                    },
                ).build()

        val key = "key01"

        var value = cache.getIfPresent(key)
        logger.info("getIfPresent, value = {}", value)
        Assertions.assertNull(value, "getIfPresent fail")

        value = cache.get(key)
        logger.info("get, value = {}", value)
        Assertions.assertEquals("value-$key", value, "set fail")
    }

    @Test
    fun test06() {
        val cache: Cache<String, String> =
            CacheTool.newLRUCache<String, String>()
                .listener(
                    object : CacheListener<String, String>() {
                        override fun onRemove(key: String, value: String) {
                            logger.info("onRemove, key = {}, value = {}", key, value)
                        }
                    },
                ).build()

        val key = "key01"
        cache.put(key, "value01")
        logger.info("put, value = {}", cache.get(key))
        Assertions.assertEquals("value01", cache.get(key), "set fail")

        cache.remove(key)
        logger.info("remove, value = {}", cache.get(key))

        Assertions.assertNull(cache.get(key), "remove fail")
    }

    @Test
    fun test12() {
        val cache: Cache<String, String> = CacheTool.newLRUCache<String, String>().build()

        for (index in 0 until 5) {
            val key = "key-$index"
            val value =
                cache.get(
                    key,
                    object : CacheLoader<String, String>() {
                        override fun load(key: String): String {
                            val value = "value-$key"
                            logger.info(StringTool.format("load: key={0}, value={1}", key, value))
                            return value
                        }
                    },
                )
            logger.info(StringTool.format("get: key={0}, value={1}", key, value))
            Assertions.assertEquals("value-$key", value, "get fail")
        }

        for (index in 0 until 10) {
            val key = "key-$index"
            logger.info(StringTool.format("get: key={0}, value={1}", key, cache.get(key)))
        }
    }

    @Test
    fun test13() {
        val cache: Cache<String, String> =
            CacheTool.newCache<String, String>()
                .expireAfterAccess(10 * 1000)
                .capacity(70)
                .build()

        for (index in 0 until 100) {
            val key = "key-$index"
            cache.get(
                key,
                object : CacheLoader<String, String>() {
                    override fun load(key: String): String = "value-$key"
                },
            )
        }

        logger.info(
            StringTool.format(
                """
                》》monitor:
                hitCount:{0} ,
                missCount:{1} ,
                size:{2} ,
                isEmpty:{3} ,
                isFull:{4} ,
                """.trimIndent(),
                cache.hitCount(),
                cache.missCount(),
                cache.size(),
                cache.isEmpty(),
                cache.isFull(),
            ),
        )

        for (index in 0 until 100) {
            cache.get("key-$index")
        }

        logger.info(
            StringTool.format(
                """
                》》monitor:
                hitCount:{0} ,
                missCount:{1} ,
                size:{2} ,
                isEmpty:{3} ,
                isFull:{4} ,
                """.trimIndent(),
                cache.hitCount(),
                cache.missCount(),
                cache.size(),
                cache.isEmpty(),
                cache.isFull(),
            ),
        )
    }

    @Test
    fun test14() {
        val cache: Cache<String, String> =
            CacheTool.newLRUCache<String, String>()
                .expireAfterAccess(10 * 1000)
                .capacity(1000)
                .cache(CacheType.LFU)
                .loader(
                    object : CacheLoader<String, String>() {
                        override fun load(key: String): String {
                            TimeUnit.MILLISECONDS.sleep(50)
                            return MessageFormat.format(
                                "data={0}, time={1}",
                                key,
                                DateFormat.getDateTimeInstance().format(Date()),
                            )
                        }
                    },
                ).build()

        var start = System.currentTimeMillis()
        for (index in 0 until 100) {
            val key = "key-$index"
            println(MessageFormat.format("result: key={0}, value={1}", key, cache.get(key)))
        }
        println("cost ${System.currentTimeMillis() - start}")

        start = System.currentTimeMillis()
        for (index in 0 until 100) {
            val key = "key2-$index"
            println(MessageFormat.format("result: key={0}, value={1}", key, cache.getIfPresent(key)))
        }
        println("cost2 ${System.currentTimeMillis() - start}")

        start = System.currentTimeMillis()
        for (index in 0 until 100) {
            val key = "key1-$index"
            println(MessageFormat.format("result: key={0}, value={1}", key, cache.getIfPresent(key)))
        }
        println("cost3 ${System.currentTimeMillis() - start}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CacheToolTest::class.java)
    }
}
