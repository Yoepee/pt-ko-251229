package com.blog.global.exception

import com.blog.global.common.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun logError(e: Exception, req: HttpServletRequest, status: Int) {
        val method = req.method
        val uri = req.requestURI
        val qs = req.queryString?.let { "?$it" } ?: ""
        val traceId = MDC.get("traceId") // Filter에서 넣는다고 가정

        // 4xx는 warn, 5xx는 error로 분리 추천
        if (status >= 500) {
            log.error("API FAIL [{} {}{}] status={} traceId={} msg={}", method, uri, qs, status, traceId, e.message, e)
        } else {
            log.warn("API FAIL  [{} {}{}] status={} traceId={} msg={}", method, uri, qs, status, traceId, e.message)
        }
    }


    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException, req: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        logError(e, req, e.status)
        return ResponseEntity
            .status(e.status)
            .body(ApiResponse.fail(status = e.status, message = e.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val msg = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: ErrorCode.INVALID_REQUEST.message
        logError(e, req, 400)
        return ResponseEntity
            .status(400)
            .body(ApiResponse.fail(status = 400, message = msg))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, req: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val ec = ErrorCode.INTERNAL_ERROR
        logError(e, req, ec.status)
        return ResponseEntity
            .status(ec.status)
            .body(ApiResponse.fail(status = ec.status, message = ec.message))
    }
}