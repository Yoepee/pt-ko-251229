package com.blog.domain.user.entity

import com.blog.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["username"])
    ]
)
class User(

    @Column(nullable= false, length = 50)
    val username: String,

    @Column(nullable = false, length = 255)
    var password: String,

    @Column(nullable = false, length = 30)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.USER

) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    fun changePassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    fun changeNickname(newNickname: String) {
        this.nickname = newNickname
    }
}