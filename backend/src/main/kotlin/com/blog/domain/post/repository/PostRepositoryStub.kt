package com.blog.domain.post.repository

import org.springframework.stereotype.Repository

@Repository
class PostRepositoryStub : PostRepository {
    override fun existsByUserIdAndCategoryId(userId: Long, categoryId: Long): Boolean = false
}
