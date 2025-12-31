package com.blog.global.util

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieManager(
    @Value("\${custom.cookie.secure:false}") private val secure: Boolean,
) {
    fun setAccess(res: HttpServletResponse, token: String, maxAgeSec: Long) {
        res.addHeader("Set-Cookie",
            ResponseCookie.from("access_token", token)
                .httpOnly(true).secure(secure).sameSite("Lax")
                .path("/").maxAge(maxAgeSec).build().toString()
        )
    }

    fun setRefresh(res: HttpServletResponse, token: String, maxAgeSec: Long) {
        res.addHeader("Set-Cookie",
            ResponseCookie.from("refresh_token", token)
                .httpOnly(true).secure(secure).sameSite("Lax")
                .path("/").maxAge(maxAgeSec).build().toString()
        )
    }

    fun clear(res: HttpServletResponse) {
        fun del(name: String) = res.addHeader("Set-Cookie",
            ResponseCookie.from(name, "")
                .httpOnly(true).secure(secure).sameSite("Lax")
                .path("/").maxAge(0).build().toString()
        )
        del("access_token"); del("refresh_token")
    }
}
