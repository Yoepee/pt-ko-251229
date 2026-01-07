package com.blog.global.util

import com.blog.global.common.PageResponse
import org.springframework.data.domain.Page

fun <T: Any, R> Page<T>.toPageResponse(mapper: (T) -> R): PageResponse<R> =
    PageResponse(
        content = this.content.map(mapper),
        page = this.number,
        size = this.size,
        totalElements = this.totalElements,
        totalPages = this.totalPages,
        hasNext = this.hasNext(),
    )