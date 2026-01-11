package com.blog.domain.poll.dto.response

data class PollStatsPreview(
    val totalVotes: Long,
    val myVoteOptionIds: List<Long>? = null,
    val endsInSeconds: Long? = null,
) {
    val myVoted: Boolean? get() = myVoteOptionIds?.isNotEmpty()
}