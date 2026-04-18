package com.ruishanio.taskpilot.tool.auth

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.text.ParseException
import java.util.Date

/**
 * JWT 工具。
 * 保留实例级 signer/verifier 设计，避免上层每次创建和校验 token 时重复构造签名器。
 */
class JwtTool(
    /**
     * 签名器和验签器继续作为实例状态保存，兼容 Java 侧现有构造和复用方式。
     */
    private val signer: JWSSigner,
    private val verifier: JWSVerifier,
) {
    constructor(secret: String) : this(createSigner(secret), createVerifier(secret))

    /**
     * 创建 JWT token。
     */
    fun createToken(subject: String, claims: Map<String, Any>?, ttlMillis: Long): String {
        try {
            val builder =
                JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(Date())
                    .expirationTime(Date(System.currentTimeMillis() + ttlMillis))
            claims?.forEach { (key, value) -> builder.claim(key, value) }

            val signedJwt =
                SignedJWT(
                    JWSHeader.Builder(JWSAlgorithm.HS256).build(),
                    builder.build(),
                )
            signedJwt.sign(signer)
            return signedJwt.serialize()
        } catch (e: Exception) {
            throw RuntimeException("创建JWT token失败", e)
        }
    }

    /**
     * 验证 token 签名和过期时间。
     */
    fun validateToken(token: String): Boolean {
        return try {
            val signedJwt = SignedJWT.parse(token)
            if (!signedJwt.verify(verifier)) {
                return false
            }
            val expirationTime = signedJwt.jwtClaimsSet.expirationTime
            expirationTime != null && expirationTime.after(Date())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 对外继续显式暴露解析异常，保持 Java 原签名。
     */
    @Throws(ParseException::class)
    fun parseToken(token: String): JWTClaimsSet {
        val signedJwt = SignedJWT.parse(token)
        return signedJwt.jwtClaimsSet
    }

    fun getClaim(token: String, claimName: String): Any? {
        return try {
            val claims = parseToken(token)
            claims.getClaim(claimName)
        } catch (_: Exception) {
            null
        }
    }

    fun getExpirationTime(token: String): Date? {
        return try {
            val claims = parseToken(token)
            claims.expirationTime
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /**
         * 这里统一把 Nimbus 的底层异常收口成 RuntimeException，和旧实现保持一致。
         */
        private fun createSigner(secret: String): JWSSigner {
            return try {
                MACSigner(secret)
            } catch (e: JOSEException) {
                throw RuntimeException(e)
            }
        }

        private fun createVerifier(secret: String): JWSVerifier {
            return try {
                MACVerifier(secret)
            } catch (e: JOSEException) {
                throw RuntimeException(e)
            }
        }
    }
}
