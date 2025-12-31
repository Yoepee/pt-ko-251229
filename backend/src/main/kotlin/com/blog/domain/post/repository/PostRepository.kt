package com.blog.domain.post.repository

interface PostRepository {
    fun existsByUserIdAndCategoryId(userId: Long, categoryId: Long): Boolean
}