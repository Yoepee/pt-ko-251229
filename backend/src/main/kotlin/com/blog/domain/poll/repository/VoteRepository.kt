package com.blog.domain.poll.repository

import com.blog.domain.poll.entity.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface VoteRepository : JpaRepository<Vote, Long> {

    fun findAllByPollIdAndUserId(pollId: Long, userId: Long): List<Vote>
    fun findAllByPollIdAndAnonymousKey(pollId: Long, anonymousKey: String): List<Vote>

    @Modifying
    fun deleteAllByPollIdAndUserId(pollId: Long, userId: Long): Long
}