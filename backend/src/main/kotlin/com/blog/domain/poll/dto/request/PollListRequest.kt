package com.blog.domain.poll.dto.request

import com.blog.domain.poll.entity.PollType

data class PollListRequest (
    val categoryId: Long? = null,
    val type: PollType? = null,
    val q: String? = null, // 제목 검색(contains, optional)
)