package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "battle_rating_history",
    indexes = [
        Index(name = "ix_battle_rating_history_user_time", columnList = "user_id,created_at"),
        Index(name = "ix_battle_rating_history_match", columnList = "match_id")
    ]
)
class BattleRatingHistory(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    var season: BattleSeason,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: BattleMatch,

    @Column(name = "rating_before", nullable = false)
    var ratingBefore: Int,

    @Column(name = "rating_after", nullable = false)
    var ratingAfter: Int,

    @Column(nullable = false)
    var delta: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var reason: RatingHistoryReason,

    @Column(name = "vs_bot", nullable = false)
    var vsBot: Boolean = false,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}