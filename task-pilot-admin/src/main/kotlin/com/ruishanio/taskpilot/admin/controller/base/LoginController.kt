package com.ruishanio.taskpilot.admin.controller.base

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.auth.model.LoginTokenPayload
import com.ruishanio.taskpilot.admin.auth.password.TaskPilotPasswordService
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.UserMapper
import com.ruishanio.taskpilot.admin.model.User
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

/**
 * 登录与密码维护控制器。
 *
 * 继续复用认证 Helper 基于 Spring Security 官方 `JwtEncoder` 的签发逻辑，只保留管理端自身的账号密码校验。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_AUTH)
class LoginController {

    @Resource
    private lateinit var userMapper: UserMapper

    @Resource
    private lateinit var taskPilotPasswordService: TaskPilotPasswordService

    /**
     * 管理端仍然走本地用户表校验，校验通过后直接返回 Bearer JWT。
     */
    @RequestMapping(value = ["/login"], method = [RequestMethod.POST])
    @ResponseBody
    fun login(
        userName: String?,
        password: String?,
        ifRemember: String?
    ): Response<LoginTokenPayload> {
        if (StringTool.isBlank(userName) || StringTool.isBlank(password)) {
            return Response.ofFail("账号或密码为空")
        }
        val normalizedPassword = password!!.trim()
        // 记住我开关改由前端决定 token 存储位置，后端只保留该参数以兼容现有表单提交。
        ifRemember?.trim()

        val user = userMapper.loadByUserName(userName)
            ?: return Response.ofFail("账号或密码错误")
        if (!taskPilotPasswordService.matches(normalizedPassword, user.password)) {
            return Response.ofFail("账号或密码错误")
        }

        return TaskPilotAuthHelper.login(buildLoginInfo(user))
    }
    /**
     * 只允许当前登录用户修改自己的密码，并要求校验旧密码。
     */
    @RequestMapping("/update_password")
    @ResponseBody
    fun updatePassword(
        request: HttpServletRequest,
        oldPassword: String?,
        password: String?
    ): Response<String> {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Response.ofFail("请输入旧密码")
        }
        if (password == null || password.trim().isEmpty()) {
            return Response.ofFail("请输入新密码")
        }
        val normalizedPassword = password.trim()
        if (normalizedPassword.length !in 4..20) {
            return Response.ofFail("长度限制[4-20]")
        }

        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        val existUser = userMapper.loadByUserName(loginInfo.userName) ?: return Response.ofFail("用户不存在")
        if (!taskPilotPasswordService.matches(oldPassword.trim(), existUser.password)) {
            return Response.ofFail("旧密码非法")
        }

        userMapper.updatePassword(existUser.id, taskPilotPasswordService.encode(normalizedPassword))
        return Response.ofSuccess()
    }

    /**
     * JWT 需要自包含当前用户的核心授权信息，这里一次性整理成登录态对象，避免控制器和解析器各自拼装。
     */
    private fun buildLoginInfo(user: User): LoginInfo {
        val roleList = if (user.role == 1) listOf(Consts.ADMIN_ROLE) else null
        val extraInfo = mapOf("executorIds" to (user.permission ?: ""))
        return LoginInfo(
            userId = user.id.toString(),
            userName = user.username,
            roleList = roleList,
            extraInfo = extraInfo
        )
    }
}
