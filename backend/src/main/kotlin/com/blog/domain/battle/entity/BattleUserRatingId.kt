package com.blog.domain.battle.entity

import java.io.Serializable

data class BattleUserRatingId(
    var season: Long? = null,
    var userId: Long? = null
) : Serializable
