package com.ruishanio.taskpilot.tool.captcha

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.geom.CubicCurve2D
import java.awt.geom.Line2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BandCombineOp
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.Kernel
import java.security.SecureRandom
import java.util.Random

/**
 * 验证码工具。
 *
 * 通过链式配置生成验证码，尽量把参数校验和图像变换细节封装在单一入口内。
 */
class CaptchaTool {

    // 文本生成器允许被外部显式置空，真正使用时再统一做参数校验。
    private var textCreator: TextCreator? = DefaultTextCreator()

    // 图片基础参数。
    private var width: Int? = 180
    private var height: Int? = 60
    private var colors: List<Color>? = listOf(
        Color(0xb83b5e),
        Color(0xf08a5d),
        Color(0xff9a00),
        Color(0x00b8a9),
        Color(0x004a7c),
        Color(0x3d84a8),
        Color(0x521262),
    )
    private var fontSize: Int? = 40
    private var fonts: List<Font>? = listOf(
        Font("Arial", Font.BOLD, 40),
        Font("Courier", Font.BOLD, 40),
    )
    private var charSpace: Int? = 8
    private var backgroundColorFrom: Color? = Color.LIGHT_GRAY
    private var backgroundColorTo: Color? = Color.WHITE
    private var isBorderDrawn: Boolean? = false
    private var borderColor: Color? = Color.WHITE
    private var borderThickness: Int? = 1
    private var noiseColor: Color? = Color.WHITE

    // 保留多种扭曲策略，运行时随机挑选。
    private var distortedEngines: List<DistortedEngine>? = listOf(
        NoneDistorted(),
        ShadowDistorted(),
        WaterRippleDistorted(),
        FishEyeDistorted(),
        RippleDistorted(),
    )

    fun getTextCreator(): TextCreator? = textCreator

    fun setTextCreator(textCreator: TextCreator?): CaptchaTool {
        this.textCreator = textCreator
        return this
    }

    fun getWidth(): Int? = width

    fun setWidth(width: Int?): CaptchaTool {
        this.width = width
        return this
    }

    fun getHeight(): Int? = height

    fun setHeight(height: Int?): CaptchaTool {
        this.height = height
        return this
    }

    fun getColors(): List<Color>? = colors

    fun setColors(colors: List<Color>?): CaptchaTool {
        this.colors = colors
        return this
    }

    fun getFontSize(): Int? = fontSize

    fun setFontSize(fontSize: Int?): CaptchaTool {
        this.fontSize = fontSize
        return this
    }

    fun getFonts(): List<Font>? = fonts

    fun setFonts(fonts: List<Font>?): CaptchaTool {
        this.fonts = fonts
        return this
    }

    fun getCharSpace(): Int? = charSpace

    fun setCharSpace(charSpace: Int?): CaptchaTool {
        this.charSpace = charSpace
        return this
    }

    fun getBackgroundColorFrom(): Color? = backgroundColorFrom

    fun setBackgroundColorFrom(backgroundColorFrom: Color?): CaptchaTool {
        this.backgroundColorFrom = backgroundColorFrom
        return this
    }

    fun getBackgroundColorTo(): Color? = backgroundColorTo

    fun setBackgroundColorTo(backgroundColorTo: Color?): CaptchaTool {
        this.backgroundColorTo = backgroundColorTo
        return this
    }

    fun getIsBorderDrawn(): Boolean? = isBorderDrawn

    fun setIsBorderDrawn(isBorderDrawn: Boolean?): CaptchaTool {
        this.isBorderDrawn = isBorderDrawn
        return this
    }

    fun getBorderColor(): Color? = borderColor

    fun setBorderColor(borderColor: Color?): CaptchaTool {
        this.borderColor = borderColor
        return this
    }

    fun getBorderThickness(): Int? = borderThickness

    fun setBorderThickness(borderThickness: Int?): CaptchaTool {
        this.borderThickness = borderThickness
        return this
    }

    fun getNoiseColor(): Color? = noiseColor

    fun setNoiseColor(noiseColor: Color?): CaptchaTool {
        this.noiseColor = noiseColor
        return this
    }

    fun getDistortedEngines(): List<DistortedEngine>? = distortedEngines

    fun setDistortedEngines(distortedEngines: List<DistortedEngine>?): CaptchaTool {
        this.distortedEngines = distortedEngines
        return this
    }

    /**
     * 生成验证码文本。
     */
    fun createText(): TextResult {
        val finalTextCreator = requireNotNullValue(textCreator, "textCreator is null")
        return finalTextCreator.create()
    }

    /**
     * 根据文本渲染验证码图片。
     *
     * 这里保持原实现的校验顺序，方便外部在参数不完整时拿到和 Java 版本一致的异常语义。
     */
    fun createImage(textResult: TextResult?): BufferedImage {
        val finalTextResult = requireNotNullValue(textResult, "textResult is null")

        val finalWidth = requireNotNullValue(width, "width is null")
        val finalHeight = requireNotNullValue(height, "height is null")
        val finalBackgroundColorFrom = requireNotNullValue(backgroundColorFrom, "backgroundColorFrom is null")
        val finalBackgroundColorTo = requireNotNullValue(backgroundColorTo, "backgroundColorTo is null")
        val finalColors = requireNotNullValue(colors, "color is null")
        val finalFontSize = requireNotNullValue(fontSize, "fontSize is null")
        val finalCharSpace = requireNotNullValue(charSpace, "charSpace is null")
        val finalFonts = requireNotNullValue(fonts, "fonts is null")
        val finalIsBorderDrawn = requireNotNullValue(isBorderDrawn, "isBorderDrawn is null")
        val finalBorderColor = requireNotNullValue(borderColor, "borderColor is null")
        val finalBorderThickness = requireNotNullValue(borderThickness, "borderThickness is null")
        val finalDistortedEngines = requireNotNullValue(distortedEngines, "distortedEngine is null")
        val finalNoiseColor = requireNotNullValue(noiseColor, "noiseColor is null")

        val text = finalTextResult.getText()
        val color = if (finalColors.size == 1) finalColors[0] else finalColors[Random().nextInt(finalColors.size)]
        val font = if (finalFonts.size == 1) finalFonts[0] else finalFonts[Random().nextInt(finalFonts.size)]

        var image = renderWord(text, finalWidth, finalHeight, finalFontSize, font, color, finalCharSpace)

        val distortedEngine =
            if (finalDistortedEngines.size == 1) {
                finalDistortedEngines[0]
            } else {
                finalDistortedEngines[Random().nextInt(finalDistortedEngines.size)]
            }
        image = distortedEngine.getDistortedImage(image)

        makeNoise(image, finalNoiseColor, .1f, .1f, .25f, .25f)
        makeNoise(image, finalNoiseColor, .1f, .25f, .5f, .9f)

        image = addBackground(image, finalBackgroundColorFrom, finalBackgroundColorTo)
        if (finalIsBorderDrawn) {
            drawBox(image, finalBorderColor, finalBorderThickness, finalWidth, finalHeight)
        }
        return image
    }

    /**
     * 将文本居中绘制到透明画布上。
     */
    private fun renderWord(
        word: String,
        width: Int,
        height: Int,
        fontSize: Int,
        font: Font,
        color: Color,
        charSpace: Int,
    ): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = color

            val hints = RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            hints.add(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
            graphics.setRenderingHints(hints)

            val fontRenderContext: FontRenderContext = graphics.fontRenderContext
            val startPosY = (height - fontSize) / 5 + fontSize

            val wordChars = word.toCharArray()
            val chosenFonts = Array(wordChars.size) { font }
            val charWidths = IntArray(wordChars.size)
            var widthNeeded = 0

            for (index in wordChars.indices) {
                val charToDraw = charArrayOf(wordChars[index])
                val glyphVector: GlyphVector = chosenFonts[index].createGlyphVector(fontRenderContext, charToDraw)
                charWidths[index] = glyphVector.visualBounds.width.toInt()
                if (index > 0) {
                    widthNeeded += charSpace
                }
                widthNeeded += charWidths[index]
            }

            var startPosX = (width - widthNeeded) / 2
            for (index in wordChars.indices) {
                graphics.font = chosenFonts[index]
                val charToDraw = charArrayOf(wordChars[index])
                graphics.drawChars(charToDraw, 0, charToDraw.size, startPosX, startPosY)
                startPosX += charWidths[index] + charSpace
            }
        } finally {
            graphics.dispose()
        }
        return image
    }

    /**
     * 通过贝塞尔曲线绘制噪声线，保留原实现的随机折线风格。
     */
    private fun makeNoise(
        image: BufferedImage,
        noiseColor: Color,
        factorOne: Float,
        factorTwo: Float,
        factorThree: Float,
        factorFour: Float,
    ) {
        val width = image.width
        val height = image.height
        val random = SecureRandom()

        val curve = CubicCurve2D.Float(
            width * factorOne,
            height * random.nextFloat(),
            width * factorTwo,
            height * random.nextFloat(),
            width * factorThree,
            height * random.nextFloat(),
            width * factorFour,
            height * random.nextFloat(),
        )

        val iterator = curve.getPathIterator(null, 2.0)
        val points = mutableListOf<Point2D>()
        while (!iterator.isDone) {
            val coords = FloatArray(6)
            when (iterator.currentSegment(coords)) {
                PathIterator.SEG_MOVETO,
                PathIterator.SEG_LINETO,
                -> points += Point2D.Float(coords[0], coords[1])
            }
            iterator.next()
        }

        val graphics = image.graphics as Graphics2D
        try {
            graphics.setRenderingHints(
                RenderingHints(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                ),
            )
            graphics.color = noiseColor
            for (index in 0 until (points.size - 1)) {
                if (index < 3) {
                    graphics.stroke = BasicStroke(0.9f * (4 - index))
                }
                val start = points[index]
                val end = points[index + 1]
                graphics.drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())
            }
        } finally {
            graphics.dispose()
        }
    }

    /**
     * 追加渐变背景，并把前景文字图层叠回去。
     */
    private fun addBackground(baseImage: BufferedImage, colorFrom: Color, colorTo: Color): BufferedImage {
        val width = baseImage.width
        val height = baseImage.height
        val imageWithBackground = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = imageWithBackground.graphics as Graphics2D
        try {
            val hints = RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            hints.add(RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY))
            hints.add(
                RenderingHints(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
                ),
            )
            hints.add(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
            graphics.setRenderingHints(hints)

            graphics.paint = GradientPaint(0f, 0f, colorFrom, width.toFloat(), height.toFloat(), colorTo)
            graphics.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
            graphics.drawImage(baseImage, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        return imageWithBackground
    }

    /**
     * 绘制边框。
     *
     * 保留原实现的线段计算方式，避免在重构时引入视觉差异。
     */
    private fun drawBox(baseImage: BufferedImage, borderColor: Color, borderThickness: Int, width: Int, height: Int) {
        val graphics = baseImage.createGraphics()
        try {
            graphics.color = borderColor
            if (borderThickness != 1) {
                graphics.stroke = BasicStroke(borderThickness.toFloat())
            }

            graphics.draw(Line2D.Double(0.0, 0.0, 0.0, width.toDouble()))
            graphics.draw(Line2D.Double(0.0, 0.0, width.toDouble(), 0.0))
            graphics.draw(Line2D.Double(0.0, (height - 1).toDouble(), width.toDouble(), (height - 1).toDouble()))
            graphics.draw(Line2D.Double((width - 1).toDouble(), (height - 1).toDouble(), (width - 1).toDouble(), 0.0))
        } finally {
            graphics.dispose()
        }
    }

    private fun <T> requireNotNullValue(value: T?, errorMessage: String): T {
        assertNotNull(value, errorMessage)
        return value!!
    }

    /**
     * 文本生成器接口。
     */
    interface TextCreator {
        fun create(): TextResult
    }

    /**
     * 文本与校验结果的承载体。
     */
    class TextResult(
        private val text: String,
        private val result: String,
    ) {
        fun getText(): String = text

        fun getResult(): String = result

        override fun toString(): String =
            "TextCreatorResult{text='$text', result='$result'}"
    }

    /**
     * 默认随机字符验证码生成器。
     */
    class DefaultTextCreator : TextCreator {
        private val length: Int
        private val str: String

        constructor() : this(4)

        constructor(length: Int) : this(length, TEXT_NUMBER_AND_LOWERCASE)

        constructor(str: String) : this(4, str)

        constructor(length: Int, str: String) {
            this.length = length
            this.str = str
        }

        override fun create(): TextResult = create(length)

        /**
         * 按指定长度从候选字符集中随机抽样。
         */
        fun create(length: Int): TextResult {
            val chars = str.toCharArray()
            val random = Random()
            val text = StringBuilder()
            repeat(length) {
                text.append(chars[random.nextInt(chars.size)])
            }
            val result = text.toString()
            return TextResult(result, result)
        }
    }

    /**
     * 算术验证码生成器。
     */
    class ArithmeticTextCreator : TextCreator {
        override fun create(): TextResult {
            val random = Random()
            val x = random.nextInt(10)
            val y = random.nextInt(10)

            val text = StringBuilder()
            val result: Int

            when (random.nextInt(4)) {
                0 -> {
                    text.append(NUMBER[x]).append("+").append(NUMBER[y])
                    result = x + y
                }

                1 -> {
                    if (x >= y) {
                        text.append(NUMBER[x]).append("-").append(NUMBER[y])
                        result = x - y
                    } else {
                        text.append(NUMBER[y]).append("-").append(NUMBER[x])
                        result = y - x
                    }
                }

                2 -> {
                    text.append(NUMBER[x]).append("*").append(NUMBER[y])
                    result = x * y
                }

                else -> {
                    if (x != 0) {
                        text.append(NUMBER[y]).append("/").append(NUMBER[x])
                        result = y / x
                    } else {
                        text.append(NUMBER[x]).append("+").append(NUMBER[y])
                        result = x + y
                    }
                }
            }
            text.append("=?")
            return TextResult(text.toString(), result.toString())
        }

        companion object {
            private val NUMBER = "0,1,2,3,4,5,6,7,8,9,10".split(",").toTypedArray()
        }
    }

    /**
     * 图片扭曲引擎接口。
     */
    interface DistortedEngine {
        fun getDistortedImage(baseImage: BufferedImage): BufferedImage
    }

    /**
     * 不做扭曲的直通实现。
     */
    class NoneDistorted : DistortedEngine {
        override fun getDistortedImage(baseImage: BufferedImage): BufferedImage = baseImage
    }

    /**
     * 鱼眼扭曲。
     */
    class FishEyeDistorted : DistortedEngine {
        override fun getDistortedImage(baseImage: BufferedImage): BufferedImage {
            val imageHeight = baseImage.height
            val imageWidth = baseImage.width

            val pixels = IntArray(imageHeight * imageWidth)
            var index = 0
            for (x in 0 until imageWidth) {
                for (y in 0 until imageHeight) {
                    pixels[index] = baseImage.getRGB(x, y)
                    index++
                }
            }

            val distance = randomInt(imageWidth / 4, imageWidth / 3).toDouble()
            val widthMiddle = baseImage.width / 2
            val heightMiddle = baseImage.height / 2

            for (x in 0 until baseImage.width) {
                for (y in 0 until baseImage.height) {
                    val relX = x - widthMiddle
                    val relY = y - heightMiddle
                    val d1 = Math.sqrt((relX * relX + relY * relY).toDouble())
                    if (d1 < distance) {
                        val j2 =
                            widthMiddle +
                                (((fishEyeFormula(d1 / distance) * distance) / d1) * (x - widthMiddle)).toInt()
                        val k2 =
                            heightMiddle +
                                (((fishEyeFormula(d1 / distance) * distance) / d1) * (y - heightMiddle)).toInt()
                        baseImage.setRGB(x, y, pixels[j2 * imageHeight + k2])
                    }
                }
            }

            return baseImage
        }

        private fun randomInt(i: Int, j: Int): Int {
            val d = Math.random()
            return (i + ((j - i) + 1) * d).toInt()
        }

        private fun fishEyeFormula(s: Double): Double =
            when {
                s < 0.0 -> 0.0
                s > 1.0 -> s
                else -> -0.75 * s * s * s + 1.5 * s * s + 0.25 * s
            }
    }

    /**
     * 水波纹扭曲。
     */
    class WaterRippleDistorted : DistortedEngine {
        override fun getDistortedImage(baseImage: BufferedImage): BufferedImage {
            val effectImage = waterFilter(baseImage)
            val distortedImage = BufferedImage(baseImage.width, baseImage.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = distortedImage.graphics as Graphics2D
            try {
                graphics.drawImage(effectImage, 0, 0, null)
            } finally {
                graphics.dispose()
            }
            return distortedImage
        }

        /**
         * 对中心区域做圆形波纹偏移。
         */
        fun waterFilter(src: BufferedImage): BufferedImage {
            val amplitude = 1.5f
            val phase = 10f
            val wavelength = 2f
            val centreX = 0.5f
            val centreY = 0.5f
            val radius = 50f

            val icentreX = src.width * centreX
            val icentreY = src.height * centreY
            val radius2 = radius * radius

            val width = src.width
            val height = src.height
            val transformedSpace = Rectangle(0, 0, width, height)

            val dstCM: ColorModel = src.colorModel
            val dst =
                BufferedImage(
                    dstCM,
                    dstCM.createCompatibleWritableRaster(transformedSpace.width, transformedSpace.height),
                    dstCM.isAlphaPremultiplied,
                    null,
                )

            val inPixels = getRGB(src, 0, 0, width, height, null)
            val srcWidth1 = width - 1
            val srcHeight1 = height - 1
            val outWidth = transformedSpace.width
            val outHeight = transformedSpace.height
            val outPixels = IntArray(outWidth)

            val outX = transformedSpace.x
            val outY = transformedSpace.y
            val out = FloatArray(2)

            for (y in 0 until outHeight) {
                for (x in 0 until outWidth) {
                    transformInverse4water(
                        outX + x,
                        outY + y,
                        out,
                        icentreX,
                        icentreY,
                        radius2,
                        amplitude,
                        phase,
                        wavelength,
                        radius,
                    )

                    val srcX = Math.floor(out[0].toDouble()).toInt()
                    val srcY = Math.floor(out[1].toDouble()).toInt()
                    val xWeight = out[0] - srcX
                    val yWeight = out[1] - srcY

                    val nw: Int
                    val ne: Int
                    val sw: Int
                    val se: Int

                    if (srcX >= 0 && srcX < srcWidth1 && srcY >= 0 && srcY < srcHeight1) {
                        val index = width * srcY + srcX
                        nw = inPixels[index]
                        ne = inPixels[index + 1]
                        sw = inPixels[index + width]
                        se = inPixels[index + width + 1]
                    } else {
                        nw = getPixel(inPixels, srcX, srcY, width, height)
                        ne = getPixel(inPixels, srcX + 1, srcY, width, height)
                        sw = getPixel(inPixels, srcX, srcY + 1, width, height)
                        se = getPixel(inPixels, srcX + 1, srcY + 1, width, height)
                    }

                    outPixels[x] = bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se)
                }
                setRGB(dst, 0, y, transformedSpace.width, 1, outPixels)
            }
            return dst
        }

        /**
         * 反向求取水波纹映射坐标，避免采样时出现空洞。
         */
        protected fun transformInverse4water(
            x: Int,
            y: Int,
            out: FloatArray,
            icentreX: Float,
            icentreY: Float,
            radius2: Float,
            amplitude: Float,
            phase: Float,
            wavelength: Float,
            radius: Float,
        ) {
            val dx = x - icentreX
            val dy = y - icentreY
            val distance2 = dx * dx + dy * dy

            if (distance2 > radius2) {
                out[0] = x.toFloat()
                out[1] = y.toFloat()
            } else {
                val distance = Math.sqrt(distance2.toDouble()).toFloat()
                var amount =
                    amplitude * Math.sin((distance / wavelength * TWO_PI - phase).toDouble()).toFloat()
                amount *= (radius - distance) / radius
                if (distance != 0f) {
                    amount *= wavelength / distance
                }

                out[0] = x + dx * amount
                out[1] = y + dy * amount
            }
        }

        companion object {
            const val TWO_PI: Float = (Math.PI * 2.0).toFloat()

            /**
             * 直接从底层 raster 读写整数像素，减少 `BufferedImage` 在热点循环中的额外开销。
             */
            fun getRGB(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, pixels: IntArray?): IntArray {
                val type = image.type
                return if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
                    image.raster.getDataElements(x, y, width, height, pixels) as IntArray
                } else {
                    image.getRGB(x, y, width, height, pixels, 0, width)
                }
            }
            fun setRGB(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, pixels: IntArray) {
                val type = image.type
                if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
                    image.raster.setDataElements(x, y, width, height, pixels)
                } else {
                    image.setRGB(x, y, width, height, pixels, 0, width)
                }
            }
            fun getPixel(pixels: IntArray, x: Int, y: Int, width: Int, height: Int): Int {
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return pixels[clamp(y, 0, height - 1) * width + clamp(x, 0, width - 1)]
                }
                return pixels[y * width + x]
            }
            fun createCompatibleDestImage(src: BufferedImage): BufferedImage {
                val dstCM = src.colorModel
                return BufferedImage(
                    dstCM,
                    dstCM.createCompatibleWritableRaster(src.width, src.height),
                    dstCM.isAlphaPremultiplied,
                    null,
                )
            }
            fun clamp(x: Int, a: Int, b: Int): Int = if (x < a) a else minOf(x, b)
            fun clamp(c: Int): Int = if (c < 0) 0 else minOf(c, 255)

            /**
             * 双线性插值用于在扭曲采样时平滑过渡四个邻近像素。
             */
            fun bilinearInterpolate(x: Float, y: Float, nw: Int, ne: Int, sw: Int, se: Int): Int {
                var m0: Float
                var m1: Float

                val a0 = nw shr 24 and 0xff
                val r0 = nw shr 16 and 0xff
                val g0 = nw shr 8 and 0xff
                val b0 = nw and 0xff

                val a1 = ne shr 24 and 0xff
                val r1 = ne shr 16 and 0xff
                val g1 = ne shr 8 and 0xff
                val b1 = ne and 0xff

                val a2 = sw shr 24 and 0xff
                val r2 = sw shr 16 and 0xff
                val g2 = sw shr 8 and 0xff
                val b2 = sw and 0xff

                val a3 = se shr 24 and 0xff
                val r3 = se shr 16 and 0xff
                val g3 = se shr 8 and 0xff
                val b3 = se and 0xff

                val cx = 1.0f - x
                val cy = 1.0f - y

                m0 = cx * a0 + x * a1
                m1 = cx * a2 + x * a3
                val a = (cy * m0 + y * m1).toInt()

                m0 = cx * r0 + x * r1
                m1 = cx * r2 + x * r3
                val r = (cy * m0 + y * m1).toInt()

                m0 = cx * g0 + x * g1
                m1 = cx * g2 + x * g3
                val g = (cy * m0 + y * m1).toInt()

                m0 = cx * b0 + x * b1
                m1 = cx * b2 + x * b3
                val b = (cy * m0 + y * m1).toInt()

                return (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    /**
     * 阴影扭曲。
     */
    class ShadowDistorted : DistortedEngine {
        override fun getDistortedImage(baseImage: BufferedImage): BufferedImage {
            val effectImage = shadowFilter(baseImage)
            val distortedImage = BufferedImage(baseImage.width, baseImage.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = distortedImage.graphics as Graphics2D
            try {
                graphics.drawImage(effectImage, 0, 0, null)
            } finally {
                graphics.dispose()
            }
            return distortedImage
        }

        /**
         * 先抽取 alpha，再做高斯模糊，最后把阴影回贴到原图。
         */
        private fun shadowFilter(src: BufferedImage): BufferedImage {
            val angle = (Math.PI * 6 / 4).toFloat()
            val radius = 10f
            val distance = 5f
            val opacity = 1f

            val width = src.width
            val height = src.height

            val xOffset = distance * Math.cos(angle.toDouble()).toFloat()
            val yOffset = -distance * Math.sin(angle.toDouble()).toFloat()

            val shadowR = 0 / 255f
            val shadowG = 0 / 255f
            val shadowB = 0 / 255f

            val extractAlpha =
                arrayOf(
                    floatArrayOf(0f, 0f, 0f, shadowR),
                    floatArrayOf(0f, 0f, 0f, shadowG),
                    floatArrayOf(0f, 0f, 0f, shadowB),
                    floatArrayOf(0f, 0f, 0f, opacity),
                )

            var shadow = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            BandCombineOp(extractAlpha, null).filter(src.raster, shadow.raster)
            shadow = gaussianFilter(shadow, radius)

            val dst = WaterRippleDistorted.createCompatibleDestImage(src)
            val graphics2D = dst.createGraphics()
            try {
                graphics2D.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
                graphics2D.drawRenderedImage(
                    shadow,
                    AffineTransform.getTranslateInstance(xOffset.toDouble(), yOffset.toDouble()),
                )
                graphics2D.composite = AlphaComposite.SrcOver
                graphics2D.drawRenderedImage(src, AffineTransform())
            } finally {
                graphics2D.dispose()
            }
            return dst
        }

        /**
         * 进行双向高斯卷积，和原实现保持一致。
         */
        fun gaussianFilter(src: BufferedImage, radius: Float): BufferedImage {
            val width = src.width
            val height = src.height

            val inPixels = IntArray(width * height)
            val outPixels = IntArray(width * height)
            src.getRGB(0, 0, width, height, inPixels, 0, width)

            if (radius > 0) {
                val kernel = makeKernel(radius)
                convolveAndTranspose(kernel, inPixels, outPixels, width, height, true, false)
                convolveAndTranspose(kernel, outPixels, inPixels, height, width, false, true)
            }

            val dst = WaterRippleDistorted.createCompatibleDestImage(src)
            dst.setRGB(0, 0, width, height, inPixels, 0, width)
            return dst
        }

        companion object {
            fun makeKernel(radius: Float): Kernel {
                val r = Math.ceil(radius.toDouble()).toInt()
                val rows = r * 2 + 1
                val matrix = FloatArray(rows)
                val sigma = radius / 3
                val sigma22 = 2 * sigma * sigma
                val sigmaPi2 = 2 * Math.PI.toFloat() * sigma
                val sqrtSigmaPi2 = Math.sqrt(sigmaPi2.toDouble()).toFloat()
                val radius2 = radius * radius
                var total = 0f
                var index = 0

                for (row in -r..r) {
                    val distance = (row * row).toFloat()
                    matrix[index] =
                        if (distance > radius2) {
                            0f
                        } else {
                            Math.exp((-(distance) / sigma22).toDouble()).toFloat() / sqrtSigmaPi2
                        }
                    total += matrix[index]
                    index++
                }

                for (i in matrix.indices) {
                    matrix[i] /= total
                }
                return Kernel(rows, 1, matrix)
            }

            /**
             * 保留原有的转置卷积写法，减少中间数组和边界拷贝。
             */
            fun convolveAndTranspose(
                kernel: Kernel,
                inPixels: IntArray,
                outPixels: IntArray,
                width: Int,
                height: Int,
                premultiply: Boolean,
                unpremultiply: Boolean,
            ) {
                val matrix = kernel.getKernelData(null)
                val cols = kernel.width
                val cols2 = cols / 2

                for (y in 0 until height) {
                    var index = y
                    val ioffset = y * width
                    for (x in 0 until width) {
                        var r = 0f
                        var g = 0f
                        var b = 0f
                        var a = 0f

                        for (col in -cols2..cols2) {
                            val factor = matrix[cols2 + col]
                            if (factor != 0f) {
                                var ix = x + col
                                if (ix < 0) {
                                    ix = 0
                                } else if (ix >= width) {
                                    ix = width - 1
                                }

                                val rgb = inPixels[ioffset + ix]
                                val pa = rgb shr 24 and 0xff
                                var pr = rgb shr 16 and 0xff
                                var pg = rgb shr 8 and 0xff
                                var pb = rgb and 0xff

                                if (premultiply) {
                                    val a255 = pa * (1.0f / 255.0f)
                                    pr *= a255.toInt()
                                    pg *= a255.toInt()
                                    pb *= a255.toInt()
                                }

                                a += factor * pa
                                r += factor * pr
                                g += factor * pg
                                b += factor * pb
                            }
                        }

                        if (unpremultiply && a != 0f && a != 255f) {
                            val factor = 255.0f / a
                            r *= factor
                            g *= factor
                            b *= factor
                        }

                        val ia = WaterRippleDistorted.clamp((a + 0.5f).toInt())
                        val ir = WaterRippleDistorted.clamp((r + 0.5f).toInt())
                        val ig = WaterRippleDistorted.clamp((g + 0.5f).toInt())
                        val ib = WaterRippleDistorted.clamp((b + 0.5f).toInt())

                        outPixels[index] = (ia shl 24) or (ir shl 16) or (ig shl 8) or ib
                        index += height
                    }
                }
            }
        }
    }

    /**
     * 波纹扭曲。
     */
    class RippleDistorted : DistortedEngine {
        override fun getDistortedImage(baseImage: BufferedImage): BufferedImage {
            val effectImage = rippleFilter(baseImage)
            val distortedImage = BufferedImage(baseImage.width, baseImage.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = distortedImage.graphics as Graphics2D
            try {
                graphics.drawImage(effectImage, 0, 0, null)
            } finally {
                graphics.dispose()
            }
            return distortedImage
        }

        /**
         * 分别在 X/Y 方向叠加正弦偏移，制造条纹波纹效果。
         */
        fun rippleFilter(src: BufferedImage): BufferedImage {
            val random = Random()
            val xAmplitude = 7.6f
            val yAmplitude = random.nextFloat() + 1.0f
            val xWavelength = (random.nextInt(7) + 8).toFloat()
            val yWavelength = (random.nextInt(3) + 2).toFloat()

            val width = src.width
            val height = src.height
            val transformedSpace = Rectangle(0, 0, width, height)

            val dstCM = src.colorModel
            val dst =
                BufferedImage(
                    dstCM,
                    dstCM.createCompatibleWritableRaster(transformedSpace.width, transformedSpace.height),
                    dstCM.isAlphaPremultiplied,
                    null,
                )

            val inPixels = WaterRippleDistorted.getRGB(src, 0, 0, width, height, null)
            val srcWidth1 = width - 1
            val srcHeight1 = height - 1
            val outWidth = transformedSpace.width
            val outHeight = transformedSpace.height
            val outPixels = IntArray(outWidth)
            val outX = transformedSpace.x
            val outY = transformedSpace.y
            val out = FloatArray(2)

            for (y in 0 until outHeight) {
                for (x in 0 until outWidth) {
                    transformInverseForRipple(
                        outX + x,
                        outY + y,
                        out,
                        xWavelength,
                        yWavelength,
                        xAmplitude,
                        yAmplitude,
                    )

                    val srcX = Math.floor(out[0].toDouble()).toInt()
                    val srcY = Math.floor(out[1].toDouble()).toInt()
                    val xWeight = out[0] - srcX
                    val yWeight = out[1] - srcY

                    val nw: Int
                    val ne: Int
                    val sw: Int
                    val se: Int

                    if (srcX >= 0 && srcX < srcWidth1 && srcY >= 0 && srcY < srcHeight1) {
                        val index = width * srcY + srcX
                        nw = inPixels[index]
                        ne = inPixels[index + 1]
                        sw = inPixels[index + width]
                        se = inPixels[index + width + 1]
                    } else {
                        nw = WaterRippleDistorted.getPixel(inPixels, srcX, srcY, width, height)
                        ne = WaterRippleDistorted.getPixel(inPixels, srcX + 1, srcY, width, height)
                        sw = WaterRippleDistorted.getPixel(inPixels, srcX, srcY + 1, width, height)
                        se = WaterRippleDistorted.getPixel(inPixels, srcX + 1, srcY + 1, width, height)
                    }

                    outPixels[x] = WaterRippleDistorted.bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se)
                }
                WaterRippleDistorted.setRGB(dst, 0, y, transformedSpace.width, 1, outPixels)
            }
            return dst
        }

        /**
         * 反向计算波纹扭曲坐标。
         */
        protected fun transformInverseForRipple(
            x: Int,
            y: Int,
            out: FloatArray,
            xWavelength: Float,
            yWavelength: Float,
            xAmplitude: Float,
            yAmplitude: Float,
        ) {
            val nx = y.toFloat() / xWavelength
            val ny = x.toFloat() / yWavelength
            val fx = Math.sin(nx.toDouble()).toFloat()
            val fy = Math.sin(ny.toDouble()).toFloat()

            out[0] = x + xAmplitude * fx
            out[1] = y + yAmplitude * fy
        }
    }

    companion object {
        const val TEXT_NUMBER: String = "0123456789"
        const val TEXT_LOWERCASE: String = "abcdefghijklmnopqrstuvwxyz"
        const val TEXT_NUMBER_AND_LOWERCASE: String = "0123456789abcdefghijklmnopqrstuvwxyz"

        /**
         * 统一通过工厂方法创建实例，避免调用方直接依赖构造细节。
         */
        fun build(): CaptchaTool = CaptchaTool()

        /**
         * 集中处理参数非空校验，避免每个构造分支重复拼装异常信息。
         */
        fun assertNotNull(`object`: Any?, errorMessage: String) {
            if (`object` == null) {
                throw IllegalArgumentException(errorMessage)
            }
        }
    }
}
