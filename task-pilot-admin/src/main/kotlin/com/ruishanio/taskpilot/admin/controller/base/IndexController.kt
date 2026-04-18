package com.ruishanio.taskpilot.admin.controller.base

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 管理端基础 API 控制器。
 */
@Controller
class IndexController {
    @Resource
    private lateinit var taskPilotService: TaskPilotService

    @RequestMapping(ManageRoute.API_MANAGE_CHART_INFO)
    @ResponseBody
    fun chartInfo(
        @RequestParam("startDate") startDate: Date,
        @RequestParam("endDate") endDate: Date
    ): Response<Map<String, Any>> = taskPilotService.chartInfo(startDate, endDate)

    @RequestMapping(ManageRoute.API_MANAGE_ERROR_PAGE)
    @ResponseBody
    @TaskPilotAuth(login = false)
    fun errorPage(response: HttpServletResponse): String = "HTTP Status Code: ${response.status}"

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
