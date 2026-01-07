package com.blog.domain.poll.dto.request

import com.blog.domain.poll.entity.PollType
import com.blog.domain.poll.entity.PollVisibility
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class PollCreateRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    val categoryId: Long? = null,

    @field:NotNull val pollType: PollType,
    @field:NotNull val visibility: PollVisibility,

    val allowAnonymous: Boolean = true,
    val allowChange: Boolean = false,
    val maxSelections: Int = 1,
    val endsAt: LocalDateTime? = null,

    @field:NotEmpty val options: List<String>,
)