package com.blog.domain.poll.dto.request

import jakarta.validation.constraints.NotEmpty

data class VoteRequest(
    @field:NotEmpty val optionIds: List<Long>,
)
