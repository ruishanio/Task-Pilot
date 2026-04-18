package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotLogGlueMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotLogGlue
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.Date

/**
 * GLUE 代码编辑控制器。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_JOBCODE)
class JobCodeController {
    @Resource
    private lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Resource
    private lateinit var taskPilotLogGlueMapper: TaskPilotLogGlueMapper

    /**
     * 保存 GLUE 源码时同步记录历史快照，便于后续回溯。
     */
    @RequestMapping("/save")
    @ResponseBody
    fun save(
        request: HttpServletRequest,
        @RequestParam("id") id: Int,
        @RequestParam("glueSource") glueSource: String,
        @RequestParam("glueRemark") glueRemark: String?
    ): Response<String> {
        if (StringTool.isBlank(glueSource)) {
            return Response.ofFail("请输入GLUE源码")
        }
        if (glueRemark == null) {
            return Response.ofFail("请输入源码备注")
        }
        if (glueRemark.length !in 4..100) {
            return Response.ofFail("源码备注长度限制为4~100")
        }

        val existsJobInfo = taskPilotInfoMapper.loadById(id)
            ?: return Response.ofFail("任务ID非法")
        val loginInfo: LoginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, existsJobInfo.jobGroup)

        existsJobInfo.glueSource = glueSource
        existsJobInfo.glueRemark = glueRemark
        existsJobInfo.glueUpdatetime = Date()
        existsJobInfo.updateTime = Date()
        taskPilotInfoMapper.update(existsJobInfo)

        val taskPilotLogGlue = TaskPilotLogGlue().apply {
            jobId = existsJobInfo.id
            glueType = existsJobInfo.glueType
            this.glueSource = glueSource
            this.glueRemark = glueRemark
            addTime = Date()
            updateTime = Date()
        }
        taskPilotLogGlueMapper.save(taskPilotLogGlue)
        taskPilotLogGlueMapper.removeOld(existsJobInfo.id, 30)

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobcode-update",
            GsonTool.toJson(taskPilotLogGlue)
        )
        return Response.ofSuccess()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobCodeController::class.java)
    }
}
