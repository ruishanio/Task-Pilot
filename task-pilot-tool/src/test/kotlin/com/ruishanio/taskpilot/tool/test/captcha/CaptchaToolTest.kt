package com.ruishanio.taskpilot.tool.test.captcha

import com.ruishanio.taskpilot.tool.captcha.CaptchaTool
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * CaptchaTool 生成验证码文本和图片的基本验证。
 */
class CaptchaToolTest {
    @Test
    fun test1() {
        val captchaTool = CaptchaTool.build()

        val textResult = captchaTool.createText()
        logger.info("text: {}", textResult.getText())

        val image = captchaTool.createImage(textResult)
        assertFalse(renderPng(image).isEmpty())
    }

    @Test
    fun test99() {
        val captchaTool =
            CaptchaTool.build()
                .setTextCreator(CaptchaTool.DefaultTextCreator(6))
                .setWidth(180)
                .setHeight(60)
                .setColors(
                    listOf(
                        Color(0xb83b5e),
                        Color(0xf08a5d),
                        Color(0xff9a00),
                        Color(0x00b8a9),
                        Color(0x004a7c),
                        Color(0x3d84a8),
                        Color(0x521262),
                    ),
                ).setFontSize(40)
                .setFonts(listOf(Font("Arial", Font.BOLD, 40), Font("Courier", Font.BOLD, 40)))
                .setCharSpace(8)
                .setBackgroundColorFrom(Color.LIGHT_GRAY)
                .setBackgroundColorTo(Color.WHITE)
                .setIsBorderDrawn(true)
                .setBorderColor(Color.WHITE)
                .setBorderThickness(1)
                .setNoiseColor(Color.WHITE)
                .setDistortedEngines(
                    listOf(
                        CaptchaTool.NoneDistorted(),
                        CaptchaTool.ShadowDistorted(),
                        CaptchaTool.WaterRippleDistorted(),
                        CaptchaTool.FishEyeDistorted(),
                        CaptchaTool.RippleDistorted(),
                    ),
                )

        val textResult = captchaTool.createText()
        logger.info("text: {}", textResult.getText())
        logger.info("result: {}", textResult.getResult())

        val image = captchaTool.createImage(textResult)
        assertFalse(renderPng(image).isEmpty())
    }

    @Test
    fun test3() {
        val captchaTool = CaptchaTool.build().setTextCreator(CaptchaTool.ArithmeticTextCreator())

        val textResult = captchaTool.createText()
        logger.info("text: {}", textResult.getText())
        logger.info("result: {}", textResult.getResult())

        val image = captchaTool.createImage(textResult)
        assertFalse(renderPng(image).isEmpty())
    }

    @Test
    fun test4() {
        repeat(10) { index ->
            val captchaTool = CaptchaTool.build().setTextCreator(CaptchaTool.ArithmeticTextCreator())
            val textResult = captchaTool.createText()
            assertFalse(renderPng(captchaTool.createImage(textResult)).isEmpty(), "captcha-4-$index should be writable")
        }
    }

    @Test
    fun test5() {
        val captchaTool = CaptchaTool.build().setTextCreator(CaptchaTool.DefaultTextCreator(6))

        val textResult = captchaTool.createText()
        logger.info("text: {}", textResult.getText())
        logger.info("result: {}", textResult.getResult())

        val image = captchaTool.createImage(textResult)
        assertFalse(renderPng(image).isEmpty())
    }

    @Test
    fun test6() {
        val captchaTool =
            CaptchaTool.build().setTextCreator(CaptchaTool.DefaultTextCreator("物华天宝人杰地灵山清水秀景色宜人"))

        val textResult = captchaTool.createText()
        logger.info("text: {}", textResult.getText())
        logger.info("result: {}", textResult.getResult())

        val image = captchaTool.createImage(textResult)
        assertFalse(renderPng(image).isEmpty())
    }

    /**
     * 改为写入内存字节流，避免测试依赖开发机的固定目录结构。
     */
    private fun renderPng(image: java.awt.image.BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CaptchaToolTest::class.java)
    }
}
