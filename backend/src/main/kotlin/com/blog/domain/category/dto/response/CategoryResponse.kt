package com.blog.domain.category.dto.response

data class CategoryResponse(
    val id: Long,
    val name: String,
    val slug: String?,
    val sortOrder: Int,
)
