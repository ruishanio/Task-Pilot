package com.ruishanio.taskpilot.admin.auth.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.admin.auth.converter.TaskPilotJwtAuthenticationConverter
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.exception.TaskPilotAuthException
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.http.enums.Header
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import java.nio.charset.StandardCharsets
import java.time.Duration
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * 管理端本地认证配置。
 *
 * 统一在这里完成官方 JwtEncoder / JwtDecoder 初始化、Spring Security 无状态过滤链和 401/403 业务响应格式的收口。
 */
@Configuration
@EnableMethodSecurity
class TaskPilotAuthConfig {
    @Value("\${task-pilot.auth.token.timeout}")
    private var tokenTimeout: Long = 0

    @Value("\${task-pilot.auth.jwt.secret}")
    private var jwtSecret: String = ""

    /**
     * HMAC 密钥统一从配置装配成 `SecretKey`，并在启动期校验最小长度，避免运行期才暴露签名问题。
     */
    @Bean
    fun taskPilotJwtSecretKey(): SecretKey {
        if (StringTool.isBlank(jwtSecret)) {
            throw TaskPilotAuthException("task-pilot jwt secret is blank.")
        }
        val normalizedSecret = jwtSecret.trim()
        val secretBytes = normalizedSecret.toByteArray(StandardCharsets.UTF_8)
        if (secretBytes.size < 32) {
            throw TaskPilotAuthException("task-pilot jwt secret is too short for HS256.")
        }
        return SecretKeySpec(secretBytes, "HmacSHA256")
    }

    /**
     * 登录接口统一使用官方 `JwtEncoder` 签发 Bearer JWT，避免继续依赖项目内自定义工具类。
     */
    @Bean
    fun taskPilotJwtEncoder(secretKey: SecretKey): JwtEncoder {
        val jwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))
        // 编码器创建成功后立即初始化业务 Helper，避免配置类反向注入同源 Bean 触发循环依赖。
        TaskPilotAuthHelper.init(jwtEncoder, tokenTimeout)
        return jwtEncoder
    }

    /**
     * Bearer 验签交给官方 `JwtDecoder` 处理，自动覆盖签名与过期时间校验。
     */
    @Bean
    fun taskPilotJwtDecoder(secretKey: SecretKey): JwtDecoder {
        val jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
        // 管理端沿用原先“过期即失效”的语义，不给默认 60 秒时钟偏移留宽限窗口。
        jwtDecoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefault(),
                JwtTimestampValidator(Duration.ZERO)
            )
        )
        return jwtDecoder
    }

    /**
     * 匿名入口单独走一条不解析 Bearer 的过滤链，避免陈旧或损坏的 Authorization 头抢先把登录/OpenAPI 请求拦掉。
     */
    @Bean
    @Order(1)
    fun publicSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        applyStatelessDefaults(http)
        http.securityMatcher(publicRequestMatcher())
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }
        return http.build()
    }

    /**
     * 管理端受保护接口单独使用官方资源服务器链，确保 JWT 验签只覆盖真正需要登录态的管理 API 前缀。
     */
    @Bean
    @Order(2)
    fun manageApiSecurityFilterChain(
        http: HttpSecurity,
        taskPilotJwtAuthenticationConverter: TaskPilotJwtAuthenticationConverter
    ): SecurityFilterChain {
        val authenticationEntryPoint = { request: jakarta.servlet.http.HttpServletRequest, response: HttpServletResponse, ex: org.springframework.security.core.AuthenticationException ->
            writeJsonError(response, AuthConst.CODE_LOGIN_FAIL, resolveAuthenticationErrorMsg(request, ex))
        }
        val accessDeniedHandler = { request: jakarta.servlet.http.HttpServletRequest, response: HttpServletResponse, ex: org.springframework.security.access.AccessDeniedException ->
            val errorMsg = ex.message ?: "permission limit for path:${request.servletPath}"
            writeJsonError(response, AuthConst.CODE_PERMISSION_FAIL, errorMsg)
        }
        applyStatelessDefaults(http)
        http.securityMatcher(PathPatternRequestMatcher.pathPattern("${ManageRoute.API_MANAGE_PREFIX}/**"))
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .oauth2ResourceServer {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
                it.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(taskPilotJwtAuthenticationConverter)
                }
            }
        return http.build()
    }

    /**
     * 其余非管理端路径不参与 JWT 认证，保留当前应用对静态资源、Actuator 等非核心接口的开放行为。
     */
    @Bean
    @Order(3)
    fun fallbackSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        applyStatelessDefaults(http)
        http.authorizeHttpRequests {
            it.anyRequest().permitAll()
        }
        return http.build()
    }

    /**
     * 继续沿用前端现有“HTTP 200 + 业务码”协议，避免登录失效时额外引入一层 HTTP 状态处理分叉。
     */
    private fun writeJsonError(response: HttpServletResponse, code: Int, msg: String) {
        response.status = HttpServletResponse.SC_OK
        response.contentType = "application/json;charset=UTF-8"
        response.writer.println(GsonTool.toJson(Response.of<Any>(code, msg)))
    }

    /**
     * 缺少 Bearer 头时维持原有未登录提示，Bearer 存在但验签失败时优先透出官方异常信息，便于排查 JWT 问题。
     */
    private fun resolveAuthenticationErrorMsg(
        request: jakarta.servlet.http.HttpServletRequest,
        ex: org.springframework.security.core.AuthenticationException
    ): String {
        if (StringTool.isBlank(request.getHeader(Header.AUTHORIZATION.value))) {
            return "not login for path:${request.servletPath}"
        }
        return ex.message ?: "token is invalid"
    }

    /**
     * 三条过滤链统一关闭会话型认证入口，避免公共链和受保护链出现配置漂移。
     */
    private fun applyStatelessDefaults(http: HttpSecurity): HttpSecurity =
        http.csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

    /**
     * 这些入口允许匿名访问，且必须跳过资源服务器的 Bearer 解析，否则坏掉的 Authorization 头会提前触发 401。
     */
    private fun publicRequestMatcher(): RequestMatcher =
        OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(ManageRoute.ROOT),
            PathPatternRequestMatcher.pathPattern(ManageRoute.WEB_PREFIX),
            PathPatternRequestMatcher.pathPattern(ManageRoute.WEB_ROOT),
            PathPatternRequestMatcher.pathPattern("${ManageRoute.WEB_PREFIX}/**"),
            PathPatternRequestMatcher.pathPattern("${ManageRoute.API_MANAGE_AUTH}/login"),
            PathPatternRequestMatcher.pathPattern(ManageRoute.API_MANAGE_ERROR_PAGE),
            PathPatternRequestMatcher.pathPattern("${Const.ADMIN_OPEN_API_PREFIX}/**"),
            PathPatternRequestMatcher.pathPattern("/actuator/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.OPTIONS, "/**")
        )
}
