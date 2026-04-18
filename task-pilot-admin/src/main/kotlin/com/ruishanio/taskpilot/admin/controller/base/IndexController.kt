package com.ruishanio.taskpilot.admin.controller.base

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.model.dto.TaskPilotBootResourceDTO
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Comparator
import java.util.Date

/**
 * 管理端首页控制器。
 */
@Controller
class IndexController {
    @Resource
    private lateinit var taskPilotService: TaskPilotService

    @RequestMapping("/")
    @TaskPilotAuth
    fun index(request: HttpServletRequest, model: Model): String {
        model.addAttribute("resourceList", findResourceList(request))
        return "base/index"
    }

    /**
     * 菜单权限仍按角色裁剪，普通用户看不到管理员入口。
     */
    private fun findResourceList(request: HttpServletRequest): List<TaskPilotBootResourceDTO> {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data
        var resourceDTOList = listOf(
            TaskPilotBootResourceDTO(1, 0, I18nUtil.getString("job_dashboard_name"), 1, "", "/dashboard", "fa-home", 1, 0, null),
            TaskPilotBootResourceDTO(2, 0, I18nUtil.getString("jobinfo_name"), 1, "", "/jobinfo", " fa-clock-o", 2, 0, null),
            TaskPilotBootResourceDTO(3, 0, I18nUtil.getString("joblog_name"), 1, "", "/joblog", " fa-database", 3, 0, null),
            TaskPilotBootResourceDTO(4, 0, I18nUtil.getString("jobgroup_name"), 1, Consts.ADMIN_ROLE, "/jobgroup", " fa-cloud", 4, 0, null),
            TaskPilotBootResourceDTO(5, 0, I18nUtil.getString("user_manage"), 1, Consts.ADMIN_ROLE, "/user", "fa-users", 5, 0, null)
        )

        if (loginInfo == null || !TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess) {
            resourceDTOList = resourceDTOList.filter { StringTool.isBlank(it.permission) }
        }

        return resourceDTOList.sortedWith(Comparator.comparing(TaskPilotBootResourceDTO::order))
            .toCollection(ArrayList())
    }

    @RequestMapping("/dashboard")
    @TaskPilotAuth
    fun dashboard(request: HttpServletRequest, model: Model): String {
        model.addAllAttributes(taskPilotService.dashboardInfo())
        return "base/dashboard"
    }

    @RequestMapping("/chartInfo")
    @ResponseBody
    fun chartInfo(
        @RequestParam("startDate") startDate: Date,
        @RequestParam("endDate") endDate: Date
    ): Response<Map<String, Any>> = taskPilotService.chartInfo(startDate, endDate)

    @RequestMapping("/errorpage")
    @TaskPilotAuth(login = false)
    fun errorPage(
        request: HttpServletRequest,
        response: HttpServletResponse,
        mv: ModelAndView
    ): ModelAndView {
        mv.addObject("exceptionMsg", "HTTP Status Code: ${response.status}")
        mv.viewName = "common/common.errorpage"
        return mv
    }

    /**
     * 继续兼容历史页面表单传入的日期格式。
     */
    @InitBinder
    fun initBinder(binder: WebDataBinder) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.isLenient = false
        binder.registerCustomEditor(Date::class.java, CustomDateEditor(dateFormat, true))
    }
}
