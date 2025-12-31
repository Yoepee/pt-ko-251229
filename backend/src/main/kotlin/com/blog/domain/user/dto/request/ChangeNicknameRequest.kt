package com.blog.domain.user.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangeNicknameRequest(

    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val nickname: String
)
