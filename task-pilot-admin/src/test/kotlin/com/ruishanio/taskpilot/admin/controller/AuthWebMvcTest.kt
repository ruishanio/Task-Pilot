package com.ruishanio.taskpilot.admin.controller

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.tool.http.http.enums.Header
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

/**
 * 验证 Spring Security 对页面入口放行，以及对管理端 JSON 接口输出业务化 401/403。
 */
class AuthWebMvcTest : AbstractSpringMvcTest() {
    @Value("\${task-pilot.auth.jwt.secret}")
    private lateinit var jwtSecret: String

    @Test
    @Throws(Exception::class)
    fun pageRequestRedirectsToWebWhenAnonymous() {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/web"))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenAnonymous() {
        mockMvc.perform(get("/api/manage/system/bootstrap"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenTokenSignatureIsInvalid() {
        val invalidToken = createBearerToken(
            "another-task-pilot-jwt-secret-key-for-invalid-signature-check"
        )

        mockMvc.perform(get("/api/manage/system/bootstrap").header(Header.AUTHORIZATION.value, invalidToken))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenTokenExpired() {
        val expiredToken = createBearerToken(jwtSecret, -1000)

        mockMvc.perform(get("/api/manage/system/bootstrap").header(Header.AUTHORIZATION.value, expiredToken))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }

    @Test
    @Throws(Exception::class)
    fun adminApiReturns403WhenRoleMissing() {
        val nonAdminToken = createBearerToken(jwtSecret, 60_000, emptyList())

        mockMvc.perform(get("/api/manage/user/meta").header(Header.AUTHORIZATION.value, nonAdminToken))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":403")))
    }

    @Test
    @Throws(Exception::class)
    fun openApiStillWorksWhenInvalidBearerHeaderIsPresent() {
        mockMvc.perform(
            post("/api/executor/registry")
                .contentType(MediaType.APPLICATION_JSON)
                .header(Const.TASK_PILOT_ACCESS_TOKEN, "default_token")
                .header(Header.AUTHORIZATION.value, "Bearer invalid.token.here")
                .content("""{"registryGroup":"EXECUTOR","registryKey":"demo-app","registryValue":"http://127.0.0.1:9999"}""")
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":200")))
    }

    /**
     * 测试 token 只保留管理端鉴权所需最小 claim，避免把无关状态混进断言。
     */
    private fun createBearerToken(
        secret: String,
        ttlMillis: Long = 60_000,
        roleList: List<String> = listOf("ADMIN")
    ): String {
        val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val jwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))
        val expiresAt = Instant.now().plusMillis(ttlMillis)
        // 官方编码器要求 exp 必须晚于 iat，因此过期 token 要把签发时间一起回拨到更早时刻。
        val issuedAt = if (ttlMillis < 0) expiresAt.minusSeconds(60) else Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject("1")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("userId", "1")
            .claim("userName", "admin")
            .claim("roleList", roleList)
            .claim("extraInfo", mapOf("executorIds" to ""))
            .build()
        val token = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
            )
        ).tokenValue
        return "${AuthConst.BEARER_TOKEN_PREFIX}$token"
    }
}
