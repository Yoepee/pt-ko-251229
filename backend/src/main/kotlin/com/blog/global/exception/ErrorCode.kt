package com.blog.global.exception

enum class ErrorCode(
    val status: Int,
    val message: String,
) {
    INVALID_REQUEST(400, "잘못된 요청입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "권한이 없습니다."),
    NOT_FOUND(404, "대상을 찾을 수 없습니다."),
    CONFLICT(409, "이미 존재합니다."),
    INTERNAL_ERROR(500, "서버 오류가 발생했습니다."),

    USERNAME_DUPLICATED(409, "이미 존재하는 username 입니다."),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
}