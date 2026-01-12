package com.blog.domain.poll.repository

import com.blog.domain.poll.dto.response.OptionCountRow
import com.blog.domain.poll.dto.response.RankingCountItemRaw
import com.blog.domain.poll.dto.response.RankingPreviewRaw
import com.blog.domain.poll.dto.response.YesNoCountRaw
import com.blog.jooq.tables.PollOptions.POLL_OPTIONS
import com.blog.jooq.tables.Votes.VOTES
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository

@Repository
class PollJooqRepositoryImpl(
    private val dsl: DSLContext,
) : PollJooqRepository {

    override fun fetchTotalVotes(pollIds: List<Long>): Map<Long, Long> {
        if (pollIds.isEmpty()) return emptyMap()

        return dsl
            .select(VOTES.POLL_ID, count().`as`("cnt"))
            .from(VOTES)
            .where(VOTES.POLL_ID.`in`(pollIds))
            .groupBy(VOTES.POLL_ID)
            .fetch()
            .associate { r ->
                val pollId = r.get(VOTES.POLL_ID)!!
                val cnt = (r.get("cnt") as Number).toLong()
                pollId to cnt
            }
    }

    /**
     * 찬반 기준:
     * - poll_options.sort_order = 0 -> 찬성
     * - poll_options.sort_order = 1 -> 반대
     */
    override fun fetchYesNoCounts(pollIds: List<Long>): Map<Long, YesNoCountRaw> {
        if (pollIds.isEmpty()) return emptyMap()

        // YES/NO를 sort_order로 합산
        val yesCount = sum(
            `when`(POLL_OPTIONS.SORT_ORDER.eq(0), inline(1)).otherwise(inline(0))
        ).`as`("yes_cnt")

        val noCount = sum(
            `when`(POLL_OPTIONS.SORT_ORDER.eq(1), inline(1)).otherwise(inline(0))
        ).`as`("no_cnt")

        val total = count().`as`("total_cnt")

        return dsl
            .select(
                VOTES.POLL_ID,
                yesCount,
                noCount,
                total,
            )
            .from(VOTES)
            .join(POLL_OPTIONS).on(POLL_OPTIONS.ID.eq(VOTES.OPTION_ID))
            .where(VOTES.POLL_ID.`in`(pollIds))
            .groupBy(VOTES.POLL_ID)
            .fetch()
            .associate { r ->
                val pollId = r.get(VOTES.POLL_ID)!!
                val yes = (r.get("yes_cnt") as Number).toLong()
                val no = (r.get("no_cnt") as Number).toLong()
                val tot = (r.get("total_cnt") as Number).toLong()
                pollId to YesNoCountRaw(pollId, yes, no, tot)
            }
    }

    /**
     * poll별 topN 옵션을 뽑는 쿼리
     * - 옵션별 count 집계
     * - row_number() over(partition by poll_id order by cnt desc, option_id asc)
     * - rn <= topN 만 필터
     */
    override fun fetchRankingTop(pollIds: List<Long>, topN: Int): Map<Long, RankingPreviewRaw> {
        if (pollIds.isEmpty()) return emptyMap()

        val cntField = count().`as`("cnt")

        // 1) poll_id + option_id 단위 집계
        val base = dsl
            .select(
                VOTES.POLL_ID.`as`("poll_id"),
                VOTES.OPTION_ID.`as`("option_id"),
                POLL_OPTIONS.TEXT.`as`("label"),
                cntField,
            )
            .from(VOTES)
            .join(POLL_OPTIONS).on(POLL_OPTIONS.ID.eq(VOTES.OPTION_ID))
            .where(VOTES.POLL_ID.`in`(pollIds))
            .groupBy(VOTES.POLL_ID, VOTES.OPTION_ID, POLL_OPTIONS.TEXT)
            .asTable("base")

        val bPollId = base.field("poll_id", Long::class.java)!!
        val bOptionId = base.field("option_id", Long::class.java)!!
        val bLabel = base.field("label", String::class.java)!!
        val bCnt = base.field("cnt", Long::class.java)!!

        // 2) poll별 total
        val totals = dsl
            .select(bPollId.`as`("poll_id"), sum(bCnt).`as`("total"))
            .from(base)
            .groupBy(bPollId)
            .asTable("totals")

        val tPollId = totals.field("poll_id", Long::class.java)!!
        val tTotal = totals.field("total", Long::class.java)!!

        // 3) row_number over(partition by poll_id order by cnt desc, option_id asc)
        val rn = rowNumber()
            .over(partitionBy(bPollId).orderBy(bCnt.desc(), bOptionId.asc()))
            .`as`("rn")

        // 4) base join totals + rn 계산 후 topN 필터
        val ranked = dsl
            .select(
                bPollId,
                bOptionId,
                bLabel,
                bCnt,
                tTotal,
                rn,
            )
            .from(base)
            .join(totals).on(tPollId.eq(bPollId))
            .asTable("ranked")

        val rPollId = ranked.field(bPollId)!!
        val rOptionId = ranked.field(bOptionId)!!
        val rLabel = ranked.field(bLabel)!!
        val rCnt = ranked.field(bCnt)!!
        val rTotal = ranked.field(tTotal)!!
        val rRn = ranked.field("rn", Int::class.java)!!

        val rows = dsl
            .select(rPollId, rOptionId, rLabel, rCnt, rTotal)
            .from(ranked)
            .where(rRn.le(topN))
            .orderBy(rPollId.asc(), rCnt.desc(), rOptionId.asc())
            .fetch()

        val grouped = rows.groupBy { it.get(rPollId)!! }

        return grouped.mapValues { (pid, rs) ->
            val tot = rs.first().get(rTotal)!!
            val top = rs.map { rec ->
                RankingCountItemRaw(
                    optionId = rec.get(rOptionId)!!,
                    label = rec.get(rLabel)!!,
                    count = rec.get(rCnt)!!,
                )
            }
            RankingPreviewRaw(pollId = pid, total = tot, top = top)
        }
    }

    override fun fetchTotalVotes(pollId: Long): Long =
        dsl.selectCount()
            .from(VOTES)
            .where(VOTES.POLL_ID.eq(pollId))
            .fetchOne(0, Int::class.java)
            ?.toLong() ?: 0L

    override fun fetchPollOptionCounts(pollId: Long): List<OptionCountRow> {
        val cnt = count(VOTES.ID).`as`("cnt")

        return dsl
            .select(
                POLL_OPTIONS.ID,
                POLL_OPTIONS.TEXT,
                POLL_OPTIONS.SORT_ORDER,
                cnt
            )
            .from(POLL_OPTIONS)
            .leftJoin(VOTES).on(
                VOTES.POLL_ID.eq(pollId).and(VOTES.OPTION_ID.eq(POLL_OPTIONS.ID))
            )
            .where(POLL_OPTIONS.POLL_ID.eq(pollId))
            .groupBy(POLL_OPTIONS.ID, POLL_OPTIONS.TEXT, POLL_OPTIONS.SORT_ORDER)
            .orderBy(POLL_OPTIONS.SORT_ORDER.asc(), POLL_OPTIONS.ID.asc())
            .fetch { r ->
                OptionCountRow(
                    optionId = r.get(POLL_OPTIONS.ID)!!,
                    label = r.get(POLL_OPTIONS.TEXT)!!,
                    sortOrder = r.get(POLL_OPTIONS.SORT_ORDER)!!,
                    count = (r.get("cnt") as Number).toLong(),
                )
            }
    }

    override fun fetchMyVoteOptionIdsMap(pollIds: List<Long>, userId: Long): Map<Long, List<Long>> {
        if (pollIds.isEmpty()) return emptyMap()

        val rows = dsl
            .select(VOTES.POLL_ID, VOTES.OPTION_ID)
            .from(VOTES)
            .where(VOTES.POLL_ID.`in`(pollIds))
            .and(VOTES.USER_ID.eq(userId))
            .fetch()

        return rows
            .groupBy({ it.get(VOTES.POLL_ID)!! }, { it.get(VOTES.OPTION_ID)!! })
            .mapValues { (_, optionIds) -> optionIds.distinct().sorted() }
    }

    override fun fetchMyVoteOptionIds(pollId: Long, userId: Long): List<Long> {
        return dsl
            .select(VOTES.OPTION_ID)
            .from(VOTES)
            .where(VOTES.POLL_ID.eq(pollId))
            .and(VOTES.USER_ID.eq(userId))
            .orderBy(VOTES.OPTION_ID.asc())
            .fetch(VOTES.OPTION_ID)
    }
}