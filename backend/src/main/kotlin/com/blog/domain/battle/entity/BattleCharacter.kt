package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "battle_characters",
    indexes = [
        Index(name = "ix_battle_characters_active", columnList = "is_active")
    ]
)
class BattleCharacter(
    @Column(nullable = false, length = 50)
    var code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "version_no", nullable = false)
    var versionNo: Int = 1,

    // jsonb
    @Column(name = "base_stats", nullable = false, columnDefinition = "jsonb")
    var baseStats: String = "{}",
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}