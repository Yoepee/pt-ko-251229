package com.blog.domain.poll.repository

import com.blog.domain.poll.dto.request.PollListRequest
import com.blog.domain.poll.dto.response.OptionCountRow
import com.blog.domain.poll.dto.response.RankingPreviewRaw
import com.blog.domain.poll.dto.response.YesNoCountRaw
import java.time.LocalDate

interface PollJooqRepository {
    fun fetchTotalVotes(pollIds: List<Long>): Map<Long, Long>

    // 찬반(옵션 sortOrder=0/1) 기준 카운트
    fun fetchYesNoCounts(pollIds: List<Long>): Map<Long, YesNoCountRaw>

    // poll별 Top N 옵션(카운트) + total
    fun fetchRankingTop(pollIds: List<Long>, topN: Int = 5): Map<Long, RankingPreviewRaw>

    fun fetchPollOptionCounts(pollId: Long): List<OptionCountRow>
    fun fetchTotalVotes(pollId: Long): Long
    fun fetchMyVoteOptionIdsMap(pollIds: List<Long>, userId: Long): Map<Long, List<Long>>
    fun fetchMyVoteOptionIds(pollId: Long, userId: Long): List<Long>
    fun countPublicPolls(req: PollListRequest): Long
    fun fetchPublicPollIdsByPopular(
        req: PollListRequest,
        offset: Int,
        limit: Int,
        desc: Boolean
    ): List<Long>
    fun insertDailyPoll(day: LocalDate, seq: Int, pollId: Long)
    fun existsDailyPoll(day: LocalDate, seq: Int): Boolean
}