package com.blog.domain.category.service

import com.blog.domain.category.dto.request.CategoryCreateRequest
import com.blog.domain.category.dto.request.CategoryUpdateRequest
import com.blog.domain.category.dto.response.CategoryNodeResponse
import com.blog.domain.category.entity.Category
import com.blog.domain.category.repository.CategoryRepository
import com.blog.domain.post.repository.PostRepository
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val postRepository: PostRepository,
) {

    @Transactional
    fun create(userId: Long, req: CategoryCreateRequest): CategoryNodeResponse {
        val (parent, depth) = resolveParentAndDepth(userId, req.parentId)

        val saved = categoryRepository.save(
            Category(
                userId = userId,
                name = req.name.trim(),
                slug = req.slug?.trim(),
                parent = parent,
                depth = depth,
                sortOrder = req.sortOrder ?: 0
            )
        )

        return saved.toNode()
    }

    @Transactional(readOnly = true)
    fun listTree(userId: Long): List<CategoryNodeResponse> {
        val roots = categoryRepository.findAllByUserIdAndParentIsNullOrderBySortOrderAscIdAsc(userId)
        if (roots.isEmpty()) return emptyList()

        val children = categoryRepository.findAllByUserIdAndParent_IdInOrderBySortOrderAscIdAsc(
            userId = userId,
            parentIds = roots.map { it.id }
        )

        val childrenMap = children.groupBy { it.parent!!.id }

        return roots.map { root ->
            root.toNode(
                children = childrenMap[root.id].orEmpty().map { it.toNode() }
            )
        }
    }

    @Transactional
    fun update(userId: Long, id: Long, req: CategoryUpdateRequest): CategoryNodeResponse {
        val category = categoryRepository.findByIdAndUserId(id, userId)
            ?: throw ApiException(ErrorCode.CATEGORY_NOT_FOUND)

        // parent 이동까지 지원할 경우
        if (req.parentId != null) {
            val (newParent, newDepth) = resolveParentAndDepth(userId, req.parentId)

            // 자기 자신을 부모로 지정 금지
            if (newParent != null && newParent.id == category.id) {
                // 새 에러코드를 추가하는게 가장 깔끔하지만, 일단은 INVALID_PARENT로 처리 가능
                throw ApiException(ErrorCode.CATEGORY_INVALID_PARENT)
            }

            category.parent = newParent
            category.depth = newDepth
        }

        req.name?.let { category.name = it.trim() }
        req.slug?.let { category.slug = it.trim() }
        req.sortOrder?.let { category.sortOrder = it }

        // JPA Dirty Checking으로 save 호출 없어도 반영됨.
        return category.toNode()
    }

    @Transactional
    fun delete(userId: Long, id: Long) {
        val category = categoryRepository.findByIdAndUserId(id, userId)
            ?: throw ApiException(ErrorCode.CATEGORY_NOT_FOUND)

        // 1) 하위 카테고리 존재 시 삭제 불가
        if (categoryRepository.existsByUserIdAndParent_Id(userId, category.id)) {
            throw ApiException(ErrorCode.CATEGORY_DELETE_FORBIDDEN_HAS_CHILD)
        }

        // 2) 게시글 연결 존재 시 삭제 불가
        if (postRepository.existsByUserIdAndCategoryId(userId, category.id)) {
            throw ApiException(ErrorCode.CATEGORY_DELETE_FORBIDDEN_HAS_POST)
        }

        categoryRepository.delete(category)
    }

    private fun resolveParentAndDepth(userId: Long, parentId: Long?): Pair<Category?, Int> {
        if (parentId == null) return null to 0

        val parent = categoryRepository.findByIdAndUserId(parentId, userId)
            ?: throw ApiException(ErrorCode.CATEGORY_NOT_FOUND)

        // depth=2 제한: 부모는 root(depth=0)만 허용
        if (parent.depth != 0) {
            throw ApiException(ErrorCode.CATEGORY_INVALID_PARENT)
        }

        return parent to 1
    }

    private fun Category.toNode(
        children: List<CategoryNodeResponse> = emptyList()
    ): CategoryNodeResponse =
        CategoryNodeResponse(
            id = this.id,
            name = this.name,
            slug = this.slug,
            depth = this.depth,
            sortOrder = this.sortOrder,
            parentId = this.parent?.id,
            children = children
        )
}
