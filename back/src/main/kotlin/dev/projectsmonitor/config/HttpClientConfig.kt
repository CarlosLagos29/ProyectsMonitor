package dev.projectsmonitor.config

import dev.projectsmonitor.check.RedirectFollower
import dev.projectsmonitor.ssrf.IpAddressValidator
import dev.projectsmonitor.ssrf.SafeDns
import dev.projectsmonitor.ssrf.SafeUrlInterceptor
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.Proxy

@Configuration
class HttpClientConfig {

    @Bean
    fun httpClient(properties: HealthCheckProperties, validator: IpAddressValidator): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(properties.timeouts.connect)
            .readTimeout(properties.timeouts.read)
            .followRedirects(false)
            .followSslRedirects(false)
            .dns(SafeDns(validator))
            .proxy(Proxy.NO_PROXY)
            .addInterceptor(SafeUrlInterceptor(validator, properties.allowedPorts))
            .build()

    @Bean
    fun redirectFollower(httpClient: OkHttpClient, properties: HealthCheckProperties): RedirectFollower =
        RedirectFollower(httpClient, properties.maxRedirects, properties.timeouts.total)
}
