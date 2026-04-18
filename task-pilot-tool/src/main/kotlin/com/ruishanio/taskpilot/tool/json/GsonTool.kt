package com.ruishanio.taskpilot.tool.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.HashMap

/**
 * Gson 工具。
 * 统一维护项目内的 Gson 配置，避免不同模块各自创建序列化实例。
 */
object GsonTool {
    private val gson: Gson =
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()
            .create()

    private val gsonPretty: Gson =
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()

    /** 对象转 JSON。 */
    fun toJson(src: Any?): String = gson.toJson(src)

    /** 对象转格式化 JSON。 */
    fun toJsonPretty(src: Any?): String = gsonPretty.toJson(src)

    /** JSON 转指定 class。 */
    fun <T> fromJson(json: String?, classOfT: Class<T>): T = gson.fromJson(json, classOfT)

    /** JSON 转指定 Type。 */
    fun <T> fromJson(json: String?, typeOfT: Type): T = gson.fromJson(json, typeOfT)

    /**
     * JSON 转带泛型参数的目标类型。
     * 仍基于 `TypeToken.getParameterized` 构建目标类型，保持历史泛型反序列化行为。
     */
    fun <T> fromJson(json: String?, rawType: Type, vararg typeArguments: Type): T {
        val type = TypeToken.getParameterized(rawType, *typeArguments).type
        return gson.fromJson(json, type)
    }

    /** JSON 转 ArrayList。 */
    fun <T> fromJsonList(json: String?, classOfT: Class<T>): ArrayList<T> {
        val type = TypeToken.getParameterized(ArrayList::class.java, classOfT).type
        return gson.fromJson(json, type)
    }

    /** JSON 转 HashMap。 */
    fun <K, V> fromJsonMap(json: String?, keyClass: Class<K>, valueClass: Class<V>): HashMap<K, V> {
        val type = TypeToken.getParameterized(HashMap::class.java, keyClass, valueClass).type
        return gson.fromJson(json, type)
    }

    /** 对象转 JsonElement。 */
    fun toJsonElement(src: Any?): JsonElement = gson.toJsonTree(src)

    /** JsonElement 转指定 class。 */
    fun <T> fromJsonElement(json: JsonElement?, classOfT: Class<T>): T = gson.fromJson(json, classOfT)

    /** JsonElement 转指定 Type。 */
    fun <T> fromJsonElement(json: JsonElement?, typeOfT: Type): T = gson.fromJson(json, typeOfT)

    /** JsonElement 转带泛型参数的目标类型。 */
    fun <T> fromJsonElement(json: JsonElement?, rawType: Type, vararg typeArguments: Type): T {
        val typeOfT = TypeToken.getParameterized(rawType, *typeArguments).type
        return gson.fromJson(json, typeOfT)
    }
}
