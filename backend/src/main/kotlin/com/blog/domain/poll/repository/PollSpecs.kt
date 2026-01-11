package com.blog.domain.poll.repository

import com.blog.domain.poll.dto.request.PollListRequest
import com.blog.domain.poll.entity.Poll
import com.blog.domain.poll.entity.PollVisibility
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object PollSpecs {
    fun publicList(req: PollListRequest): Specification<Poll> = Specification { root, _, cb ->
        val predicates = mutableListOf<Predicate>()

        // 기본: PUBLIC만
        predicates += cb.equal(root.get<PollVisibility>("visibility"), PollVisibility.PUBLIC)

        req.categoryId?.let {
            predicates += cb.equal(root.get<Long>("categoryId"), it)
        }

        req.type?.let {
            predicates += cb.equal(root.get<Any>("pollType"), it)
        }

        req.q?.trim()?.takeIf { it.isNotBlank() }?.let { q ->
            predicates += cb.like(cb.lower(root.get("title")), "%${q.lowercase()}%")
        }

        cb.and(*predicates.toTypedArray())
    }
}