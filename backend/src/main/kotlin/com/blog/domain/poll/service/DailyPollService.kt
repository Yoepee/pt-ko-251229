package com.blog.domain.poll.service

import com.blog.domain.poll.dto.request.PollCreateRequest
import com.blog.domain.poll.entity.PollType
import com.blog.domain.poll.entity.PollVisibility
import com.blog.domain.poll.repository.PollJooqRepository
import com.blog.domain.poll.schedular.DailyPollProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty(
    prefix = "custom.daily-poll",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class DailyPollService(
    private val pollService: PollService,
    private val pollJooqRepository: PollJooqRepository,
    private val props: DailyPollProperties,
) {

    @Transactional
    fun createTodayIfAbsent() {
        val today = java.time.LocalDate.now(java.time.ZoneId.of(props.zone))

        for (seq in 1..props.count) {
            if (pollJooqRepository.existsDailyPoll(today, seq)) continue

            val req = buildRequest(seq, today)
            val created = pollService.create(props.creatorUserId, req)

            // daily_polls에 매핑 저장 (day, seq)
            // 유니크 충돌나면(경쟁 상황) created poll은 롤백되거나 별도 정리 필요
            pollJooqRepository.insertDailyPoll(today, seq, created.pollId)
        }
    }

    private fun buildRequest(seq: Int, today: java.time.LocalDate): PollCreateRequest {
        // 1번: 네가 말한 "오늘 기분 어때?"
        if (seq == 1) {
            return PollCreateRequest(
                title = "[오늘의 투표] 오늘 기분 어때? (${today})",
                description = null,
                categoryId = null,
                pollType = PollType.RANK,
                visibility = PollVisibility.PUBLIC,
                allowAnonymous = true,
                allowChange = false,
                maxSelections = 1,
                endsAt = null,
                options = listOf(
                    "기분 좋음",
                    "기분 좋을 예정",
                    "평범",
                    "기분 나쁠 예정",
                    "기분 나쁨"
                )
            )
        }

        // 2번: 예시 하나 더 (원하면 바꾸면 됨)
        return PollCreateRequest(
            title = "[오늘의 투표] 오늘 뭐 먹을래? (${today})",
            description = null,
            categoryId = null,
            pollType = PollType.RANK,
            visibility = PollVisibility.PUBLIC,
            allowAnonymous = true,
            allowChange = false,
            maxSelections = 1,
            endsAt = null,
            options = listOf("한식", "중식", "일식", "양식", "아무거나")
        )
    }
}