package com.blog.domain.user.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(

    @field:NotBlank
    @field:Size(min = 6, max = 255)
    val currentPassword: String,

    @field:NotBlank
    @field:Size(min = 6, max = 255)
    val newPassword: String
)
