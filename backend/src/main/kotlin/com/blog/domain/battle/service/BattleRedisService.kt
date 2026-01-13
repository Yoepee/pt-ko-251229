package com.blog.domain.battle.service

import com.blog.domain.battle.dto.response.LaneSnapshot
import com.blog.domain.battle.entity.BattleTeam
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class BattleRedisService(
    private val redis: StringRedisTemplate,
    private val battleInputLuaScript: DefaultRedisScript<List<*>>
) : BattleRedisPort {

    override fun scheduleFinish(matchId: Long, endsAtEpochMs: Long) {
        redis.opsForValue().set("battle:$matchId:endsAt", endsAtEpochMs.toString())
        redis.opsForZSet().add("battle:finish:zset", matchId.toString(), endsAtEpochMs.toDouble())
    }

    override fun popDueMatches(nowEpochMs: Long, limit: Long): List<Long> {
        val key = "battle:finish:zset"
        val due = redis.opsForZSet()
            .rangeByScore(key, 0.0, nowEpochMs.toDouble(), 0, limit)
            ?: emptySet()

        if (due.isEmpty()) return emptyList()

        // 먼저 제거해서 중복 처리 방지 (MVP용)
        redis.opsForZSet().remove(key, *due.toTypedArray())
        return due.mapNotNull { it.toLongOrNull() }
    }

    override fun getEndsAt(matchId: Long): Long? =
        redis.opsForValue().get("battle:$matchId:endsAt")?.toLongOrNull()

    override fun getLaneSnapshot(matchId: Long): LaneSnapshot {
        val m = redis.opsForHash<String, String>().entries("battle:$matchId:lanes")
        fun v(k: String) = m[k]?.toIntOrNull() ?: 0

        return LaneSnapshot(
            lane0A = v("lane0A"),
            lane1A = v("lane1A"),
            lane2A = v("lane2A"),
            lane0B = v("lane0B"),
            lane1B = v("lane1B"),
            lane2B = v("lane2B"),
            inputsA = v("inputsA"),
            inputsB = v("inputsB"),
        )
    }

    override fun clearMatchKeys(matchId: Long) {
        redis.delete(listOf("battle:$matchId:endsAt", "battle:$matchId:lanes"))
    }

    override fun submitInput(
        matchId: Long,
        userId: Long,
        lane: Int,
        power: Int,
        team: BattleTeam,
        limitPerSec: Int
    ): Boolean {
        // lane 범위 방어 (서비스에서 해도 되지만 Redis 레이어에서도 한 번 더)
        if (lane !in 0..2) return false

        val epochSec = Instant.now().epochSecond
        val rateKey = "battle:$matchId:rl:$userId:$epochSec"
        val lanesKey = "battle:$matchId:lanes"

        // ✅ 팀 분리: lane0A / lane0B ...
        val laneField = "lane${lane}${team.name}"
        val teamInputsField = if (team == BattleTeam.A) "inputsA" else "inputsB"

        val res = redis.execute(
            battleInputLuaScript,
            listOf(rateKey, lanesKey),
            limitPerSec.toString(),
            "2", // ttlSec (초 단위 rate-limit 키의 TTL)
            laneField,
            power.toString(),
            teamInputsField
        ) ?: return false

        val allowed = (res[0] as Number).toInt()
        return allowed == 1
    }
}