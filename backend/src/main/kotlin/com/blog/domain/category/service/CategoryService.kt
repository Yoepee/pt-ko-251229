package com.blog.domain.category.service

import com.blog.domain.category.dto.request.CategoryCreateRequest
import com.blog.domain.category.dto.request.CategoryUpdateRequest
import com.blog.domain.category.dto.response.CategoryResponse
import com.blog.domain.category.entity.Category
import com.blog.domain.category.repository.CategoryRepository
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.blog.domain.poll.repository.PollRepository

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val pollRepository: PollRepository,
) {

    @Transactional
    fun create(req: CategoryCreateRequest): CategoryResponse {
        val saved = categoryRepository.save(
            Category(
                name = req.name.trim(),
                slug = normalizeSlug(req.slug),
                sortOrder = req.sortOrder ?: 0
            )
        )
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(): List<CategoryResponse> {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc()
            .map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, req: CategoryUpdateRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { ApiException(ErrorCode.CATEGORY_NOT_FOUND) }

        req.name?.let { category.name = it.trim() }
        if (req.slug != null) category.slug = normalizeSlug(req.slug)
        req.sortOrder?.let { category.sortOrder = it }

        return category.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val category = categoryRepository.findById(id)
            .orElseThrow { ApiException(ErrorCode.CATEGORY_NOT_FOUND) }

        // 투표 연결 존재 시 삭제 불가
        if (pollRepository.existsByCategoryId(category.id)) {
            throw ApiException(ErrorCode.CATEGORY_DELETE_FORBIDDEN_HAS_POLL)
        }

        categoryRepository.delete(category)
    }

    private fun normalizeSlug(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

    private fun Category.toResponse(): CategoryResponse =
        CategoryResponse(
            id = this.id,
            name = this.name,
            slug = this.slug,
            sortOrder = this.sortOrder
        )
}