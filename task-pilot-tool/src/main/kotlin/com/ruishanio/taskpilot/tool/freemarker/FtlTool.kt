package com.ruishanio.taskpilot.tool.freemarker

import freemarker.core.TemplateClassResolver
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.util.Locale
import org.slf4j.LoggerFactory

/**
 * Freemarker 工具。
 * 统一收口模板初始化与渲染逻辑，避免业务层直接散落 Freemarker 配置细节。
 */
object FtlTool {
    private val logger = LoggerFactory.getLogger(FtlTool::class.java)

    /**
     * 配置允许延迟注入；未初始化时直接失败，避免静默使用默认配置。
     */
    private var freemarkerConfig: Configuration? = null

    fun init(templatePath: String) {
        try {
            freemarkerConfig = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply {
                setDirectoryForTemplateLoading(File(templatePath))
                defaultEncoding = "UTF-8"
                numberFormat = "0.##########"
                newBuiltinClassResolver = TemplateClassResolver.SAFER_RESOLVER
                isClassicCompatible = true
                locale = Locale.CHINA
                templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
            }
        } catch (e: IOException) {
            logger.error("初始化 Freemarker 配置时发生异常。", e)
        }
    }
    fun init(freemarkerConfig: Configuration) {
        this.freemarkerConfig = freemarkerConfig
    }
    @Throws(IOException::class, TemplateException::class)
    fun processTemplateIntoString(template: Template, model: Any?): String {
        val result = StringWriter()
        template.process(model, result)
        return result.toString()
    }
    @Throws(IOException::class, TemplateException::class)
    fun processString(templateName: String, params: Map<String, Any>?): String {
        val template = requireConfig().getTemplate(templateName)
        return processTemplateIntoString(template, params)
    }
    @Throws(IOException::class, TemplateException::class)
    fun processString(
        freemarkerConfig: Configuration,
        templateName: String,
        params: Map<String, Any>?,
    ): String {
        val template = freemarkerConfig.getTemplate(templateName)
        return processTemplateIntoString(template, params)
    }

    private fun requireConfig(): Configuration {
        return freemarkerConfig ?: throw IllegalStateException("Freemarker 未初始化，请先调用 FtlTool.init(...)")
    }
}
