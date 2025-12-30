package com.blog.global.common

data class ApiResponse<T>(
    val status: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> ok(data: T? = null, message: String = "OK") =
            ApiResponse(status = 200, message = message, data = data)

        fun <T> created(data: T? = null, message: String = "CREATED") =
            ApiResponse(status = 201, message = message, data = data)

        fun fail(status: Int, message: String) =
            ApiResponse<Nothing>(status = status, message = message, data = null)
    }
}