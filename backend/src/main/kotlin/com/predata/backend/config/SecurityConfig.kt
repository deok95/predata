package com.predata.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }  // REST API는 CSRF 불필요
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()  // 기존 JWT 인터셉터가 인증 처리
            }

        return http.build()
    }
}
