package com.blog.domain.poll.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "poll_options",
    indexes = [
        Index(name = "idx_poll_options_poll_id", columnList = "poll_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_poll_options_poll_text", columnNames = ["poll_id", "text"])
    ]
)
class PollOption(

    @Column(name = "poll_id", nullable = false)
    val pollId: Long,

    @Column(nullable = false, length = 120)
    var text: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}