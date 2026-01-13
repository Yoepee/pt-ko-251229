package com.blog.domain.battle.service

import com.blog.domain.battle.dto.response.LaneSnapshot
import com.blog.domain.battle.entity.BattleTeam

interface BattleRedisPort {
    /** 입력 누적 + 초당 제한. 허용되면 true, 초과면 false */
    fun submitInput(
        matchId: Long,
        userId: Long,
        lane: Int,
        power: Int,
        team: BattleTeam,
        limitPerSec: Int = 20
    ): Boolean

    /** RUNNING 시작 시 endsAt 세팅 + finish zset 등록 */
    fun scheduleFinish(matchId: Long, endsAtEpochMs: Long)

    fun popDueMatches(nowEpochMs: Long, limit: Long = 50): List<Long>

    fun getEndsAt(matchId: Long): Long?
    fun getLaneSnapshot(matchId: Long): LaneSnapshot
    fun clearMatchKeys(matchId: Long)
}