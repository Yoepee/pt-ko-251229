package com.blog.domain.category.controller

import com.blog.domain.category.dto.request.CategoryCreateRequest
import com.blog.domain.category.dto.request.CategoryUpdateRequest
import com.blog.domain.category.dto.response.CategoryResponse
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
    ): ResponseEntity<ApiResponse<CategoryResponse>> {
        val created = categoryService.create(req)
        return ResponseEntity.status(201)
            .body(ApiResponse.created(data = created, message = "카테고리 생성 완료"))
    }

    @GetMapping
    fun list(
    ): ResponseEntity<ApiResponse<List<CategoryResponse>>> {
        val list = categoryService.list()
        return ResponseEntity.ok(ApiResponse.ok(data = list))
    }

    @PatchMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody req: CategoryUpdateRequest,
    ): ResponseEntity<ApiResponse<CategoryResponse>> {
        val updated = categoryService.update(id, req)
        return ResponseEntity.ok(ApiResponse.ok(data = updated, message = "카테고리 수정 완료"))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        categoryService.delete(id)
        return ResponseEntity.ok(ApiResponse.ok(message = "카테고리 삭제 완료"))
    }
}

