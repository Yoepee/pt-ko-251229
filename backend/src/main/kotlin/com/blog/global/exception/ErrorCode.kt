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

    // Socket
    WS_INVALID_PATH(400, "WebSocket 경로가 올바르지 않습니다."),
    WS_UNAUTHORIZED(401, "WebSocket 인증이 필요합니다."),
    WS_FORBIDDEN(403, "WebSocket 접근 권한이 없습니다."),

    // User
    USERNAME_DUPLICATED(409, "이미 존재하는 계정 입니다."),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),

    PASSWORD_MISMATCH(400, "현재 비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD_NOT_ALLOWED(400, "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),

    // Category
    CATEGORY_NOT_FOUND(404, "카테고리를 찾을 수 없습니다."),
    CATEGORY_DELETE_FORBIDDEN_HAS_POLL(409, "카테고리에 연결된 투표가 존재하여 삭제할 수 없습니다."),
    CATEGORY_NAME_DUPLICATED(409, "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_SLUG_DUPLICATED(409, "이미 존재하는 카테고리 슬러그입니다."),

    // Poll
    POLL_NOT_FOUND(404, "투표를 찾을 수 없습니다."),
    POLL_FORBIDDEN(403, "투표에 대한 권한이 없습니다."),
    POLL_CLOSED(409, "종료된 투표입니다."),
    POLL_ANONYMOUS_NOT_ALLOWED(403, "익명 투표가 허용되지 않습니다."),

    POLL_INVALID_OPTIONS(400, "선택지 구성이 올바르지 않습니다."),
    POLL_OPTION_NOT_FOUND(404, "선택지를 찾을 수 없습니다."),
    POLL_MAX_SELECTION_EXCEEDED(400, "선택 가능한 개수를 초과했습니다."),

    POLL_ALREADY_VOTED(409, "이미 투표했습니다."),
    POLL_CHANGE_NOT_ALLOWED(403, "투표 변경이 허용되지 않습니다."),
    POLL_ANONYMOUS_CHANGE_NOT_ALLOWED(403, "익명 투표는 변경할 수 없습니다."),

    // Rate limit
    RATE_LIMIT_EXCEEDED(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    RATE_LIMIT_VOTE_EXCEEDED(429, "투표 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    RATE_LIMIT_POLL_CREATE_EXCEEDED(429, "투표 생성 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // Battle
    BATTLE_MATCH_NOT_FOUND(404, "배틀 매치를 찾을 수 없습니다."),
    BATTLE_MATCH_NOT_LEAVABLE(400, "현재 상태에서는 방을 나갈 수 없습니다."),
    BATTLE_MATCH_NOT_JOINABLE(409, "현재 참가할 수 없는 매치입니다."),
    BATTLE_MATCH_FULL(409, "이미 매치 정원이 찼습니다."),
    BATTLE_NOT_PARTICIPANT(403, "매치 참가자가 아닙니다."),
    BATTLE_MATCH_NOT_RUNNING(409, "진행 중인 매치가 아닙니다."),
    BATTLE_FINISH_NOT_ALLOWED(403, "매치를 종료할 권한이 없습니다."),
    BATTLE_INVALID_INPUT(400, "입력 값이 올바르지 않습니다."),
    BATTLE_CHARACTER_CHANGE_NOT_ALLOWED(400, "대기중인 방에서만 캐릭터를 변경할 수 있습니다."),
    BATTLE_READY_NOT_ALLOWED(400, "대기중인 방에서만 준비 상태를 변경할 수 있습니다."),
    BATTLE_START_NOT_ALLOWED(403, "방장만 시작할 수 있습니다."),
    BATTLE_START_CONDITION_NOT_MET(400, "시작 조건이 충족되지 않았습니다."),
    BATTLE_RATE_LIMIT_EXCEEDED(429, "배틀 입력이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    BATTLE_KICK_NOT_ALLOWED(403, "강퇴할 권한이 없습니다."),
    BATTLE_TEAM_CHANGE_NOT_ALLOWED(400, "현재 상태에서는 팀을 변경할 수 없습니다."),
    BATTLE_KICK_TARGET_NOT_IN_ROOM(404, "강퇴 대상이 방에 존재하지 않습니다."),
    BATTLE_TEAM_CHANGE_CONFLICT(409, "팀 변경이 불가능합니다."),
    BATTLE_TEAM_CHANGE_REQUIRES_NOT_READY(400, "준비 해제 상태에서만 팀을 변경할 수 있습니다."),
    BATTLE_SEASON_NOT_FOUND(404, "활성 시즌을 찾을 수 없습니다."),
    BATTLE_RATING_NOT_FOUND(404, "사용자 레이팅 정보를 찾을 수 없습니다."),
    BATTLE_MATCH_ALREADY_FINISHED(409, "이미 종료된 매치입니다."),
    BATTLE_MATCH_CANCELED(409, "취소된 매치입니다."),
}
