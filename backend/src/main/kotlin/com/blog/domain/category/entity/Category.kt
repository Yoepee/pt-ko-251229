package com.blog.domain.category.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "categories",
    indexes = [
        Index(name = "idx_categories_sort_order", columnList = "sort_order")
    ],
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["name"]),
        UniqueConstraint(columnNames = ["slug"])
    ]
)
class Category (

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = true, length = 80)
    var slug: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

) : BaseEntity() {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}