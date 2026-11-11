package dev.projectsmonitor.check

import dev.projectsmonitor.common.isDomain
import dev.projectsmonitor.common.isIpLiteral
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.ApiException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.springframework.http.HttpStatus

class InvalidUrlException(message: String) : ApiException(HttpStatus.BAD_REQUEST, ApiErrorReason.INVALID_URL, message)

object UrlNormalizer {

    private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*)://")
    private val ALLOWED_SCHEMES = setOf("http", "https")

    fun normalize(raw: String): HttpUrl {
        val trimmed = raw.trim()
        val scheme = SCHEME.find(trimmed)?.groupValues?.get(1)?.lowercase()

        if (scheme != null && scheme !in ALLOWED_SCHEMES) {
            throw InvalidUrlException("Only http and https URLs are allowed")
        }

        val url = (if (scheme == null) "https://$trimmed" else trimmed).toHttpUrlOrNull()
            ?: throw InvalidUrlException("Malformed URL")

        if (!isIpLiteral(url.host) && !isDomain(url.host)) {
            throw InvalidUrlException("Host ${url.host} is not a valid domain")
        }

        return url
    }
}
