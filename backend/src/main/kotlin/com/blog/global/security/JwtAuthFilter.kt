package com.blog.global.security

import com.blog.global.auth.AuthUserStateStore
import com.blog.global.auth.RefreshTokenStore
import com.blog.global.util.AuthCookieManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwt: JwtProvider,
    private val props: JwtProperties,
    private val refreshStore: RefreshTokenStore,
    private val cookies: AuthCookieManager,
    private val userStateStore: AuthUserStateStore,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val cookieList = request.cookies?.asList().orEmpty()
        val access = cookieList.firstOrNull { it.name == "access_token" }?.value
        val refresh = cookieList.firstOrNull { it.name == "refresh_token" }?.value

        val traceId = MDC.get("traceId") ?: "-"
        val uri = request.requestURI
        val method = request.method

        if (!access.isNullOrBlank()) {
            val principal = jwt.tryParseAccess(access)
            if (principal != null && isActiveAndVersionOk(principal)) {
                setAuth(principal)
                log.debug("AUTH OK (access) [{} {}] userId={} traceId={}", method, uri, principal.userId, traceId)
                filterChain.doFilter(request, response)
                return
            } else {
                log.debug("AUTH FAIL (access) [{} {}] traceId={}", method, uri, traceId)
            }
        }

        if (!refresh.isNullOrBlank()) {
            runCatching {
                val parsed = jwt.parseRefresh(refresh)

                if (!refreshStore.exists(parsed.jti)) {
                    log.debug("REFRESH FAIL (not found jti) userId={} traceId={}", parsed.principal.userId, traceId)
                    return@runCatching
                }

                if (!isActiveAndVersionOk(parsed.principal)) {
                    log.debug("REFRESH FAIL (tokenVersion mismatch) userId={} traceId={}", parsed.principal.userId, traceId)
                    return@runCatching
                }

                val newAccess = jwt.createAccessToken(
                    userId = parsed.principal.userId,
                    username = parsed.principal.username,
                    roles = parsed.principal.roles,
                    tokenVersion = parsed.principal.tokenVersion,
                )

                val (newRefresh, newJti) = jwt.createRefreshToken(
                    userId = parsed.principal.userId,
                    username = parsed.principal.username,
                    roles = parsed.principal.roles,
                    tokenVersion = parsed.principal.tokenVersion,
                )

                refreshStore.rotate(
                    oldJti = parsed.jti,
                    newJti = newJti,
                    userId = parsed.principal.userId,
                    ttlSec = props.refreshExpireSeconds
                )

                cookies.setAccess(response, newAccess, props.accessExpireSeconds)
                cookies.setRefresh(response, newRefresh, props.refreshExpireSeconds)

                setAuth(parsed.principal)

                log.info("REFRESH OK (rotated) userId={} traceId={}", parsed.principal.userId, traceId)
            }.onFailure { ex ->
                // parse 실패/redis 오류 같은 케이스
                log.debug("REFRESH ERROR [{} {}] traceId={} msg={}", method, uri, traceId, ex.message)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun isActiveAndVersionOk(p: JwtPrincipal): Boolean {
        val serverVer = userStateStore.getVersion(p.userId)
        return p.tokenVersion == serverVer
    }

    private fun setAuth(p: JwtPrincipal) {
        val auth = UsernamePasswordAuthenticationToken(p, null, p.roles.map { SimpleGrantedAuthority(it) })
        SecurityContextHolder.getContext().authentication = auth
    }
}