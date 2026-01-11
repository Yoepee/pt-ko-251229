package com.blog.domain.poll.repository

import com.blog.domain.poll.dto.request.RankingRange
import com.blog.domain.poll.dto.request.RankingTrack
import com.blog.domain.poll.dto.response.PollRankingResponse
import com.blog.domain.poll.entity.PollType
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class PollRankingJooqRepository(
    private val dsl: DSLContext,
) {
    private val zone = ZoneId.of("Asia/Seoul")

    fun rank(
        range: RankingRange,
        track: RankingTrack,
        pollType: PollType?,
        categoryId: Long?,
        offset: Int,
        limit: Int
    ): List<PollRankingResponse> {
        val (from, to) = resolveRange(range)

        val polls = table(name("polls"))
        val votes = table(name("votes"))

        val pId = field(name("polls", "id"), Long::class.java)!!
        val pTitle = field(name("polls", "title"), String::class.java)!!
        val pVisibility = field(name("polls", "visibility"), String::class.java)!!
        val pPollType = field(name("polls", "poll_type"), String::class.java)!!
        val pCategoryId = field(name("polls", "category_id"), Long::class.java) // nullable 가능
        val pEndsAt = field(name("polls", "ends_at"), LocalDateTime::class.java) // nullable 가능

        val vId = field(name("votes", "id"), Long::class.java)!!
        val vPollId = field(name("votes", "poll_id"), Long::class.java)!!
        val vCreatedAt = field(name("votes", "created_at"), LocalDateTime::class.java)!!
        val vUserId = field(name("votes", "user_id"), Long::class.java) // nullable 가능

        var condition = pVisibility.eq("PUBLIC")
            .and(vCreatedAt.ge(from))
            .and(vCreatedAt.lt(to))

        if (track == RankingTrack.TRUSTED) {
            condition = condition.and(vUserId.isNotNull)
        }

        if (pollType != null) {
            condition = condition.and(pPollType.eq(pollType.name))
        }

        if (categoryId != null) {
            condition = condition.and(pCategoryId.eq(categoryId))
        }

        return dsl
            .select(
                pId.`as`("pollId"),
                pTitle.`as`("title"),
                pPollType.`as`("pollType"),
                pCategoryId.`as`("categoryId"),
                pEndsAt.`as`("endsAt"),
                count(vId).`as`("voteCount"),
            )
            .from(polls)
            .join(votes).on(vPollId.eq(pId))
            .where(condition)
            .groupBy(pId, pTitle, pPollType, pCategoryId, pEndsAt)
            .orderBy(count(vId).desc(), pId.desc())
            .offset(offset)
            .limit(limit)
            .fetch { r ->
                PollRankingResponse(
                    pollId = r.get("pollId", Long::class.java)!!,
                    title = r.get("title", String::class.java)!!,
                    pollType = PollType.valueOf(r.get("pollType", String::class.java)!!),
                    categoryId = r.get("categoryId", Long::class.java),
                    endsAt = r.get("endsAt", LocalDateTime::class.java),
                    voteCount = r.get("voteCount", Long::class.java)!!,
                )
            }
    }

    fun countDistinctPolls(
        range: RankingRange,
        track: RankingTrack,
        pollType: PollType?,
        categoryId: Long?,
    ): Long {
        val (from, to) = resolveRange(range)

        val polls = table(name("polls"))
        val votes = table(name("votes"))

        val pId = field(name("polls", "id"), Long::class.java)!!
        val pVisibility = field(name("polls", "visibility"), String::class.java)!!
        val pPollType = field(name("polls", "poll_type"), String::class.java)!!
        val pCategoryId = field(name("polls", "category_id"), Long::class.java)

        val vPollId = field(name("votes", "poll_id"), Long::class.java)!!
        val vCreatedAt = field(name("votes", "created_at"), LocalDateTime::class.java)!!
        val vUserId = field(name("votes", "user_id"), Long::class.java)

        var condition = pVisibility.eq("PUBLIC")
            .and(vCreatedAt.ge(from))
            .and(vCreatedAt.lt(to))

        if (track == RankingTrack.TRUSTED) {
            condition = condition.and(vUserId.isNotNull)
        }

        if (pollType != null) {
            condition = condition.and(pPollType.eq(pollType.name))
        }

        if (categoryId != null) {
            condition = condition.and(pCategoryId.eq(categoryId))
        }

        // ✅ DISTINCT poll_id 개수
        return dsl
            .select(countDistinct(vPollId))
            .from(polls)
            .join(votes).on(vPollId.eq(pId))
            .where(condition)
            .fetchOne(0, Long::class.java) ?: 0L
    }

    private fun resolveRange(range: RankingRange): Pair<LocalDateTime, LocalDateTime> {
        val today = LocalDate.now(zone)
        val endExclusive = today.plusDays(1).atStartOfDay() // 오늘+1일 00:00 (exclusive)

        return when (range) {
            RankingRange.TODAY ->
                today.atStartOfDay() to endExclusive

            RankingRange.LAST_7_DAYS ->
                endExclusive.minusDays(7) to endExclusive

            RankingRange.LAST_30_DAYS ->
                endExclusive.minusDays(30) to endExclusive
        }
    }
}
