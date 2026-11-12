package com.ruishanio.taskpilot.admin.controller

import com.ruishanio.taskpilot.admin.controller.biz.ExecutorController
import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.mapper.RegistryMapper
import com.ruishanio.taskpilot.admin.model.Executor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

/**
 * 验证新增和编辑入口在写库前校验名称长度，避免超长输入触发数据库错误。
 */
@ExtendWith(MockitoExtension::class)
class ExecutorControllerTest {
    @Mock
    private lateinit var executorMapper: ExecutorMapper

    @Mock
    private lateinit var registryMapper: RegistryMapper

    private lateinit var controller: ExecutorController

    @BeforeEach
    fun setUp() {
        controller = ExecutorController().apply { executorMapper = this@ExecutorControllerTest.executorMapper }
        ReflectionTestUtils.setField(controller, "registryMapper", registryMapper)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun shouldAccept64CharacterTitleOnInsertAndUpdate(isUpdate: Boolean) {
        val title = "名".repeat(64)
        val executor = Executor(id = 11, appname = "demo-app", title = " $title ")
        if (isUpdate) {
            `when`(registryMapper.findAll(anyInt(), anyObject())).thenReturn(emptyList())
            `when`(executorMapper.update(executor)).thenReturn(1)
        } else {
            `when`(executorMapper.save(executor)).thenReturn(1)
        }

        val response = if (isUpdate) controller.update(executor) else controller.insert(executor)

        assertTrue(response.isSuccess)
        assertEquals(title, executor.title)
        if (isUpdate) verify(executorMapper).update(executor) else verify(executorMapper).save(executor)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun shouldReject65CharacterTitleBeforeDatabaseAccess(isUpdate: Boolean) {
        val executor = Executor(id = 11, appname = "demo-app", title = "名".repeat(65))

        val response = if (isUpdate) controller.update(executor) else controller.insert(executor)

        assertFalse(response.isSuccess)
        assertEquals("名称长度不能超过64个字符", response.msg)
        verifyNoInteractions(executorMapper, registryMapper)
    }

    /**
     * 兼容 Kotlin 非空参数与 Mockito `any()` 的配合写法，避免 matcher 返回 null 直接触发 NPE。
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T {
        any<T>()
        return null as T
    }
}
