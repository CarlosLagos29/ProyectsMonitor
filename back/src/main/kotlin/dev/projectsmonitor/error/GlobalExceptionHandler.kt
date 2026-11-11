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

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ProblemDetail> {
        LOG.warn("Request refused: status={} reason={} detail={}", ex.status.value(), ex.reason, ex.message.toLogSafe())
        val body = ProblemDetail.forStatusAndDetail(ex.status, ex.message).apply {
            title = ex.status.reasonPhrase
            setProperty("reason", ex.reason)
        }
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
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
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
        val body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found").apply {
            setProperty("reason", ApiErrorReason.NOT_FOUND)
        }
        return handleExceptionInternal(ex, body, headers, HttpStatus.NOT_FOUND, request)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        LOG.warn("Unreadable request body")
        val body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Unreadable request body").apply {
            setProperty("reason", ApiErrorReason.VALIDATION_ERROR)
        }
        return handleExceptionInternal(ex, body, headers, status, request)
    }
}
