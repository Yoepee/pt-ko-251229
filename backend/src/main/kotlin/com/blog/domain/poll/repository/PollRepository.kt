package com.blog.domain.poll.repository

import com.blog.domain.poll.entity.Poll
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface PollRepository : JpaRepository<Poll, Long>, JpaSpecificationExecutor<Poll> {
    fun existsByCategoryId(categoryId: Long): Boolean
}