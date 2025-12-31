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

    USERNAME_DUPLICATED(409, "이미 존재하는 계정 입니다."),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),

    PASSWORD_MISMATCH(400, "현재 비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD_NOT_ALLOWED(400, "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),

    // Category
    CATEGORY_NOT_FOUND(404, "카테고리를 찾을 수 없습니다."),
    CATEGORY_INVALID_PARENT(400, "잘못된 부모 카테고리입니다."),
    CATEGORY_DELETE_FORBIDDEN_HAS_CHILD(409, "하위 카테고리가 존재하여 삭제할 수 없습니다."),
    CATEGORY_DELETE_FORBIDDEN_HAS_POST(409, "카테고리에 연결된 게시글이 존재하여 삭제할 수 없습니다."),
    CATEGORY_NAME_DUPLICATED(409, "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_SLUG_DUPLICATED(409, "이미 존재하는 카테고리 슬러그입니다."),
}
