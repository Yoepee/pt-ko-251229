package com.blog.domain.category.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "categories",
    indexes = [
        Index(name = "idx_categories_user_id", columnList = "user_id"),
        Index(name = "idx_categories_parent_id", columnList = "parent_id"),
        Index(name = "idx_categories_depth", columnList = "depth")
    ],
    uniqueConstraints = [
        // 같은 유저 내에서 같은 부모 아래 같은 이름 금지 (형제 unique)
        UniqueConstraint(columnNames = ["user_id", "parent_id", "name"]),
        // slug를 쓴다면 유저 범위 unique
        UniqueConstraint(columnNames = ["user_id", "slug"])
    ]
)
class Category (

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = true, length = 80)
    var slug: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    @Column(nullable = false)
    var depth: Int = 0, // 0(root) or 1(child)

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
) : BaseEntity() {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}