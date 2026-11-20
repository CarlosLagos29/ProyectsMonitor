package dev.projectsmonitor.error

import dev.projectsmonitor.common.toLogSafe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ProblemDetail> {
        LOG.warn("Request refused: status={} reason={} detail={}", ex.status.value(), ex.reason, ex.message.toLogSafe())
        val body = problem(ex.status, ex.reason, ex.message)
        val response = ResponseEntity.status(ex.status)
        ex.retryAfter?.let { response.header(HttpHeaders.RETRY_AFTER, it.toRetryAfterSeconds().toString()) }
        return response.body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail {
        LOG.error("Unexpected error", ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to if (it.isBindingFailure) "has an invalid value" else it.defaultMessage
        }
        LOG.warn("Invalid request: {}", errors)
        val body = ex.body.apply {
            setProperty("reason", ApiErrorReason.VALIDATION_ERROR)
            setProperty("errors", errors)
        }
        return handleExceptionInternal(ex, body, headers, status, request)
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        LOG.warn("Malformed path or parameter: {}", ex.propertyName)
        val body = problem(HttpStatus.NOT_FOUND, ApiErrorReason.NOT_FOUND, "Resource not found")
        return handleExceptionInternal(ex, body, headers, HttpStatus.NOT_FOUND, request)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        LOG.warn("Unreadable request body")
        val body = problem(HttpStatus.BAD_REQUEST, ApiErrorReason.VALIDATION_ERROR, "Unreadable request body")
        return handleExceptionInternal(ex, body, headers, status, request)
    }
}
