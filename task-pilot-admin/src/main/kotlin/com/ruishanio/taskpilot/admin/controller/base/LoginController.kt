package com.ruishanio.taskpilot.admin.controller.base

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.auth.password.TaskPilotPasswordService
import com.ruishanio.taskpilot.admin.mapper.UserMapper
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.id.UUIDTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import jakarta.servlet.http.HttpServletResponse

/**
 * 登录与密码维护控制器。
 *
 * 继续复用本地认证 Helper 的登录/登出逻辑，只保留管理端自身的账号密码校验。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_AUTH)
class LoginController {

    @Resource
    private lateinit var userMapper: UserMapper

    @Resource
    private lateinit var taskPilotPasswordService: TaskPilotPasswordService

    /**
     * 管理端仍然走本地用户表校验，校验通过后再创建本地登录态。
     */
    @RequestMapping(value = ["/login"], method = [RequestMethod.POST])
    @ResponseBody
    @TaskPilotAuth(login = false)
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
        userName: String?,
        password: String?,
        ifRemember: String?
    ): Response<String> {
        val ifRem = StringTool.isNotBlank(ifRemember) && "on" == ifRemember
        if (StringTool.isBlank(userName) || StringTool.isBlank(password)) {
            return Response.ofFail("账号或密码为空")
        }
        val normalizedPassword = password!!.trim()

        val user = userMapper.loadByUserName(userName)
            ?: return Response.ofFail("账号或密码错误")
        if (!taskPilotPasswordService.matches(normalizedPassword, user.password)) {
            return Response.ofFail("账号或密码错误")
        }

        val loginInfo = LoginInfo(
            userId = user.id.toString(),
            userName = user.username,
            signature = UUIDTool.getSimpleUUID()
        )
        val result = TaskPilotAuthHelper.loginWithCookie(loginInfo, response, ifRem)
        return Response.of(result.code, result.msg)
    }

    @RequestMapping(value = ["/logout"], method = [RequestMethod.POST])
    @ResponseBody
    @TaskPilotAuth(login = false)
    fun logout(request: HttpServletRequest, response: HttpServletResponse): Response<String> {
        val result = TaskPilotAuthHelper.logoutWithCookie(request, response)
        return Response.of(result.code, result.msg)
    }

    /**
     * 只允许当前登录用户修改自己的密码，并要求校验旧密码。
     */
    @RequestMapping("/update_password")
    @ResponseBody
    @TaskPilotAuth
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
}
