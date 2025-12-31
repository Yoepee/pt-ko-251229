package com.blog.domain.category.dto.response

data class CategoryNodeResponse(
    val id: Long,
    val name: String,
    val slug: String?,
    val depth: Int,
    val sortOrder: Int,
    val parentId: Long?,
    val children: List<CategoryNodeResponse> = emptyList()
)
