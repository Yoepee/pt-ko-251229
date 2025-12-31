package com.blog.domain.user.controller

import com.blog.domain.user.dto.request.ChangeNicknameRequest
import com.blog.domain.user.dto.request.ChangePasswordRequest
import com.blog.domain.user.dto.request.LoginRequest
import com.blog.domain.user.dto.request.SignUpRequest
import com.blog.domain.user.dto.response.MeResponse
import com.blog.domain.user.service.AuthService
import com.blog.domain.user.service.UserService
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
) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody req: SignUpRequest): ApiResponse<Long> {
        val id = userService.singUp(req.username, req.password, req.nickname)
        return ApiResponse.created(data = id, message = "회원가입 완료")
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest, res: HttpServletResponse): ApiResponse<Unit> {
        val pair = authService.login(req.username, req.password)

        cookies.setAccess(res, pair.accessToken, jwtProps.accessExpireSeconds)
        cookies.setRefresh(res, pair.refreshToken, jwtProps.refreshExpireSeconds)
        return ApiResponse.ok(message = "로그인 성공")
    }

    /**
     * 무상태 JWT라면 서버에서 할 일이 없음.
     * (진짜 로그아웃을 만들려면 RefreshToken + Redis 블랙리스트 같은 설계가 추가로 필요)
     */
    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = "refresh_token", required = false) refresh: String?,
        res: HttpServletResponse
    ): ApiResponse<Unit> {
        if (!refresh.isNullOrBlank()) {
            val parsed = jwtProvider.parseRefresh(refresh) // jti 추출
            refreshTokenStore.delete(parsed.jti)
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
        @RequestBody req: ChangeNicknameRequest
    ): ApiResponse<Unit> {
        userService.changeNickname(principal.userId, req.nickname)
        return ApiResponse.ok(message = "닉네임 변경 완료")
    }

    @PatchMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @RequestBody req: ChangePasswordRequest,
        res: HttpServletResponse
    ): ApiResponse<Unit> {
        userService.changePassword(principal.userId, req.currentPassword, req.newPassword)
        cookies.clear(res)
        return ApiResponse.ok(message = "비밀번호 변경 완료")
    }
}