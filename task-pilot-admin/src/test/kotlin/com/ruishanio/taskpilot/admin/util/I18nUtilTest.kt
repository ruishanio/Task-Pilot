package com.ruishanio.taskpilot.admin.util

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

/**
 * 验证国际化工具的单 key 与批量读取入口。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class I18nUtilTest {
    @Test
    fun test() {
        logger.info(I18nUtil.getString("admin_name"))
        logger.info(I18nUtil.getMultString("admin_name", "admin_name_full"))
        logger.info(I18nUtil.getMultString())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(I18nUtilTest::class.java)
    }
}
