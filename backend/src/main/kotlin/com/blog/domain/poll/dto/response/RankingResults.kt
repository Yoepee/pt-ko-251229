package com.blog.domain.poll.dto.response

data class RankingResults(
    val totalVotes: Long,
    val items: List<RankingResultItem>,
) : PollResultsResponse
