package com.blog.global.exception

import com.blog.global.common.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException


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

    private fun logValidation(e: MethodArgumentNotValidException, req: HttpServletRequest) {
        val field = e.bindingResult.fieldErrors.firstOrNull()?.field
        val detail = e.bindingResult.fieldErrors.joinToString { "${it.field}:${it.defaultMessage}" }
        log.warn("VALIDATION FAIL [{} {}] field={} detail={}", req.method, req.requestURI, field, detail)
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
        logValidation(e, req)
        return ResponseEntity
            .status(400)
            .body(ApiResponse.fail(status = 400, message = msg))
    }

    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handleAsyncNotUsable(ex: Exception, req: HttpServletRequest): ResponseEntity<Void> {
        // SSE 끊김: 정상 상황이 많음
        log.debug("SSE disconnected [{} {}] msg={}", req.method, req.requestURI, ex.message)
        return ResponseEntity.noContent().build()
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