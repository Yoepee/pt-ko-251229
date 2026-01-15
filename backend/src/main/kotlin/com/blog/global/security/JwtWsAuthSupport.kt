package com.blog.global.security

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession

@Component
class JwtWsAuthSupport(
    private val jwtProvider: JwtProvider,
    private val cookieProps: CookieProperties,
) {
    fun userIdFromAccessCookie(session: WebSocketSession): Long? {
        val token = session.handshakeInfo.cookies[cookieProps.accessTokenName]
            ?.firstOrNull()
            ?.value
            ?: return null

        return jwtProvider.tryParseAccess(token)?.userId
    }
}