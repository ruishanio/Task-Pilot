package com.ruishanio.taskpilot.admin.web.error

import com.ruishanio.taskpilot.admin.auth.exception.TaskPilotAuthException
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import java.io.IOException

/**
 * 全局 Web 异常解析器。
 *
 * JSON 接口统一返回 200，错误码写在响应体中，避免前端在登录失效场景下同时处理 HTTP 错误与业务错误。
 */
@Component
class WebHandlerExceptionResolver : HandlerExceptionResolver {
    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception
    ): ModelAndView {
        if (ex !is TaskPilotException && ex !is TaskPilotAuthException) {
            logger.error("WebHandlerExceptionResolver 捕获到未处理异常。", ex)
        }

        val isJson = handler is HandlerMethod && isJsonHandler(handler)

        val mv = ModelAndView()
        if (isJson) {
            try {
                var errorCode = 500
                val errorMsgText = ex.message ?: ex.toString()
                if (ex is TaskPilotAuthException) {
                    errorCode = ex.errorCode
                }

                val errorMsg = GsonTool.toJson(Response.of<Any>(errorCode, errorMsgText))
                response.status = HttpServletResponse.SC_OK
                response.contentType = "application/json;charset=UTF-8"
                response.writer.println(errorMsg)
            } catch (ioe: IOException) {
                logger.error("输出异常响应时发生异常。", ioe)
            }
            return mv
        }

        mv.addObject("exceptionMsg", ex.toString())
        mv.viewName = "common/common.errorpage"
        return mv
    }

    /**
     * 同时兼容 `@ResponseBody` 方法和 `@RestController` 类，避免接口请求被误判成页面。
     */
    private fun isJsonHandler(handlerMethod: HandlerMethod): Boolean =
        AnnotatedElementUtils.hasAnnotation(handlerMethod.method, ResponseBody::class.java) ||
            AnnotatedElementUtils.hasAnnotation(handlerMethod.beanType, ResponseBody::class.java)

    companion object {
        private val logger = LoggerFactory.getLogger(WebHandlerExceptionResolver::class.java)
    }
}
