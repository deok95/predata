package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * spring.mail.* 에서 검증에 필요한 값만 바인딩.
 * Spring Boot의 MailAutoConfiguration 과 독립적으로 동작한다.
 */
@ConfigurationProperties(prefix = "spring.mail")
data class MailProperties(
    val host: String = "",
    val username: String = "",
    val password: String = "",
)
