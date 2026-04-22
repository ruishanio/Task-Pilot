package com.ruishanio.taskpilot.admin.auth.converter

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.tool.core.StringTool
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * 管理端 JWT 认证结果转换器。
 *
 * Spring Security 官方资源服务器在完成验签后会把 `Jwt` 交给转换器，这里把它还原成业务层持续使用的
 * `LoginInfo`，并同步映射出角色 / 权限 authority，避免 controller 再感知 JWT 原始结构。
 */
@Component
class TaskPilotJwtAuthenticationConverter : Converter<Jwt, UsernamePasswordAuthenticationToken> {
    override fun convert(jwt: Jwt): UsernamePasswordAuthenticationToken {
        val loginInfo = TaskPilotAuthHelper.fromJwt(jwt)
            ?: throw BadCredentialsException("token is invalid")
        return UsernamePasswordAuthenticationToken(loginInfo, jwt.tokenValue, buildAuthorities(loginInfo))
    }

    /**
     * 角色仍按 `ROLE_` 前缀映射给 `hasRole(...)`，权限值则原样映射给细粒度授权规则。
     */
    private fun buildAuthorities(loginInfo: LoginInfo): Collection<GrantedAuthority> {
        val authorities = LinkedHashSet<GrantedAuthority>()
        loginInfo.roleList.orEmpty()
            .map(String::trim)
            .filter(StringTool::isNotBlank)
            .forEach { role ->
                authorities.add(SimpleGrantedAuthority("${AuthConst.ROLE_AUTHORITY_PREFIX}$role"))
            }
        loginInfo.permissionList.orEmpty()
            .map(String::trim)
            .filter(StringTool::isNotBlank)
            .forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission))
            }
        return authorities
    }
}
