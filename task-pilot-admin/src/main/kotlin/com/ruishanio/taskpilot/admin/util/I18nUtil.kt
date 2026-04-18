package com.ruishanio.taskpilot.admin.util

import com.ruishanio.taskpilot.core.constant.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.tool.core.PropTool
import com.ruishanio.taskpilot.tool.freemarker.FtlTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import freemarker.template.Configuration
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Properties

/**
 * i18n 工具。
 *
 * 作为 Spring 单例统一管理文案加载，并把自身暴露给 Freemarker 共享变量。
 */
@Component
class I18nUtil : InitializingBean {
    @Autowired
    private lateinit var configuration: Configuration

    /**
     * 启动时直接把当前单例暴露给 Freemarker，避免模板层重复包装访问入口。
     */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        configuration.setSharedVariable("I18nUtil", this)
        initI18nEnum()
    }

    /**
     * 枚举文案由资源文件驱动，启动时统一覆盖默认英文值。
     */
    private fun initI18nEnum() {
        for (item in ExecutorBlockStrategyEnum.values()) {
            item.title = getString("jobconf_block_${item.name}")
        }
    }

    companion object {
        private const val I18N_FILE = "i18n/message_zh_CN.properties"

        @Volatile
        private var prop: Properties? = null

        /**
         * 属性文件只做一次懒加载，避免高频文案读取时重复 IO。
         */
        fun loadI18nProp(): Properties {
            prop?.let { return it }
            return synchronized(this) {
                prop ?: PropTool.loadProp(I18N_FILE).also { prop = it }
            }
        }

        /**
         * 按 key 读取单个国际化文案。
         */
        fun getString(key: String): String = loadI18nProp().getProperty(key)

        /**
         * 批量读取文案并按 JSON 返回，兼容前端一次性拉取场景。
         */
        fun getMultString(vararg keys: String): String {
            val map = LinkedHashMap<String, String?>()
            val properties = loadI18nProp()
            if (keys.isNotEmpty()) {
                for (key in keys) {
                    map[key] = properties.getProperty(key)
                }
            } else {
                for (key in properties.stringPropertyNames()) {
                    map[key] = properties.getProperty(key)
                }
            }
            return GsonTool.toJson(map)
        }
    }
}
