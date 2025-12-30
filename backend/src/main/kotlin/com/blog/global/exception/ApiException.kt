package com.blog.global.exception

class ApiException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message) {
    val status: Int get() = errorCode.status
}