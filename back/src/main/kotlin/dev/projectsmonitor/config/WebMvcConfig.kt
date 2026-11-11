package dev.projectsmonitor.config

import dev.projectsmonitor.auth.AuthController
import dev.projectsmonitor.auth.CurrentUserArgumentResolver
import dev.projectsmonitor.ratelimit.ApiRateLimitInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(private val properties: AppProperties) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserArgumentResolver())
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(ApiRateLimitInterceptor(properties.limits.guest.requests))
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(*AuthController.API_TOKEN_PATHS)
    }
}
