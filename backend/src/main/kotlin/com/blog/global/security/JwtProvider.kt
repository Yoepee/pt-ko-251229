package com.blog.global.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtProvider(
    private val props: JwtProperties
) {
    data class ParsedRefresh(val jti: String, val principal: JwtPrincipal)
    private val key = Keys.hmacShaKeyFor(props.secret.toByteArray())

    fun createAccessToken(userId: Long, username: String, roles: List<String>): String =
        createToken("access", userId, username, roles, props.accessExpireSeconds)

    fun createRefreshToken(userId: Long, username: String, roles: List<String>): Pair<String, String> {
        val jti = UUID.randomUUID().toString()
        val token = createToken("refresh", userId, username, roles, props.refreshExpireSeconds, jti)
        return token to jti
    }

    private fun createToken(
        typ: String,
        userId: Long,
        username: String,
        roles: List<String>,
        expireSeconds: Long,
        jti: String? = null
    ): String {
        val now = Date()
        val exp = Date(now.time + expireSeconds * 1000)

        val b = Jwts.builder()
            .subject(userId.toString())
            .claim("typ", typ)
            .claim("username", username)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(exp)

        if (jti != null) b.id(jti)

        return b.signWith(key).compact()
    }

    fun parseAccess(token: String): JwtPrincipal {
        val claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        require(claims["typ"] == "access") { "not access token" }
        return toPrincipal(claims)
    }

    fun tryParseAccess(token: String): JwtPrincipal? = try {
        parseAccess(token)
    } catch (e: ExpiredJwtException) {
        null
    }

    fun parseRefresh(token: String): ParsedRefresh {
        val claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        require(claims["typ"] == "refresh") { "not refresh token" }
        val jti = claims.id ?: error("refresh token missing jti")
        return ParsedRefresh(jti, toPrincipal(claims))
    }

    private fun toPrincipal(claims: io.jsonwebtoken.Claims): JwtPrincipal {
        val userId = claims.subject.toLong()
        val username = claims["username"].toString()
        val roles = (claims["roles"] as? List<*>)?.map { it.toString() } ?: emptyList()
        return JwtPrincipal(userId, username, roles)
    }
}