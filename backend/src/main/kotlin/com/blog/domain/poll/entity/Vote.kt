package com.blog.domain.poll.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "votes",
    indexes = [
        Index(name = "idx_votes_poll_id", columnList = "poll_id"),
        Index(name = "idx_votes_created_at", columnList = "created_at"),
    ]
)
class Vote(
    @Column(name = "poll_id", nullable = false)
    val pollId: Long,

    @Column(name = "option_id", nullable = false)
    val optionId: Long,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "anonymous_key", length = 64)
    val anonymousKey: String? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}