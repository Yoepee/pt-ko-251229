package com.blog.domain.poll.repository

import com.blog.domain.poll.entity.PollOption
import org.springframework.data.jpa.repository.JpaRepository

interface PollOptionRepository : JpaRepository<PollOption, Long> {
    fun findAllByPollIdOrderBySortOrderAscIdAsc(pollId: Long): List<PollOption>
    fun findAllByPollIdAndIdIn(pollId: Long, ids: Collection<Long>): List<PollOption>
}