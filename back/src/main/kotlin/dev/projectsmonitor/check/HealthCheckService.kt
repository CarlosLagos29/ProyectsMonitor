package dev.projectsmonitor.check

import dev.projectsmonitor.common.nowMillis
import okhttp3.HttpUrl
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class HealthCheckService(private val redirectFollower: RedirectFollower) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(HealthCheckService::class.java)
        const val USER_AGENT = "ProjectsMonitor/1.0 (+https://github.com/CarlosLagos29/ProjectsMonitor)"
    }

    fun check(target: HttpUrl): HealthCheckResult {
        val start = System.nanoTime()

        val result = try {
            val followed = redirectFollower.execute(headRequest(target))
            HealthCheckMapper.fromResponse(followed, elapsedMs(start), nowMillis())
        } catch (e: FollowFailedException) {
            HealthCheckMapper.fromFailure(e, elapsedMs(start), nowMillis())
        }

        LOG.debug(
            "Checked {}: status={} reason={} httpStatus={} finalUrl={} redirects={} latencyMs={}",
            target,
            result.status,
            result.reason,
            result.httpStatus,
            result.finalUrl,
            result.redirects,
            result.latencyMs,
        )
        return result
    }

    private fun headRequest(url: HttpUrl): Request = Request.Builder()
        .url(url)
        .head()
        .header("User-Agent", USER_AGENT)
        .header("Accept", "*/*")
        .build()

    private fun elapsedMs(start: Long): Int = Duration.ofNanos(System.nanoTime() - start).toMillis().toInt()
}
