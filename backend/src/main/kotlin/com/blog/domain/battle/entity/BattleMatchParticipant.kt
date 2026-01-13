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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "battle_match_participants",
    uniqueConstraints = [
        UniqueConstraint(name = "ux_battle_participants_match_user", columnNames = ["match_id", "user_id"])
    ],
    indexes = [
        Index(name = "ix_battle_participants_user_joined", columnList = "user_id,joined_at"),
        Index(name = "ix_battle_participants_match_team", columnList = "match_id,team"),
    ]
)
class BattleMatchParticipant(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: BattleMatch,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    var team: BattleTeam,

    @Column(name = "is_bot", nullable = false)
    var isBot: Boolean = false,

    @Column(name = "bot_profile", length = 50)
    var botProfile: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    var character: BattleCharacter,

    @Column(name = "character_version_no", nullable = false)
    var characterVersionNo: Int = 1,

    @Column(name = "character_snapshot", columnDefinition = "jsonb")
    var characterSnapshot: String? = null,

    @Column(name = "joined_at")
    var joinedAt: LocalDateTime? = null,

    @Column(name = "left_at")
    var leftAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}