package com.ruishanio.taskpilot.tool.http.http

import com.ruishanio.taskpilot.tool.core.AssertTool
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.http.http.enums.ContentType
import com.ruishanio.taskpilot.tool.http.http.enums.Header
import com.ruishanio.taskpilot.tool.http.http.enums.Method
import com.ruishanio.taskpilot.tool.http.http.iface.HttpInterceptor
import com.ruishanio.taskpilot.tool.json.GsonTool
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.ArrayList
import java.util.HashMap
import java.util.Objects
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.slf4j.LoggerFactory

/**
 * HTTP 请求对象。
 * 继续保留 builder 写法和显式执行入口，避免现有调用链在迁移后失去可读性。
 */
class HttpRequest {
    private var url: String? = null
    private var method: Method? = Method.POST
    private var contentType: ContentType? = ContentType.JSON
    private var headers: MutableMap<String, String>? = null
    private var cookies: MutableMap<String, String>? = null
    private var connectTimeout: Int = 3 * 1000
    private var readTimeout: Int = 3 * 1000
    private var useCaches: Boolean = false
    private var body: String? = null
    private var form: MutableMap<String, String>? = null
    private var auth: String? = null
    private var interceptors: MutableList<HttpInterceptor>? = null

    fun url(url: String?): HttpRequest {
        this.url = url
        return this
    }

    fun method(method: Method?): HttpRequest {
        this.method = method
        return this
    }

    fun contentType(contentType: ContentType?): HttpRequest {
        this.contentType = contentType
        return this
    }

    fun header(header: kotlin.collections.Map<String, String>?): HttpRequest {
        if (MapTool.isEmpty(header)) {
            return this
        }

        if (MapTool.isNotEmpty(headers)) {
            headers!!.clear()
        }
        for (key in header!!.keys) {
            header(key, header[key])
        }
        return this
    }

    fun header(key: String?, value: String?): HttpRequest {
        if (StringTool.isBlank(key) || Objects.isNull(value)) {
            return this
        }

        if (headers == null) {
            headers = HashMap()
        }
        headers!![key!!] = value!!
        return this
    }

    /** 默认请求头仍只补一个 User-Agent，保持旧工具链最小默认行为。 */
    fun headerDefault(): HttpRequest {
        header(Header.USER_AGENT.value, Header.DEFAULT_USER_AGENT_WIN)
        return this
    }

    fun cookie(cookie: kotlin.collections.Map<String, String>?): HttpRequest {
        if (MapTool.isEmpty(cookie)) {
            return this
        }

        if (MapTool.isNotEmpty(cookies)) {
            cookies!!.clear()
        }
        for (key in cookie!!.keys) {
            cookie(key, cookie[key])
        }
        return this
    }

    fun cookie(key: String?, value: String?): HttpRequest {
        if (StringTool.isBlank(key) || Objects.isNull(value)) {
            return this
        }

        if (cookies == null) {
            cookies = HashMap()
        }
        cookies!![key!!] = value!!
        return this
    }

    fun connectTimeout(connectTimeout: Int): HttpRequest {
        this.connectTimeout = connectTimeout
        return this
    }

    fun readTimeout(readTimeout: Int): HttpRequest {
        this.readTimeout = readTimeout
        return this
    }

    fun useCaches(useCaches: Boolean): HttpRequest {
        this.useCaches = useCaches
        return this
    }

    fun body(body: String?): HttpRequest {
        this.body = body
        return this
    }

    fun request(request: Any?): HttpRequest {
        if (request != null) {
            body = GsonTool.toJson(request)
        }
        return this
    }

    fun form(key: String?, value: String?): HttpRequest {
        if (StringTool.isBlank(key) || Objects.isNull(value)) {
            return this
        }

        if (form == null) {
            form = HashMap()
        }
        form!![key!!] = value!!
        return this
    }

    fun form(key: String?, value: kotlin.collections.List<String>?): HttpRequest {
        if (CollectionTool.isEmpty(value)) {
            return this
        }
        return form(key, StringTool.join(value, ","))
    }

    fun form(form: kotlin.collections.Map<String, String>?): HttpRequest {
        if (MapTool.isEmpty(form)) {
            return this
        }

        if (MapTool.isNotEmpty(this.form)) {
            this.form!!.clear()
        }
        for (key in form!!.keys) {
            form(key, form[key])
        }
        return this
    }

    fun auth(auth: String?): HttpRequest {
        this.auth = auth
        return this
    }

    fun interceptor(interceptors: kotlin.collections.List<HttpInterceptor>?): HttpRequest {
        if (CollectionTool.isEmpty(interceptors)) {
            return this
        }

        if (CollectionTool.isNotEmpty(this.interceptors)) {
            this.interceptors!!.clear()
        }
        for (interceptor in interceptors!!) {
            interceptor(interceptor)
        }
        return this
    }

    fun interceptor(interceptor: HttpInterceptor?): HttpRequest {
        if (Objects.isNull(interceptor)) {
            return this
        }

        if (interceptors == null) {
            interceptors = ArrayList()
        }
        interceptors!!.add(interceptor!!)
        return this
    }

    /**
     * Cookie 头继续按 `k1=v1; k2=v2` 拼接，兼容老式服务端对格式的宽松解析。
     */
    fun getCookieString(): String? {
        if (MapTool.isEmpty(cookies)) {
            return null
        }
        val cookieHeader = StringBuilder()
        for (key in cookies!!.keys) {
            if (cookieHeader.isNotEmpty() && !cookieHeader.toString().endsWith("; ")) {
                cookieHeader.append("; ")
            }
            cookieHeader.append(key).append("=").append(cookies!![key])
        }
        return cookieHeader.toString()
    }

    fun getUrl(): String? = url

    fun getMethod(): Method? = method

    fun getContentType(): ContentType? = contentType

    fun getHeader(): kotlin.collections.Map<String, String>? = headers

    fun getCookie(): kotlin.collections.Map<String, String>? = cookies

    fun getConnectTimeout(): Int = connectTimeout

    fun getReadTimeout(): Int = readTimeout

    fun isUseCaches(): Boolean = useCaches

    fun getBody(): String? = body

    fun getForm(): kotlin.collections.Map<String, String>? = form

    fun getAuth(): String? = auth

    /**
     * 发送请求时继续按照“拦截器前置 -> 建连 -> 写 body -> 读取响应 -> 拦截器后置”的顺序执行。
     * 这里不引入更高层的 HTTP 客户端库，避免行为细节发生偏移。
     */
    fun execute(): HttpResponse {
        AssertTool.isTrue(StringTool.isNotBlank(url), "http-request url is null")
        AssertTool.isTrue(Objects.nonNull(method), "http-request method is null")
        AssertTool.isTrue(Objects.nonNull(contentType), "http-request contentType is null")

        var connection: HttpURLConnection? = null
        var dataOutputStream: DataOutputStream? = null
        var bufferedReader: BufferedReader? = null
        try {
            if (CollectionTool.isNotEmpty(interceptors)) {
                for (interceptor in interceptors!!) {
                    interceptor.before(this)
                }
            }

            var finalUrl = url!!
            if (Method.GET == method) {
                val formParam = HttpTool.generateUrlParam(form)
                if (StringTool.isNotBlank(formParam)) {
                    finalUrl += if (finalUrl.contains("?")) "&$formParam" else "?$formParam"
                }
            }

            val finalUrlURL = createRequestUrl(finalUrl)
            connection = finalUrlURL.openConnection() as HttpURLConnection

            if (HttpTool.isHttps(finalUrl)) {
                val https = connection as HttpsURLConnection
                trustAllHosts(https)
            }

            connection.doOutput = true
            connection.doInput = true
            connection.setRequestProperty(Header.CONNECTION.value, "Keep-Alive")
            connection.setRequestProperty(Header.ACCEPT_CHARSET.value, StandardCharsets.UTF_8.toString())

            connection.requestMethod = method.toString()
            connection.setRequestProperty(Header.CONTENT_TYPE.value, contentType!!.getValue(StandardCharsets.UTF_8))
            connection.readTimeout = readTimeout
            connection.connectTimeout = connectTimeout
            connection.useCaches = useCaches

            if (MapTool.isNotEmpty(headers)) {
                for (key in headers!!.keys) {
                    connection.setRequestProperty(key, headers!![key])
                }
            }

            if (MapTool.isNotEmpty(cookies)) {
                val cookieString = getCookieString()
                connection.setRequestProperty(Header.COOKIE.value, cookieString)
            }

            if (StringTool.isNotBlank(auth)) {
                connection.setRequestProperty(Header.AUTHORIZATION.value, auth)
            }

            connection.connect()

            if (Method.GET != method) {
                val requestBody =
                    when {
                        StringTool.isNotBlank(body) -> body
                        MapTool.isNotEmpty(form) -> HttpTool.generateUrlParam(form)
                        else -> null
                    }

                if (StringTool.isNotBlank(requestBody)) {
                    dataOutputStream = DataOutputStream(connection.outputStream)
                    dataOutputStream.write(requestBody!!.toByteArray(StandardCharsets.UTF_8))
                    dataOutputStream.flush()
                    dataOutputStream.close()
                }
            }

            val httpResponse = HttpResponse()
            httpResponse.url = url
            httpResponse.statusCode = connection.responseCode

            val inputStream: InputStream? =
                if (httpResponse.isSuccess) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            if (inputStream != null) {
                bufferedReader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                val result = StringBuilder()
                var line = bufferedReader.readLine()
                while (line != null) {
                    result.append(line)
                    line = bufferedReader.readLine()
                }
                httpResponse.response = result.toString()
            }

            httpResponse.cookies = parseResponseCookieData(connection)

            if (CollectionTool.isNotEmpty(interceptors)) {
                for (interceptor in interceptors!!) {
                    interceptor.after(this, httpResponse)
                }
            }

            return httpResponse
        } catch (e: Exception) {
            throw RuntimeException("Http Request Error (${e.message}). for url : $url", e)
        } finally {
            try {
                dataOutputStream?.close()
            } catch (e2: Exception) {
                logger.error("关闭 HttpRequest 输出流时发生异常。", e2)
            }
            try {
                bufferedReader?.close()
            } catch (e2: Exception) {
                logger.error("关闭 HttpRequest 响应读取器时发生异常。", e2)
            }
            try {
                connection?.disconnect()
            } catch (e2: Exception) {
                logger.error("断开 HttpRequest 连接时发生异常。", e2)
            }
        }
    }

    /**
     * 仅提取 `Set-Cookie` 头里的键值对第一段，保持旧版对 path/domain/max-age 等附加信息的忽略策略。
     */
    private fun parseResponseCookieData(connection: HttpURLConnection): MutableMap<String, String>? {
        val headerFields = connection.headerFields
        val setCookieHeaders = headerFields[Header.SET_COOKIE.value]
        if (CollectionTool.isEmpty(setCookieHeaders)) {
            return null
        }

        val cookieMap = HashMap<String, String>()
        for (setCookieHeader in setCookieHeaders!!) {
            val parts = setCookieHeader.split(";")
            if (parts.isNotEmpty()) {
                val cookiePart = parts[0]
                val cookieKeyValue = cookiePart.split("=", limit = 2)
                if (cookieKeyValue.size == 2) {
                    cookieMap[cookieKeyValue[0]] = cookieKeyValue[1]
                }
            }
        }
        return cookieMap
    }

    /**
     * 这里继续沿用 `URL(String)` 的宽松解析行为。
     * 历史调用方允许直接传入带原始 query 文本的完整 URL，如果先转 `URI` 会提前失败。
     */
    @Suppress("DEPRECATION")
    private fun createRequestUrl(finalUrl: String): URL = URL(finalUrl)

    /**
     * 继续信任所有 HTTPS 证书，保持旧工具类的“方便调试优先”默认行为。
     * 这是历史语义，不在 Kotlin 迁移阶段顺手收紧。
     */
    private fun trustAllHosts(connection: HttpsURLConnection) {
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            val newFactory: SSLSocketFactory = sc.socketFactory
            connection.sslSocketFactory = newFactory
        } catch (e: Exception) {
            logger.error("初始化 HTTPS 信任上下文时发生异常。", e)
        }
        connection.hostnameVerifier =
            HostnameVerifier { _: String?, _: SSLSession? -> true }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpRequest::class.java)

        private val trustAllCerts: Array<TrustManager> =
            arrayOf(
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    }
                }
            )
    }
}
