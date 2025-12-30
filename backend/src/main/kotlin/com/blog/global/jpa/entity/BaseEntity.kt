package com.blog.global.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity : BaseTimeEntity() {
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    @Version
    var version: Long? = null
        protected set

    fun isDeleted(): Boolean = deletedAt != null

    fun softDelete(now: LocalDateTime = LocalDateTime.now()) {
        this.deletedAt = now
    }

    fun restore() {
        this.deletedAt = null
    }
}