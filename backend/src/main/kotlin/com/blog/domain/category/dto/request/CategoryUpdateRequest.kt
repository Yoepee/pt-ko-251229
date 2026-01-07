package com.blog.domain.category.dto.request

data class CategoryUpdateRequest(
    val name: String? = null,
    val slug: String? = null,
    val sortOrder: Int? = null,
)
