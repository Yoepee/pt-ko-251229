package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "battle_character_skills",
    indexes = [
        Index(name = "ix_battle_skills_character_active", columnList = "character_id,is_active")
    ]
)
class BattleCharacterSkill(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    var character: BattleCharacter,

    @Column(name = "skill_code", nullable = false, length = 50)
    var skillCode: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var trigger: SkillTrigger,

    @Column(name = "cooldown_ms", nullable = false)
    var cooldownMs: Int = 0,

    @Column(name = "version_no", nullable = false)
    var versionNo: Int = 1,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "effect_def", nullable = false, columnDefinition = "jsonb")
    var effectDef: String = "{}",
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}