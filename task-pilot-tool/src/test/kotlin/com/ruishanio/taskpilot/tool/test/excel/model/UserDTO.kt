package com.ruishanio.taskpilot.tool.test.excel.model

/**
 * Excel 用户测试模型。
 */
class UserDTO() {
    var userId: Long = 0
    var userName: String? = null
    var sex: UserSexEnum? = null

    constructor(userId: Long, userName: String?) : this() {
        this.userId = userId
        this.userName = userName
        this.sex = UserSexEnum.MALE
    }

    override fun toString(): String = "UserDTO{userId=$userId, userName='$userName', sex=$sex}"
}
