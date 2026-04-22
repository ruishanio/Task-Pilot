package com.ruishanio.taskpilot.admin.auth.password

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证管理端只接受 Argon2 密码哈希。
 */
class TaskPilotPasswordServiceTest {
    private val taskPilotPasswordService = TaskPilotPasswordService()

    @Test
    fun shouldEncodePasswordWithArgon2() {
        val encodedPassword = taskPilotPasswordService.encode("123456")

        assertTrue(encodedPassword.startsWith("\$argon2"))
        assertTrue(taskPilotPasswordService.matches("123456", encodedPassword))
    }

    @Test
    fun shouldRejectLegacyOrInvalidHash() {
        val legacyPassword = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"

        assertFalse(taskPilotPasswordService.matches("123456", legacyPassword))
        assertFalse(taskPilotPasswordService.matches("123456", "plain-text"))
        assertFalse(taskPilotPasswordService.matches("123456", null))
    }
}
