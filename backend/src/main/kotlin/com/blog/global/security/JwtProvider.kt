package com.blog.global.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtProvider(
    private val props: JwtProperties
) {
    private val key = Keys.hmacShaKeyFor(props.secret.toByteArray())

    fun createAccessToken(userId: Long, username: String, roles: List<String>): String {
        val now = Date()
        val exp = Date(now.time + props.expireSeconds * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(exp)
            .signWith(key)
            .compact()
    }

    fun parse(token: String): JwtPrincipal {
        val claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload

        val userId = claims.subject.toLong()
        val username = claims["username"].toString()

        // roles는 List로 안전하게 파싱
        val roles = (claims["roles"] as? List<*>)?.map { it.toString() } ?: emptyList()

        return JwtPrincipal(userId = userId, username = username, roles = roles)
    }
}