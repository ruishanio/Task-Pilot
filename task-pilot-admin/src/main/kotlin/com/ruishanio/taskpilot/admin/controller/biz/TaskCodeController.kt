package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.mapper.GlueLogMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.model.GlueLog
import com.ruishanio.taskpilot.admin.util.ExecutorPermissionUtil
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
@RequestMapping(ManageRoute.API_MANAGE_TASK_CODE)
class TaskCodeController {
    @Resource
    private lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    private lateinit var glueLogMapper: GlueLogMapper

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

        val existsTaskInfo = taskInfoMapper.loadById(id)
            ?: return Response.ofFail("任务ID非法")
        val loginInfo: LoginInfo = ExecutorPermissionUtil.validExecutorPermission(request, existsTaskInfo.executorId)

        existsTaskInfo.glueSource = glueSource
        existsTaskInfo.glueRemark = glueRemark
        existsTaskInfo.glueUpdateTime = Date()
        existsTaskInfo.updateTime = Date()
        taskInfoMapper.update(existsTaskInfo)

        val glueLog = GlueLog().apply {
            taskId = existsTaskInfo.id
            glueType = existsTaskInfo.glueType
            this.glueSource = glueSource
            this.glueRemark = glueRemark
            addTime = Date()
            updateTime = Date()
        }
        glueLogMapper.save(glueLog)
        glueLogMapper.removeOld(existsTaskInfo.id, 30)

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "task-code-update",
            GsonTool.toJson(glueLog)
        )
        return Response.ofSuccess()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskCodeController::class.java)
    }
}
