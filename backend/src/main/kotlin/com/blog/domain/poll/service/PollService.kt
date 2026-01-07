package com.blog.domain.poll.service

import com.blog.domain.poll.dto.request.PollCreateRequest
import com.blog.domain.poll.dto.request.PollListRequest
import com.blog.domain.poll.dto.request.PollUpdateRequest
import com.blog.domain.poll.dto.request.VoteRequest
import com.blog.domain.poll.dto.response.PollCreateResponse
import com.blog.domain.poll.dto.response.PollDetailResponse
import com.blog.domain.poll.dto.response.PollOptionResponse
import com.blog.domain.poll.dto.response.PollPreview
import com.blog.domain.poll.dto.response.PollResultsResponse
import com.blog.domain.poll.dto.response.PollSummaryResponse
import com.blog.domain.poll.dto.response.PollStatsPreview
import com.blog.domain.poll.entity.Poll
import com.blog.domain.poll.entity.PollOption
import com.blog.domain.poll.entity.PollType
import com.blog.domain.poll.entity.Vote
import com.blog.domain.poll.repository.PollJooqRepository
import com.blog.domain.poll.repository.PollOptionRepository
import com.blog.domain.poll.repository.PollRepository
import com.blog.domain.poll.repository.PollSpecs
import com.blog.domain.poll.repository.VoteRepository
import com.blog.domain.poll.service.mapper.toRankingPreview
import com.blog.domain.poll.service.mapper.toYesNoPreview
import com.blog.domain.poll.service.mapper.buildYesNoResults
import com.blog.domain.poll.service.mapper.buildRankingResults
import com.blog.global.common.PageResponse
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import com.blog.global.security.JwtPrincipal
import com.blog.global.util.toPageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PollService(
    private val pollRepository: PollRepository,
    private val optionRepository: PollOptionRepository,
    private val voteRepository: VoteRepository,
    private val pollJooqRepository: PollJooqRepository,
) {

    @Transactional
    fun create(userId: Long, req: PollCreateRequest): PollCreateResponse {
        val title = req.title.trim()
        val options = req.options.map { it.trim() }.filter { it.isNotBlank() }

        if (options.size < 2) throw ApiException(ErrorCode.POLL_INVALID_OPTIONS)
        if (options.toSet().size != options.size) throw ApiException(ErrorCode.POLL_INVALID_OPTIONS)
        if (req.maxSelections < 1 || req.maxSelections > options.size) throw ApiException(ErrorCode.POLL_INVALID_OPTIONS)

        val poll = pollRepository.save(
            Poll(
                creatorUserId = userId,
                categoryId = req.categoryId,
                title = title,
                description = req.description?.trim()?.takeIf { it.isNotBlank() },
                pollType = req.pollType,
                visibility = req.visibility,
                allowAnonymous = req.allowAnonymous,
                allowChange = req.allowChange,
                maxSelections = req.maxSelections,
                endsAt = req.endsAt,
            )
        )

        optionRepository.saveAll(
            options.mapIndexed { idx, text ->
                PollOption(pollId = poll.id, text = text, sortOrder = idx)
            }
        )

        return PollCreateResponse(pollId = poll.id)
    }

    @Transactional(readOnly = true)
    fun listPublic(principal: JwtPrincipal?, req: PollListRequest, pageable: Pageable): PageResponse<PollSummaryResponse> {
        val page = pollRepository.findAll(PollSpecs.publicList(req), pageable)
        val polls = page.content
        if (polls.isEmpty()) return page.toPageResponse { it.toSummary() }

        val pollIds = polls.map { it.id }

        val totalMap = pollJooqRepository.fetchTotalVotes(pollIds)
        val yesNoMap = pollJooqRepository.fetchYesNoCounts(pollIds)
        val rankMap = pollJooqRepository.fetchRankingTop(pollIds, topN = 5)

        val myVotesMap: Map<Long, List<Long>> =
            if (principal != null) pollJooqRepository.fetchMyVoteOptionIdsMap(pollIds, principal.userId)
            else emptyMap()

        val now = LocalDateTime.now()

        return page.toPageResponse { poll ->
            val totalVotes = totalMap[poll.id] ?: 0L

            val myVoteOptionIds: List<Long>? =
                if (principal != null) (myVotesMap[poll.id] ?: emptyList())
                else null

            val stats = PollStatsPreview(
                totalVotes = totalVotes,
                myVoteOptionIds = myVoteOptionIds,
                endsInSeconds = poll.endsAt?.let { java.time.Duration.between(now, it).seconds.coerceAtLeast(0) }
            )

            val preview: PollPreview? = when (poll.pollType) {
                PollType.VOTE -> yesNoMap[poll.id]?.toYesNoPreview()
                PollType.RANK -> rankMap[poll.id]?.toRankingPreview(topN = 5)
            }

            poll.toSummary(stats = stats, preview = preview)
        }
    }

    @Transactional(readOnly = true)
    fun detail(principal: JwtPrincipal?, pollId: Long): PollDetailResponse {
        val poll = pollRepository.findById(pollId).orElseThrow { ApiException(ErrorCode.POLL_NOT_FOUND) }

        val options = optionRepository.findAllByPollIdOrderBySortOrderAscIdAsc(pollId)

        val totalVotes = pollJooqRepository.fetchTotalVotes(pollId)

        val myVoteOptionIds =
            if (principal != null) pollJooqRepository.fetchMyVoteOptionIds(pollId, principal.userId)
            else null


        val stats = PollStatsPreview(
            totalVotes = totalVotes,
            myVoteOptionIds = myVoteOptionIds,
            endsInSeconds = poll.endsAt?.let { java.time.Duration.between(LocalDateTime.now(), it).seconds.coerceAtLeast(0) }
        )

        val optionCounts = pollJooqRepository.fetchPollOptionCounts(pollId)

        val results: PollResultsResponse? = when (poll.pollType) {
            PollType.VOTE -> buildYesNoResults(optionCounts, totalVotes)
            PollType.RANK -> buildRankingResults(optionCounts, totalVotes)
        }

        // ✅ DTO에 stats/results를 포함해서 내려주기
        return poll.toDetail(options, stats = stats, results = results)
    }

    @Transactional
    fun update(userId: Long, pollId: Long, req: PollUpdateRequest): PollDetailResponse {
        val poll = pollRepository.findById(pollId).orElseThrow { ApiException(ErrorCode.POLL_NOT_FOUND) }
        if (poll.creatorUserId != userId) throw ApiException(ErrorCode.FORBIDDEN)

        req.title?.let { poll.title = it.trim() }
        if (req.description != null) {
            poll.description = req.description.trim().takeIf { it.isNotBlank() }
        }
        req.categoryId?.let { poll.categoryId = it }
        req.visibility?.let { poll.visibility = it }
        req.allowAnonymous?.let { poll.allowAnonymous = it }
        req.allowChange?.let { poll.allowChange = it }
        req.endsAt?.let { poll.endsAt = it }

        val options = optionRepository.findAllByPollIdOrderBySortOrderAscIdAsc(pollId)
        return poll.toDetail(options)
    }

    @Transactional
    fun delete(userId: Long, pollId: Long) {
        val poll = pollRepository.findById(pollId).orElseThrow { ApiException(ErrorCode.POLL_NOT_FOUND) }
        if (poll.creatorUserId != userId) throw ApiException(ErrorCode.FORBIDDEN)

        // soft delete 쓰면 여기서 deletedAt 세팅으로 변경 권장
        pollRepository.delete(poll)
    }

    @Transactional
    fun voteAsUser(pollId: Long, userId: Long, req: VoteRequest) {
        val poll = pollRepository.findById(pollId).orElseThrow { ApiException(ErrorCode.POLL_NOT_FOUND) }
        validateOpen(poll)

        val optionIds = normalizeOptionIds(req.optionIds, poll.maxSelections)
        ensureOptionsBelongToPoll(pollId, optionIds)

        val existing = voteRepository.countByPollIdAndUserId(pollId, userId)
        if (existing > 0) {
            if (!poll.allowChange) throw ApiException(ErrorCode.POLL_CHANGE_NOT_ALLOWED)
            voteRepository.deleteAllByPollIdAndUserId(pollId, userId)
        }

        voteRepository.saveAll(optionIds.map { Vote(pollId = pollId, optionId = it, userId = userId) })
    }

    @Transactional
    fun voteAsAnonymous(pollId: Long, anonymousKey: String, req: VoteRequest) {
        val poll = pollRepository.findById(pollId).orElseThrow { ApiException(ErrorCode.POLL_NOT_FOUND) }
        validateOpen(poll)

        if (!poll.allowAnonymous) throw ApiException(ErrorCode.POLL_ANONYMOUS_NOT_ALLOWED)

        val optionIds = normalizeOptionIds(req.optionIds, poll.maxSelections)
        ensureOptionsBelongToPoll(pollId, optionIds)

        val existing = voteRepository.countByPollIdAndAnonymousKey(pollId, anonymousKey)
        if (existing > 0) throw ApiException(ErrorCode.POLL_ANONYMOUS_CHANGE_NOT_ALLOWED)

        voteRepository.saveAll(optionIds.map { Vote(pollId = pollId, optionId = it, anonymousKey = anonymousKey) })
    }

    private fun validateOpen(poll: Poll) {
        poll.endsAt?.let {
            if (it.isBefore(LocalDateTime.now())) throw ApiException(ErrorCode.POLL_CLOSED)
        }
    }

    private fun normalizeOptionIds(optionIds: List<Long>, maxSelections: Int): List<Long> {
        val ids = optionIds.distinct()
        if (ids.isEmpty()) throw ApiException(ErrorCode.POLL_INVALID_OPTIONS)
        if (ids.size > maxSelections) throw ApiException(ErrorCode.POLL_MAX_SELECTION_EXCEEDED)
        return ids
    }

    private fun ensureOptionsBelongToPoll(pollId: Long, optionIds: List<Long>) {
        val found = optionRepository.findAllByPollIdAndIdIn(pollId, optionIds)
        if (found.size != optionIds.size) throw ApiException(ErrorCode.POLL_OPTION_NOT_FOUND)
    }

    private fun Poll.toSummary(
        stats: PollStatsPreview? = null,
        preview: PollPreview? = null,
    ): PollSummaryResponse =
        PollSummaryResponse(
            id = id,
            title = title,
            pollType = pollType,
            visibility = visibility,
            categoryId = categoryId,
            endsAt = endsAt,
            createdAt = createdAt,
            stats = stats,
            preview = preview,
        )

    private fun Poll.toDetail(
        options: List<PollOption>,
        stats: PollStatsPreview? = null,
        results: PollResultsResponse? = null,
    ): PollDetailResponse =
        PollDetailResponse(
            id = id,
            creatorUserId = creatorUserId,
            title = title,
            description = description,
            pollType = pollType,
            visibility = visibility,
            categoryId = categoryId,
            allowAnonymous = allowAnonymous,
            allowChange = allowChange,
            maxSelections = maxSelections,
            endsAt = endsAt,
            createdAt = createdAt,
            options = options.map { PollOptionResponse(it.id, it.text, it.sortOrder) },

            stats = stats,
            results = results,
        )
}
