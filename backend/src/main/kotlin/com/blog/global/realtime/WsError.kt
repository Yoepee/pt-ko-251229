package com.blog.global.realtime

data class WsError(
    val t: String = "ERROR",
    val code: String,
    val status: Int,
    val message: String,
)
