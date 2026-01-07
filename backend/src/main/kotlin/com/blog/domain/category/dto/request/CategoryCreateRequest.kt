package com.blog.domain.category.dto.request

data class CategoryCreateRequest(
    val name: String,
    val slug: String? = null,
    val sortOrder: Int? = null,
)