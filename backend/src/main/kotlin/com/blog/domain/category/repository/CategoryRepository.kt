package com.blog.domain.category.repository

import com.blog.domain.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findAllByUserIdAndParentIsNullOrderBySortOrderAscIdAsc(userId: Long): List<Category>

    fun findAllByUserIdAndParent_IdInOrderBySortOrderAscIdAsc(
        userId: Long,
        parentIds: List<Long>
    ): List<Category>

    fun existsByUserIdAndParent_Id(userId: Long, parentId: Long): Boolean

    fun findByIdAndUserId(id: Long, userId: Long): Category?
}