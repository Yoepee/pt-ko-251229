package com.blog.global.security

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
    private val cookies: AuthCookieManager
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val access = request.cookies?.firstOrNull { it.name == "access_token" }?.value
        val refresh = request.cookies?.firstOrNull { it.name == "refresh_token" }?.value

        // 1) access 유효하면 인증 세팅
        if (!access.isNullOrBlank()) {
            val principal = jwt.tryParseAccess(access)
            if (principal != null) {
                setAuth(principal)
                filterChain.doFilter(request, response)
                return
            }
        }

        // 2) access 만료/없음 → refresh로 access 재발급
        if (!refresh.isNullOrBlank()) {
            runCatching {
                val parsed = jwt.parseRefresh(refresh)
                if (!refreshStore.exists(parsed.jti)) return@runCatching

                val newAccess = jwt.createAccessToken(
                    userId = parsed.principal.userId,
                    username = parsed.principal.username,
                    roles = parsed.principal.roles
                )
                cookies.setAccess(response, newAccess, props.accessExpireSeconds)
                setAuth(parsed.principal)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun setAuth(p: JwtPrincipal) {
        val auth = UsernamePasswordAuthenticationToken(p, null, p.roles.map { SimpleGrantedAuthority(it) })
        SecurityContextHolder.getContext().authentication = auth
    }
}