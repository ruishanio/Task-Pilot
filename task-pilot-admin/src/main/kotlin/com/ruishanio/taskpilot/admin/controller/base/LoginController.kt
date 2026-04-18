package com.ruishanio.taskpilot.admin.controller.base

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.mapper.TaskPilotUserMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotUser
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.crypto.Sha256Tool
import com.ruishanio.taskpilot.tool.id.UUIDTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView

/**
 * 登录与密码维护控制器。
 *
 * 继续复用本地认证 Helper 的登录/登出逻辑，只保留管理端自身的账号密码校验。
 */
@Controller
@RequestMapping("/auth")
class LoginController {
    @Resource
    private lateinit var taskPilotUserMapper: TaskPilotUserMapper

    @RequestMapping("/login")
    @TaskPilotAuth(login = false)
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
        modelAndView: ModelAndView
    ): ModelAndView {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithCookie(request, response)
        if (loginInfoResponse.isSuccess) {
            modelAndView.view = RedirectView("/", true, false)
            return modelAndView
        }
        return ModelAndView("base/login")
    }

    /**
     * 管理端仍然走本地用户表校验，校验通过后再创建本地登录态。
     */
    @RequestMapping(value = ["/doLogin"], method = [RequestMethod.POST])
    @ResponseBody
    @TaskPilotAuth(login = false)
    fun doLogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
        userName: String?,
        password: String?,
        ifRemember: String?
    ): Response<String> {
        val ifRem = StringTool.isNotBlank(ifRemember) && "on" == ifRemember
        if (StringTool.isBlank(userName) || StringTool.isBlank(password)) {
            return Response.ofFail(I18nUtil.getString("login_param_empty"))
        }
        val normalizedPassword = password!!.trim()

        val taskPilotUser = taskPilotUserMapper.loadByUserName(userName)
            ?: return Response.ofFail(I18nUtil.getString("login_param_unvalid"))
        val passwordHash = Sha256Tool.sha256(normalizedPassword)
        if (passwordHash != taskPilotUser.password) {
            return Response.ofFail(I18nUtil.getString("login_param_unvalid"))
        }

        val loginInfo = LoginInfo(
            userId = taskPilotUser.id.toString(),
            userName = taskPilotUser.username,
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
    @RequestMapping("/updatePwd")
    @ResponseBody
    @TaskPilotAuth
    fun updatePwd(
        request: HttpServletRequest,
        oldPassword: String?,
        password: String?
    ): Response<String> {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"))
        }
        if (password == null || password.trim().isEmpty()) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"))
        }
        val normalizedPassword = password.trim()
        if (normalizedPassword.length !in 4..20) {
            return Response.ofFail(I18nUtil.getString("system_lengh_limit") + "[4-20]")
        }

        val oldPasswordHash = Sha256Tool.sha256(oldPassword)
        val passwordHash = Sha256Tool.sha256(normalizedPassword)

        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        val existUser = taskPilotUserMapper.loadByUserName(loginInfo.userName) as TaskPilotUser
        if (oldPasswordHash != existUser.password) {
            return Response.ofFail(I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_unvalid"))
        }

        existUser.password = passwordHash
        taskPilotUserMapper.update(existUser)
        return Response.ofSuccess()
    }
}
