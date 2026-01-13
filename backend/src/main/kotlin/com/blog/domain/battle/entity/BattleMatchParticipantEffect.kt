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
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "battle_match_participant_effects")
class BattleMatchParticipantEffect(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    var participant: BattleMatchParticipant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    var skill: BattleCharacterSkill,

    @Column(name = "effect_type", nullable = false, length = 40)
    var effectType: String,

    @Column(nullable = false, columnDefinition = "jsonb")
    var params: String = "{}",  // 추후 Map으로 바꿔도 됨

    @Column(name = "starts_at")
    var startsAt: LocalDateTime? = null,

    @Column(name = "ends_at")
    var endsAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "applied_by", nullable = false, length = 10)
    var appliedBy: EffectAppliedBy,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}