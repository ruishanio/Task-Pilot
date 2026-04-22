package com.ruishanio.taskpilot.admin.auth.password

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 统一封装管理端密码哈希策略。
 *
 * 管理端密码统一使用 Argon2，未发布阶段不再保留旧 SHA-256 兼容分支。
 */
@Component
class TaskPilotPasswordService {

    private val argon2PasswordEncoder = Argon2PasswordEncoder(16, 32, 1, 1 shl 14, 2)

    fun encode(rawPassword: String): String = requireNotNull(argon2PasswordEncoder.encode(rawPassword))

    /**
     * 仅接受 Argon2 哈希，避免旧弱哈希继续在未发布版本中流转。
     */
    fun matches(rawPassword: String, storedPassword: String?): Boolean {
        val normalizedStoredPassword = storedPassword ?: return false
        return try {
            argon2PasswordEncoder.matches(rawPassword, normalizedStoredPassword)
        } catch (_: Exception) {
            false
        }
    }
}
