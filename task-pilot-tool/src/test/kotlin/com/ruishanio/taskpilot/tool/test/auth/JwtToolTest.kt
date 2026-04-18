package com.ruishanio.taskpilot.tool.test.auth

import com.ruishanio.taskpilot.tool.auth.JwtTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * JwtTool 创建与解析验证。
 */
class JwtToolTest {
    @Test
    fun test01() {
        val secret = "your-256-bit-secret-key-should-be-at-least-32-bytes"
        val jwtTool = JwtTool(secret)

        val subject = "user_01"
        val claims = hashMapOf<String, Any>("userId" to "123", "role" to "admin")

        val token = jwtTool.createToken(subject, claims, 1000 * 60 * 60 * 24)
        logger.info("Generated Token: {}", token)

        val isValid = jwtTool.validateToken(token)
        logger.info("Token is valid: {}", isValid)

        val userId = jwtTool.getClaim(token, "userId")
        logger.info("UserId: {}", userId)

        val expirationTime: Date? = jwtTool.getExpirationTime(token)
        logger.info("Expiration Time: {}", expirationTime)
    }

    @Test
    fun test02() {
        val secret = "your-256-bit-secret-key-should-be-at-least-32-bytes"
        val jwtTool = JwtTool(secret)

        val token =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyXzAxIiwicm9sZSI6ImFkbWluIiwiZXhwIjoxNzQ2MDYzOTMyLCJpYXQiOjE3NDU5Nzc1MzIsInVzZXJJZCI6IjEyMyJ9.UOscITPd5OThuhe1s61jXwDtvVUOfBWF1E1Ns2sNujs"
        logger.info("{}", jwtTool.validateToken(token))
        logger.info("{}", jwtTool.getClaim(token, "userId"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JwtToolTest::class.java)
    }
}
