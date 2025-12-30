package com.blog.domain.user.controller

import com.blog.domain.user.dto.LoginRequest
import com.blog.domain.user.dto.LoginResponse
import com.blog.domain.user.dto.MeResponse
import com.blog.domain.user.dto.SignUpRequest
import com.blog.domain.user.service.AuthService
import com.blog.domain.user.service.UserService
import com.blog.global.common.ApiResponse
import com.blog.global.security.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userService: UserService,
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody req: SignUpRequest): ApiResponse<Long> {
        val id = userService.singUp(req.username, req.password, req.nickname)
        return ApiResponse.created(data = id, message = "회원가입 완료")
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ApiResponse<LoginResponse> {
        val token = authService.login(req.username, req.password)
        return ApiResponse.ok(data = LoginResponse(accessToken = token), message = "로그인 성공")
    }

    /**
     * 무상태 JWT라면 서버에서 할 일이 없음.
     * (진짜 로그아웃을 만들려면 RefreshToken + Redis 블랙리스트 같은 설계가 추가로 필요)
     */
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        return ApiResponse.ok(message = "로그아웃 완료")
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: JwtPrincipal): ApiResponse<MeResponse> {
        val me = authService.me(principal.userId)
        return ApiResponse.ok(data = me)
    }
}