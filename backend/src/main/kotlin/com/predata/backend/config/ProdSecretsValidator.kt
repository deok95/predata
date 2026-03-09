package com.predata.backend.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment

/**
 * prod/staging 프로필에서만 활성화되는 시크릿 강도 및 조건부 필수 검증기.
 *
 * 검증 항목:
 *  - JWT 서명 키: 최소 32바이트 (256비트)
 *  - DB 패스워드: 반드시 설정, 최소 12자
 *  - 메일 패스워드: auth.demo-mode=false 이면 필수
 *  - Google OAuth: Google 로그인이 활성화된 경우 clientId/secret 필수
 *  - Polygon 프라이빗 키: 값이 존재하면 0x + 64 hex 형식 필수
 *
 * 모든 위반 항목을 수집한 뒤 한 번에 예외를 던져 운영자가 전체 현황을 한눈에 파악할 수 있게 한다.
 */
class ProdSecretsValidator : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val profileProperty = environment.getProperty("spring.profiles.active", "") ?: ""
        val envProfile = System.getenv("SPRING_PROFILES_ACTIVE").orEmpty()
        val profiles = buildSet {
            addAll(environment.activeProfiles)
            addAll(parseProfiles(profileProperty))
            addAll(parseProfiles(envProfile))
        }
        if ("prod" !in profiles && "staging" !in profiles) return

        val errors = mutableListOf<String>()

        validateJwtSecrets(environment, errors)
        validateDbPassword(environment, errors)
        validateMailPassword(environment, errors)
        validateGoogleOAuth(environment, errors)
        validatePolygonPrivateKey(environment, errors)

        val distinctErrors = errors.distinct()
        if (distinctErrors.isNotEmpty()) {
            throw IllegalStateException(buildErrorMessage(distinctErrors))
        }
    }

    private fun validateJwtSecrets(environment: ConfigurableEnvironment, errors: MutableList<String>) {
        val secrets = Binder.get(environment)
            .bind("jwt.secrets", Bindable.listOf(String::class.java))
            .orElse(emptyList())
        val activeSecrets = secrets
            .mapIndexedNotNull { index, secret ->
                val normalized = secret.trim()
                if (normalized.isBlank()) null else index to normalized
            }

        if (activeSecrets.isEmpty()) {
            errors += "JWT secret must not be empty"
            return
        }

        activeSecrets.forEach { (index, secret) ->
            if (secret.toByteArray(Charsets.UTF_8).size < 32) {
                errors += "JWT secret must be at least 32 characters (jwt.secrets[$index])"
            }
        }
    }

    private fun validateDbPassword(environment: ConfigurableEnvironment, errors: MutableList<String>) {
        val password = environment.getProperty("spring.datasource.password", "") ?: ""
        if (password.isBlank()) {
            errors += "DB_PASSWORD must not be blank in production"
            return
        }
        if (password.length < 12) {
            errors += "DB_PASSWORD must be at least 12 characters"
        }
    }

    private fun validateMailPassword(environment: ConfigurableEnvironment, errors: MutableList<String>) {
        val demoMode = environment.getProperty("auth.demo-mode", Boolean::class.java, false)
        if (demoMode) return
        val mailPassword = environment.getProperty("spring.mail.password", "") ?: ""
        if (mailPassword.isBlank()) {
            errors += "MAIL_PASSWORD is required when auth.demo-mode=false"
        }
    }

    private fun validateGoogleOAuth(environment: ConfigurableEnvironment, errors: MutableList<String>) {
        val springOAuthClientId = environment.getProperty(
            "spring.security.oauth2.client.registration.google.client-id",
            "",
        ) ?: ""
        val isGoogleOAuthEnabled = springOAuthClientId.isNotBlank()
        if (!isGoogleOAuthEnabled) return

        val oauthClientId = environment.getProperty("google.oauth.client-id", "") ?: ""
        val oauthClientSecret = environment.getProperty("google.oauth.client-secret", "") ?: ""

        if (oauthClientId.isBlank()) {
            errors += "GOOGLE_CLIENT_ID (google.oauth.client-id) is required when Google OAuth is enabled"
        }
        if (oauthClientSecret.isBlank()) {
            errors += "GOOGLE_CLIENT_SECRET (google.oauth.client-secret) is required when Google OAuth is enabled"
        }
    }

    private fun validatePolygonPrivateKey(environment: ConfigurableEnvironment, errors: MutableList<String>) {
        val key = environment.getProperty("polygon.sender-private-key", "") ?: ""
        if (key.isBlank()) return
        if (!HEX_PRIVATE_KEY_PATTERN.matches(key)) {
            errors += "SENDER_PRIVATE_KEY must be 0x followed by exactly 64 hex characters (e.g. 0xabc...def)"
        }
    }

    private fun buildErrorMessage(errors: List<String>): String =
        "[FATAL] 시크릿 검증 실패 (${errors.size}건):\n" +
            errors.joinToString("\n") { "  - $it" }

    private fun parseProfiles(value: String): Set<String> =
        value.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    companion object {
        private val HEX_PRIVATE_KEY_PATTERN = Regex("^0x[0-9a-fA-F]{64}$")
    }
}
