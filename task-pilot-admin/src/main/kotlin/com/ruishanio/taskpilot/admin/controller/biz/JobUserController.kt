package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.TaskPilotUserMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotUser
import com.ruishanio.taskpilot.admin.util.FrontendEntry
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.crypto.Sha256Tool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

/**
 * 管理端用户控制器。
 */
@Controller
@RequestMapping("/user")
class JobUserController {
    @Resource
    private lateinit var taskPilotUserMapper: TaskPilotUserMapper

    @RequestMapping
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun index(): String = FrontendEntry.route("/user")

    @RequestMapping("/pageList")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun pageList(
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        @RequestParam username: String?,
        @RequestParam role: Int
    ): Response<PageModel<TaskPilotUser>> {
        val list = taskPilotUserMapper.pageList(offset, pagesize, username, role)
        val listCount = taskPilotUserMapper.pageListCount(offset, pagesize, username, role)
        if (list.isNotEmpty()) {
            for (item in list) {
                item.password = null
            }
        }

        val pageModel = PageModel<TaskPilotUser>()
        pageModel.data = list
        pageModel.total = listCount
        return Response.ofSuccess(pageModel)
    }

    @RequestMapping("/insert")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun insert(taskPilotUser: TaskPilotUser): Response<String> {
        if (StringTool.isBlank(taskPilotUser.username)) {
            return Response.ofFail("请输入账号")
        }
        taskPilotUser.username = taskPilotUser.username!!.trim()
        if (taskPilotUser.username!!.length !in 4..20) {
            return Response.ofFail("长度限制[4-20]")
        }
        if (StringTool.isBlank(taskPilotUser.password)) {
            return Response.ofFail("请输入密码")
        }
        taskPilotUser.password = taskPilotUser.password!!.trim()
        val normalizedPassword = taskPilotUser.password!!
        if (normalizedPassword.length !in 4..20) {
            return Response.ofFail("长度限制[4-20]")
        }
        taskPilotUser.password = Sha256Tool.sha256(normalizedPassword)

        val existUser = taskPilotUserMapper.loadByUserName(taskPilotUser.username)
        if (existUser != null) {
            return Response.ofFail("账号重复")
        }

        taskPilotUserMapper.save(taskPilotUser)
        return Response.ofSuccess()
    }

    /**
     * 管理员不能通过该接口直接修改当前登录账号，避免误操作把自己锁出系统。
     */
    @RequestMapping("/update")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun update(request: HttpServletRequest, taskPilotUser: TaskPilotUser): Response<String> {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        if (loginInfo.userName == taskPilotUser.username) {
            return Response.ofFail("禁止操作当前登录账号")
        }

        if (StringTool.isNotBlank(taskPilotUser.password)) {
            taskPilotUser.password = taskPilotUser.password!!.trim()
            val normalizedPassword = taskPilotUser.password!!
            if (normalizedPassword.length !in 4..20) {
                return Response.ofFail("长度限制[4-20]")
            }
            taskPilotUser.password = Sha256Tool.sha256(normalizedPassword)
        } else {
            taskPilotUser.password = null
        }

        taskPilotUserMapper.update(taskPilotUser)
        return Response.ofSuccess()
    }

    @RequestMapping("/delete")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun delete(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail("请选择一条数据")
        }

        val loginInfoResponse: Response<LoginInfo> = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        if (ids.contains(loginInfo.userId!!.toInt())) {
            return Response.ofFail("禁止操作当前登录账号")
        }

        taskPilotUserMapper.delete(ids[0])
        return Response.ofSuccess()
    }
}
