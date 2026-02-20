package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * google.oauth.* 커스텀 설정.
 * Spring Security 의 spring.security.oauth2.* 와 별개로 운용되는 OAuth 클라이언트 정보.
 */
@ConfigurationProperties(prefix = "google.oauth")
data class OAuthProperties(
    val clientId: String = "",
    val clientSecret: String = "",
) {
    val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()
}
