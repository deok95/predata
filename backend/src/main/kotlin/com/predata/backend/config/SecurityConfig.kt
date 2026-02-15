package com.predata.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val oAuth2LoginFailureHandler: OAuth2LoginFailureHandler
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }  // REST API는 CSRF 불필요
            .sessionManagement { session ->
                // OAuth2 로그인 시 state 저장을 위해 세션 허용
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()  // 기존 JWT 인터셉터가 인증 처리
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler)
            }

        return http.build()
    }
}
