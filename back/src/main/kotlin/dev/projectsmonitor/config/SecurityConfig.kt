package dev.projectsmonitor.config

import dev.projectsmonitor.auth.ApiTokenFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.CorsFilter

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, properties: AppProperties): SecurityFilterChain = http
        .cors(Customizer.withDefaults())
        .csrf { it.disable() }
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .addFilterAfter(ApiTokenFilter(properties.apiToken), CorsFilter::class.java)
        .authorizeHttpRequests {
            it.requestMatchers("/", "/actuator/health/**", "/docs/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            it.requestMatchers(*ApiTokenFilter.PROTECTED_PATHS).permitAll()
            it.anyRequest().denyAll()
        }
        .build()
}
