package com.ruishanio.taskpilot.tool.test.id

import com.ruishanio.taskpilot.tool.id.RandomIdTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * RandomIdTool 生成结果验证。
 */
class RandomIdToolTest {
    @Test
    fun test() {
        logger.info("getDigitId = {}", RandomIdTool.getDigitId())
        logger.info("getDigitId = {}", RandomIdTool.getDigitId(10))

        logger.info("getLowercaseId = {}", RandomIdTool.getLowercaseId())
        logger.info("getLowercaseId = {}", RandomIdTool.getLowercaseId(10))

        logger.info("getUppercaseId = {}", RandomIdTool.getUppercaseId())
        logger.info("getUppercaseId = {}", RandomIdTool.getUppercaseId(10))

        logger.info("getAlphaNumeric = {}", RandomIdTool.getAlphaNumeric())
        logger.info("getAlphaNumeric = {}", RandomIdTool.getAlphaNumeric(10))

        logger.info("getAlphaNumericWithSpecial = {}", RandomIdTool.getAlphaNumericWithSpecial())
        logger.info("getAlphaNumericWithSpecial = {}", RandomIdTool.getAlphaNumericWithSpecial(10))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RandomIdToolTest::class.java)
    }
}
