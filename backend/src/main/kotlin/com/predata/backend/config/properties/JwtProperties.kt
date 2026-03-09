package com.predata.backend.config.properties

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "jwt")
@Validated
data class JwtProperties(
    @field:NotEmpty(message = "jwt.secrets must contain at least one signing key")
    val secrets: List<String>,
    @field:Positive
    val expirationMs: Long = 86_400_000L,
) {
    val activeSecrets: List<String>
        get() = secrets.filter { it.isNotBlank() }

    val signingSecret: String
        get() = activeSecrets.firstOrNull()
            ?: error("No active JWT secret available")
}
