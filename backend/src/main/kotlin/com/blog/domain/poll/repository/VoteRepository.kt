package com.blog.domain.poll.repository

import com.blog.domain.poll.entity.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface VoteRepository : JpaRepository<Vote, Long> {

    fun countByPollIdAndUserId(pollId: Long, userId: Long): Long
    fun countByPollIdAndAnonymousKey(pollId: Long, anonymousKey: String): Long

    @Modifying
    fun deleteAllByPollIdAndUserId(pollId: Long, userId: Long): Long
}