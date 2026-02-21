package kr.io.team.loop.auth.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Profile("dev")
class H2ConsoleSecurityConfig {
    @Bean
    @Order(1)
    fun h2ConsoleFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/h2-console/**")
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }.authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }

        return http.build()
    }
}
