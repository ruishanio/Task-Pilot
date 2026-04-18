package com.ruishanio.taskpilot.tool.core

import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import org.slf4j.LoggerFactory

/**
 * Properties 工具。
 * 继续区分“类路径加载”和“文件路径加载”两条入口，保持原有配置读取习惯不变。
 */
object PropTool {
    private val logger = LoggerFactory.getLogger(PropTool::class.java)

    /**
     * 从类路径读取配置，读取失败时返回空 `Properties`，保持历史容错行为。
     */
    fun loadProp(resourcePath: String?): Properties {
        val prop = Properties()
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            return prop
        }

        try {
            Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath).use { input ->
                if (input != null) {
                    prop.load(InputStreamReader(input, StandardCharsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            logger.error("从资源路径加载配置文件时发生异常。resourcePath={}", resourcePath, e)
        }
        return prop
    }

    /**
     * 从文件系统路径读取配置，不存在时继续返回空 `Properties`。
     */
    fun loadFileProp(fileName: String?): Properties {
        val prop = Properties()
        if (fileName == null || fileName.trim().isEmpty()) {
            return prop
        }

        val path: Path = Paths.get(fileName)
        if (!Files.exists(path)) {
            return prop
        }

        try {
            Files.newInputStream(path).use { input ->
                prop.load(InputStreamReader(input, StandardCharsets.UTF_8))
            }
        } catch (e: IOException) {
            logger.error("从文件路径加载配置文件时发生异常。fileName={}", fileName, e)
        }
        return prop
    }
    fun getString(prop: Properties, key: String): String? = prop.getProperty(key)
    fun getString(prop: Properties, key: String, defaultValue: String): String {
        val value = getString(prop, key)
        return value ?: defaultValue
    }
    fun getInt(prop: Properties, key: String): Int = getString(prop, key)!!.toInt()
    fun getBoolean(prop: Properties, key: String): Boolean = getString(prop, key).toBoolean()
    fun getLong(prop: Properties, key: String): Long = getString(prop, key)!!.toLong()
    fun getDouble(prop: Properties, key: String): Double = getString(prop, key)!!.toDouble()
}
