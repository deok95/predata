package com.predata.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.exception.ErrorCode
import com.predata.backend.exception.ErrorResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val oAuth2LoginFailureHandler: OAuth2LoginFailureHandler,
    private val jwtSecurityBridgeFilter: JwtSecurityBridgeFilter,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }  // REST API는 CSRF 불필요
            .sessionManagement { session ->
                // OAuth2 로그인 시 state 저장을 위해 세션 허용
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    response.status = ErrorCode.UNAUTHORIZED.status
                    response.contentType = "application/json"
                    response.characterEncoding = "UTF-8"
                    response.writer.write(objectMapper.writeValueAsString(ErrorResponse.of(ErrorCode.UNAUTHORIZED)))
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/admin/**").authenticated()
                    .anyRequest().permitAll()  // 기존 JWT 인터셉터가 인증 처리
            }
            .addFilterBefore(jwtSecurityBridgeFilter, UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler)
            }

        return http.build()
    }
}
