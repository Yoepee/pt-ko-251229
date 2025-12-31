package com.blog.domain.user.controller

import com.blog.domain.user.dto.request.ChangeNicknameRequest
import com.blog.domain.user.dto.request.ChangePasswordRequest
import com.blog.domain.user.dto.request.LoginRequest
import com.blog.domain.user.dto.request.SignUpRequest
import com.blog.domain.user.dto.response.MeResponse
import com.blog.domain.user.service.AuthService
import com.blog.domain.user.service.UserService
import com.blog.global.auth.AuthUserStateStore
import com.blog.global.auth.RefreshTokenStore
import com.blog.global.common.ApiResponse
import com.blog.global.security.JwtPrincipal
import com.blog.global.security.JwtProperties
import com.blog.global.security.JwtProvider
import com.blog.global.util.AuthCookieManager
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userService: UserService,
    private val authService: AuthService,
    private val cookies: AuthCookieManager,
    private val refreshTokenStore: RefreshTokenStore,
    private val jwtProps: JwtProperties,
    private val jwtProvider: JwtProvider,
    private val userStateStore: AuthUserStateStore,
) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody req: SignUpRequest): ApiResponse<Long> {
        val id = userService.signUp(req.username, req.password, req.nickname)
        return ApiResponse.created(data = id, message = "회원가입 완료")
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest, res: HttpServletResponse): ApiResponse<Unit> {
        val pair = authService.login(req.username, req.password)

        cookies.setAccess(res, pair.accessToken, jwtProps.accessExpireSeconds)
        cookies.setRefresh(res, pair.refreshToken, jwtProps.refreshExpireSeconds)
        return ApiResponse.ok(message = "로그인 성공")
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = "refresh_token", required = false) refresh: String?,
        res: HttpServletResponse
    ): ApiResponse<Unit> {
        if (!refresh.isNullOrBlank()) {
            runCatching {
                val parsed = jwtProvider.parseRefresh(refresh)
                refreshTokenStore.delete(parsed.jti)
            }
        }
        cookies.clear(res)
        return ApiResponse.ok(message = "로그아웃 완료")
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: JwtPrincipal): ApiResponse<MeResponse> {
        val me = authService.me(principal.userId)
        return ApiResponse.ok(data = me)
    }

    @PatchMapping("/me/nickname")
    fun changeNickname(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @Valid @RequestBody req: ChangeNicknameRequest
    ): ApiResponse<Unit> {
        userService.changeNickname(principal.userId, req.nickname)
        return ApiResponse.ok(message = "닉네임 변경 완료")
    }

    @PatchMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @Valid @RequestBody req: ChangePasswordRequest,
        res: HttpServletResponse
    ): ApiResponse<Unit> {
        userService.changePassword(principal.userId, req.currentPassword, req.newPassword)
        userStateStore.bumpVersion(principal.userId)
        cookies.clear(res)
        return ApiResponse.ok(message = "비밀번호 변경 완료")
    }

    @PostMapping("/withdraw")
    fun withdraw(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @CookieValue(name = "refresh_token", required = false) refresh: String?,
        res: HttpServletResponse
    ): ApiResponse<Unit> {
        authService.withdraw(principal.userId, refresh)
        cookies.clear(res)
        return ApiResponse.ok(message = "회원탈퇴 완료")
    }
}