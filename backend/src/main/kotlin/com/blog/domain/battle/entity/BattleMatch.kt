package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "battle_matches",
    indexes = [
        Index(name = "ix_battle_matches_season_created", columnList = "season_id,created_at"),
        Index(name = "ix_battle_matches_status_created", columnList = "status,created_at"),
        Index(name = "ix_battle_matches_mode_status", columnList = "mode,status"),
    ]
)
class BattleMatch(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    var season: BattleSeason,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 10)
    var matchType: BattleMatchType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var mode: BattleMode,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: BattleMatchStatus = BattleMatchStatus.WAITING,

    @Column(nullable = false)
    var lanes: Int = 3,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Int = 30000,

    @Column(name = "p_max", nullable = false)
    var pMax: Int = 100,

    @Column(name = "focus_lane")
    var focusLane: Int? = null,

    @Column(name = "created_by_user_id")
    var createdByUserId: Long? = null,

    @Column(name = "has_bot", nullable = false)
    var hasBot: Boolean = false,

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}