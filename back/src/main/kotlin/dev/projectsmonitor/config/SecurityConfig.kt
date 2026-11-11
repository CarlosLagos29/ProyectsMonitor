package dev.projectsmonitor.config

import dev.projectsmonitor.auth.AuthController
import dev.projectsmonitor.auth.token.ApiTokenFilter
import dev.projectsmonitor.error.ForbiddenForRoleHandler
import dev.projectsmonitor.error.SessionExpiredEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.time.Duration

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        properties: AppProperties,
        bearerTokenResolver: BearerTokenResolver,
    ): SecurityFilterChain {
        val entryPoint = SessionExpiredEntryPoint()
        val accessDeniedHandler = ForbiddenForRoleHandler()
        return http
            .cors(Customizer.withDefaults())
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .anonymous(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterAfter(ApiTokenFilter(properties.apiToken), CorsFilter::class.java)
            .oauth2ResourceServer {
                it.bearerTokenResolver(bearerTokenResolver)
                it.jwt(Customizer.withDefaults())
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/",
                    "/error",
                    "/actuator/health/**",
                    "/docs/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                )
                    .permitAll()
                it.requestMatchers(*AuthController.API_TOKEN_PATHS).permitAll()
                it.requestMatchers("/api/v1/**").authenticated()
                it.anyRequest().denyAll()
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(properties: AppProperties): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = properties.cors.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE)
            exposedHeaders = listOf(HttpHeaders.RETRY_AFTER)
            setMaxAge(Duration.ofHours(1))
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", configuration) }
    }
}
