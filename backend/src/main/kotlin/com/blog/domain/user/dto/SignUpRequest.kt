package com.blog.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank
    @field:Size(min = 4, max = 50)
    val username: String,

    @field:NotBlank
    @field:Size(min = 6, max = 255)
    val password: String,

    @field:NotBlank
    @field:Size(min = 4, max = 30)
    val nickname: String
)
