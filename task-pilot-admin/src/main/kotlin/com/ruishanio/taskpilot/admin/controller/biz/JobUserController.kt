package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.mapper.UserMapper
import com.ruishanio.taskpilot.admin.model.User
import com.ruishanio.taskpilot.admin.web.ManageRoute
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
import java.util.HashMap

/**
 * 管理端用户控制器。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_USER)
class UserController {
    @Resource
    private lateinit var executorMapper: ExecutorMapper

    @Resource
    private lateinit var userMapper: UserMapper

    /**
     * 用户管理页需要的角色与执行器权限选项直接跟随用户资源输出，便于前端按资源域加载。
     */
    @RequestMapping("/meta")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun meta(): Response<Map<String, Any>> {
        val data = HashMap<String, Any>()
        data["executors"] = executorMapper.findAll()
        data["roleOptions"] = listOf(
            option("1", "管理员"),
            option("0", "普通用户")
        )
        return Response.ofSuccess(data)
    }

    @RequestMapping("/page")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun page(
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        @RequestParam username: String?,
        @RequestParam role: Int
    ): Response<PageModel<User>> {
        val list = userMapper.pageList(offset, pagesize, username, role)
        val listCount = userMapper.pageListCount(offset, pagesize, username, role)
        if (list.isNotEmpty()) {
            for (item in list) {
                item.password = null
            }
        }

        val pageModel = PageModel<User>()
        pageModel.data = list
        pageModel.total = listCount
        return Response.ofSuccess(pageModel)
    }

    @RequestMapping("/insert")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun insert(user: User): Response<String> {
        if (StringTool.isBlank(user.username)) {
            return Response.ofFail("请输入账号")
        }
        user.username = user.username!!.trim()
        if (user.username!!.length !in 4..20) {
            return Response.ofFail("长度限制[4-20]")
        }
        if (StringTool.isBlank(user.password)) {
            return Response.ofFail("请输入密码")
        }
        user.password = user.password!!.trim()
        val normalizedPassword = user.password!!
        if (normalizedPassword.length !in 4..20) {
            return Response.ofFail("长度限制[4-20]")
        }
        user.password = Sha256Tool.sha256(normalizedPassword)

        val existUser = userMapper.loadByUserName(user.username)
        if (existUser != null) {
            return Response.ofFail("账号重复")
        }

        userMapper.save(user)
        return Response.ofSuccess()
    }

    /**
     * 管理员不能通过该接口直接修改当前登录账号，避免误操作把自己锁出系统。
     */
    @RequestMapping("/update")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun update(request: HttpServletRequest, user: User): Response<String> {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        if (loginInfo.userName == user.username) {
            return Response.ofFail("禁止操作当前登录账号")
        }

        if (StringTool.isNotBlank(user.password)) {
            user.password = user.password!!.trim()
            val normalizedPassword = user.password!!
            if (normalizedPassword.length !in 4..20) {
                return Response.ofFail("长度限制[4-20]")
            }
            user.password = Sha256Tool.sha256(normalizedPassword)
        } else {
            user.password = null
        }

        userMapper.update(user)
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

        userMapper.delete(ids[0])
        return Response.ofSuccess()
    }

    /**
     * 统一复用列表筛选用的轻量枚举结构，避免角色选项再单独定义 DTO。
     */
    private fun option(value: String, label: String): MutableMap<String, Any?> {
        val payload = HashMap<String, Any?>()
        payload["value"] = value
        payload["label"] = label
        return payload
    }
}
