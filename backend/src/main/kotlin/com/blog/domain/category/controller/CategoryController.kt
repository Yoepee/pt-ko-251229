package com.blog.domain.category.controller

import com.blog.domain.category.dto.request.CategoryCreateRequest
import com.blog.domain.category.dto.request.CategoryUpdateRequest
import com.blog.domain.category.dto.response.CategoryNodeResponse
import com.blog.domain.category.service.CategoryService
import com.blog.global.common.ApiResponse
import com.blog.global.security.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val categoryService: CategoryService,
) {

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @Valid @RequestBody req: CategoryCreateRequest,
    ): ResponseEntity<ApiResponse<CategoryNodeResponse>> {
        val created = categoryService.create(principal.userId, req)
        return ResponseEntity.status(201).body(ApiResponse.created(data = created, message = "카테고리 생성 완료"))
    }

    @GetMapping
    fun listTree(
        @AuthenticationPrincipal principal: JwtPrincipal,
    ): ResponseEntity<ApiResponse<List<CategoryNodeResponse>>> {
        val tree = categoryService.listTree(principal.userId)
        return ResponseEntity.ok(ApiResponse.ok(data = tree))
    }

    @PatchMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody req: CategoryUpdateRequest,
    ): ResponseEntity<ApiResponse<CategoryNodeResponse>> {
        val updated = categoryService.update(principal.userId, id, req)
        return ResponseEntity.ok(ApiResponse.ok(data = updated, message = "카테고리 수정 완료"))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        categoryService.delete(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.ok(message = "카테고리 삭제 완료"))
    }
}
