package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "battle_user_ratings",
    indexes = [
        Index(name = "ix_battle_ratings_season_rating", columnList = "season_id,rating")
    ]
)
@IdClass(BattleUserRatingId::class)
class BattleUserRating(

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    var season: BattleSeason,

    @Id
    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false)
    var rating: Int = 1500,

    @Column(nullable = false)
    var matches: Int = 0,

    @Column(nullable = false)
    var wins: Int = 0,

    @Column(nullable = false)
    var losses: Int = 0,

    @Column(nullable = false)
    var draws: Int = 0,
) : BaseTimeEntity()