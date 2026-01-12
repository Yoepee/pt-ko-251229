package com.blog.domain.poll.repository

import com.blog.domain.poll.dto.request.PollListRequest
import com.blog.domain.poll.dto.response.OptionCountRow
import com.blog.domain.poll.dto.response.RankingCountItemRaw
import com.blog.domain.poll.dto.response.RankingPreviewRaw
import com.blog.domain.poll.dto.response.YesNoCountRaw
import com.blog.jooq.tables.PollOptions.POLL_OPTIONS
import com.blog.jooq.tables.Polls.POLLS
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

        val pid = field(name("pid"), Long::class.java)

        val idsTable = values(*pollIds.map { row(it) }.toTypedArray()).`as`("ids", "pid")
        val pidField = idsTable.field(pid)!!

        // YES/NO를 sort_order로 합산
        val yesAgg = sum(
            `when`(POLL_OPTIONS.SORT_ORDER.eq(0), inline(1)).otherwise(inline(0))
        )
        val noAgg = sum(
            `when`(POLL_OPTIONS.SORT_ORDER.eq(1), inline(1)).otherwise(inline(0))
        )

        val yesCnt = coalesce(yesAgg, inline(0)).`as`("yes_cnt")
        val noCnt  = coalesce(noAgg, inline(0)).`as`("no_cnt")

        val totalCnt = coalesce(count(VOTES.ID), inline(0)).`as`("total_cnt")

        return dsl
            .select(
                pidField,
                yesCnt,
                noCnt,
                totalCnt,
            )
            .from(idsTable)
            .leftJoin(VOTES).on(VOTES.POLL_ID.eq(pidField))
            .leftJoin(POLL_OPTIONS).on(POLL_OPTIONS.ID.eq(VOTES.OPTION_ID))
            .groupBy(pidField)
            .fetch()
            .associate { r ->
                val pollId = r.get(pidField)!!
                val yes = (r.get(yesCnt) as Number).toLong()
                val no  = (r.get(noCnt) as Number).toLong()
                val tot = (r.get(totalCnt) as Number).toLong()
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

        // 1) 옵션 기준 vote count 집계 (0 포함)
        val base = dsl
            .select(
                POLL_OPTIONS.POLL_ID.`as`("poll_id"),
                POLL_OPTIONS.ID.`as`("option_id"),
                POLL_OPTIONS.TEXT.`as`("label"),
                count(VOTES.ID).`as`("cnt"),
            )
            .from(POLL_OPTIONS)
            .leftJoin(VOTES).on(
                VOTES.POLL_ID.eq(POLL_OPTIONS.POLL_ID)
                    .and(VOTES.OPTION_ID.eq(POLL_OPTIONS.ID))
            )
            .where(POLL_OPTIONS.POLL_ID.`in`(pollIds))
            .groupBy(POLL_OPTIONS.POLL_ID, POLL_OPTIONS.ID, POLL_OPTIONS.TEXT)
            .asTable("base")

        val bPollId = base.field("poll_id", Long::class.java)!!
        val bOptionId = base.field("option_id", Long::class.java)!!
        val bLabel = base.field("label", String::class.java)!!
        val bCnt = base.field("cnt", Long::class.java)!!

        val total = sum(bCnt).over(partitionBy(bPollId)).`as`("total")
        val optionCount = count().over(partitionBy(bPollId)).`as`("option_count")
        val rn = rowNumber()
            .over(partitionBy(bPollId).orderBy(bCnt.desc(), bOptionId.asc()))
            .`as`("rn")

        // ✅ 2) rn/total 포함한 "ranked" 서브쿼리 생성
        val ranked = dsl
            .select(bPollId, bOptionId, bLabel, bCnt, total, optionCount, rn)
            .from(base)
            .asTable("ranked")

        val rPollId = ranked.field(bPollId)!!
        val rOptionId = ranked.field(bOptionId)!!
        val rLabel = ranked.field(bLabel)!!
        val rCnt = ranked.field(bCnt)!!
        val rTotal = ranked.field("total", Long::class.java)!!
        val rOptionCount = ranked.field("option_count", Int::class.java)!!
        val rRn = ranked.field("rn", Int::class.java)!!


        // ✅ 3) 바깥에서 rn 필터
        val rows = dsl
            .select(rPollId, rOptionId, rLabel, rCnt, rTotal, rOptionCount, rRn)
            .from(ranked)
            .where(rRn.le(topN))
            .orderBy(rPollId.asc(), rCnt.desc(), rOptionId.asc())
            .fetch()

        val grouped = rows.groupBy { it.get(rPollId)!! }

        return grouped.mapValues { (pid, rs) ->
            val tot = (rs.first().get(rTotal) as Number).toLong()
            val optCnt = rs.first().get(rOptionCount)!!
            val top = rs.map { rec ->
                RankingCountItemRaw(
                    optionId = rec.get(rOptionId)!!,
                    label = rec.get(rLabel)!!,
                    count = rec.get(rCnt)!!,
                )
            }
            RankingPreviewRaw(pollId = pid, total = tot, optionCount = optCnt, top = top)
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

    override fun countPublicPolls(req: PollListRequest): Long =
        dsl.selectCount()
            .from(POLLS)
            .where(publicListCondition(req))
            .fetchOne(0, Long::class.java) ?: 0L

    override fun fetchPublicPollIdsByPopular(
        req: PollListRequest,
        offset: Int,
        limit: Int,
        desc: Boolean,
    ): List<Long> {
        val voteCnt = count(VOTES.ID)

        return dsl.select(POLLS.ID)
            .from(POLLS)
            .leftJoin(VOTES).on(VOTES.POLL_ID.eq(POLLS.ID))
            .where(publicListCondition(req))
            .groupBy(POLLS.ID)
            .orderBy(
                if (desc) voteCnt.desc() else voteCnt.asc(),
                POLLS.ID.desc(), // tie-breaker(고정 추천)
            )
            .offset(offset)
            .limit(limit)
            .fetch(POLLS.ID)
    }

    private fun publicListCondition(req: PollListRequest): org.jooq.Condition {
        var c = POLLS.VISIBILITY.eq("PUBLIC")
            .and(POLLS.DELETED_AT.isNull)

        req.categoryId?.let { c = c.and(POLLS.CATEGORY_ID.eq(it)) }
        req.type?.let { c = c.and(POLLS.POLL_TYPE.eq(it.name)) } // 또는 enum 컬럼 타입에 맞게
        req.q?.takeIf { it.isNotBlank() }?.let { keyword ->
            c = c.and(POLLS.TITLE.likeIgnoreCase("%$keyword%")) // Postgres면 ILIKE로 나감
        }

        return c
    }
}