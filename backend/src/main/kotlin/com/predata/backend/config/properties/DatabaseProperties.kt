package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * spring.datasource.* 에서 검증에 필요한 값만 바인딩.
 * Spring Boot의 DataSourceAutoConfiguration 과 독립적으로 동작한다.
 */
@ConfigurationProperties(prefix = "spring.datasource")
data class DatabaseProperties(
    val username: String = "root",
    val password: String = "",
    val url: String = "",
)
