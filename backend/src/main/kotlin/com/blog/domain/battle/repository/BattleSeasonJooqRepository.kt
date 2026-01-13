package com.blog.domain.battle.repository

import com.blog.jooq.Tables.BATTLE_SEASONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class BattleSeasonJooqRepository(
    private val dsl: DSLContext
) {
    fun findActiveSeasonId(): Long? =
        dsl.select(BATTLE_SEASONS.ID)
            .from(BATTLE_SEASONS)
            .where(BATTLE_SEASONS.IS_ACTIVE.eq(true))
            .and(BATTLE_SEASONS.DELETED_AT.isNull)
            .orderBy(BATTLE_SEASONS.ID.desc())
            .limit(1)
            .fetchOne(BATTLE_SEASONS.ID)
}