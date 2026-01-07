package com.blog.domain.post.repository

import org.springframework.stereotype.Repository

@Repository
class PostRepositoryStub : PostRepository {
    override fun existsByCategoryId(categoryId: Long): Boolean = false
}
