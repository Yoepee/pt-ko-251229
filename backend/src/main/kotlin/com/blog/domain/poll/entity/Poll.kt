package com.blog.domain.poll.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "polls",
    indexes = [
        Index(name = "idx_polls_created_at", columnList = "created_at"),
        Index(name = "idx_polls_visibility", columnList = "visibility"),
        Index(name = "idx_polls_category_id", columnList = "category_id"),
        Index(name = "idx_polls_ends_at", columnList = "ends_at"),
    ]
)
class Poll(
    @Column(name = "creator_user_id", nullable = false)
    val creatorUserId: Long,

    @Column(name = "category_id", nullable = true)
    var categoryId: Long? = null,

    @Column(nullable = false, length = 120)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "poll_type", nullable = false, length = 20)
    var pollType: PollType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var visibility: PollVisibility,

    @Column(name = "allow_anonymous", nullable = false)
    var allowAnonymous: Boolean = true,

    @Column(name = "allow_change", nullable = false)
    var allowChange: Boolean = false,

    @Column(name = "max_selections", nullable = false)
    var maxSelections: Int = 1,

    @Column(name = "ends_at")
    var endsAt: LocalDateTime? = null,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}