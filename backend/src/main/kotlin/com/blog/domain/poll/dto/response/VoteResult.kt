package com.blog.domain.poll.dto.response

enum class VoteResult {
    CREATED,     // 첫 투표
    CHANGED,     // 다른 선택지로 변경됨
    UNCHANGED    // 이미 동일한 투표 상태(멱등)
}