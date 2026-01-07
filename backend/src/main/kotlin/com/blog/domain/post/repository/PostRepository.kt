package com.blog.domain.post.repository

interface PostRepository {
    fun existsByCategoryId(categoryId: Long): Boolean
}