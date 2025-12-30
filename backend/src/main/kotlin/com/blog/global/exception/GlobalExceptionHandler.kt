package com.blog.global.exception

import com.blog.global.common.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException, req: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(e.status)
            .body(ApiResponse.fail(status = e.status, message = e.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val msg = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: ErrorCode.INVALID_REQUEST.message
        return ResponseEntity
            .status(400)
            .body(ApiResponse.fail(status = 400, message = msg))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, req: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val ec = ErrorCode.INTERNAL_ERROR
        return ResponseEntity
            .status(ec.status)
            .body(ApiResponse.fail(status = ec.status, message = ec.message))
    }
}