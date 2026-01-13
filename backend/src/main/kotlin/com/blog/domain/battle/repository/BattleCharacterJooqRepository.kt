package com.blog.domain.battle.repository

import com.blog.domain.battle.entity.BattleCharacterRow
import com.blog.jooq.Tables.BATTLE_CHARACTERS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class BattleCharacterJooqRepository(
    private val dsl: DSLContext
) {
    fun findDefaultCharacterId(): Long? =
        dsl.select(BATTLE_CHARACTERS.ID)
            .from(BATTLE_CHARACTERS)
            .where(BATTLE_CHARACTERS.IS_ACTIVE.eq(true))
            .and(BATTLE_CHARACTERS.DELETED_AT.isNull)
            .orderBy(BATTLE_CHARACTERS.ID.asc())
            .limit(1)
            .fetchOne(BATTLE_CHARACTERS.ID)

    fun findCharacterVersionNo(characterId: Long): Int =
        dsl.select(BATTLE_CHARACTERS.VERSION_NO)
            .from(BATTLE_CHARACTERS)
            .where(BATTLE_CHARACTERS.ID.eq(characterId))
            .fetchOne(BATTLE_CHARACTERS.VERSION_NO)!!

    fun listActiveCharacters(): List<BattleCharacterRow> {
        return dsl.select(
            BATTLE_CHARACTERS.ID,
            BATTLE_CHARACTERS.CODE,
            BATTLE_CHARACTERS.NAME,
            BATTLE_CHARACTERS.DESCRIPTION,
            BATTLE_CHARACTERS.VERSION_NO
        )
            .from(BATTLE_CHARACTERS)
            .where(BATTLE_CHARACTERS.IS_ACTIVE.eq(true))
            .and(BATTLE_CHARACTERS.DELETED_AT.isNull)
            .orderBy(BATTLE_CHARACTERS.ID.asc())
            .fetch { r ->
                BattleCharacterRow(
                    id = r.get(BATTLE_CHARACTERS.ID)!!,
                    code = r.get(BATTLE_CHARACTERS.CODE)!!,
                    name = r.get(BATTLE_CHARACTERS.NAME)!!,
                    description = r.get(BATTLE_CHARACTERS.DESCRIPTION),
                    versionNo = r.get(BATTLE_CHARACTERS.VERSION_NO)!!
                )
            }
    }
}