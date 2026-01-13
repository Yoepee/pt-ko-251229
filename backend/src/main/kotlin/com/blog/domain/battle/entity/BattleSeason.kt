package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "battle_seasons")
class BattleSeason(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "starts_at")
    var startsAt: LocalDateTime? = null,

    @Column(name = "ends_at")
    var endsAt: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}