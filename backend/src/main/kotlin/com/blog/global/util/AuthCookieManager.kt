package com.blog.global.util

import com.blog.global.security.CookieProperties
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieManager(
    private val cookieProps: CookieProperties,
) {
    fun setAccess(res: HttpServletResponse, token: String, maxAgeSec: Long) {
        res.addHeader("Set-Cookie",
            ResponseCookie.from(cookieProps.accessTokenName, token)
                .httpOnly(true).secure(cookieProps.secure).sameSite(cookieProps.sameSite)
                .path("/").maxAge(maxAgeSec).build().toString()
        )
    }

    fun setRefresh(res: HttpServletResponse, token: String, maxAgeSec: Long) {
        res.addHeader("Set-Cookie",
            ResponseCookie.from(cookieProps.refreshTokenName, token)
                .httpOnly(true).secure(cookieProps.secure).sameSite(cookieProps.sameSite)
                .path("/").maxAge(maxAgeSec).build().toString()
        )
    }

    fun clear(res: HttpServletResponse) {
        fun del(name: String) = res.addHeader("Set-Cookie",
            ResponseCookie.from(name, "")
                .httpOnly(true).secure(cookieProps.secure).sameSite(cookieProps.sameSite)
                .path("/").maxAge(0).build().toString()
        )
        del(cookieProps.accessTokenName); del(cookieProps.refreshTokenName)
    }
}
