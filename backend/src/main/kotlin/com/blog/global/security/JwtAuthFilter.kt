package com.blog.global.security

import com.blog.global.auth.AuthUserStateStore
import com.blog.global.auth.RefreshTokenStore
import com.blog.global.util.AuthCookieManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val cookieList = request.cookies?.asList().orEmpty()
        val access = cookieList.firstOrNull { it.name == "access_token" }?.value
        val refresh = cookieList.firstOrNull { it.name == "refresh_token" }?.value

        if (!access.isNullOrBlank()) {
            val principal = jwt.tryParseAccess(access)
            if (principal != null && isActiveAndVersionOk(principal)) {
                setAuth(principal)
                filterChain.doFilter(request, response)
                return
            }
        }

        if (!refresh.isNullOrBlank()) {
            runCatching {
                val parsed = jwt.parseRefresh(refresh)

                if (!refreshStore.exists(parsed.jti)) return@runCatching

                if (!isActiveAndVersionOk(parsed.principal)) return@runCatching

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