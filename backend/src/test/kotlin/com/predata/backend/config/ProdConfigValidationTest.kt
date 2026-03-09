package com.predata.backend.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration

class ProdConfigValidationTest {

    @Test
    fun `prod context fails when JWT secret is too short`() {
        val exception = assertThrows<Exception> {
            runWithProd(
                mapOf(
                    "JWT_SECRET" to "weak",
                    "jwt.secrets[0]" to "weak",
                    "spring.datasource.password" to "StrongPass123!",
                ),
            )
        }

        val message = exception.rootCauseMessage()
        assertThat(message).containsIgnoringCase("jwt")
        assertThat(message).containsIgnoringCase("32")
    }

    @Test
    fun `prod context fails when DB password is too short`() {
        val exception = assertThrows<Exception> {
            runWithProd(
                mapOf(
                    "JWT_SECRET" to "a-very-strong-secret-key-that-is-at-least-32-characters-long",
                    "jwt.secrets[0]" to "a-very-strong-secret-key-that-is-at-least-32-characters-long",
                    "DB_PASSWORD" to "short",
                    "spring.datasource.password" to "short",
                ),
            )
        }

        val message = exception.rootCauseMessage()
        assertThat(message).containsIgnoringCase("db_password")
        assertThat(message).containsIgnoringCase("12")
    }

    private fun runWithProd(overrides: Map<String, Any>) {
        val app = SpringApplication(TestApplication::class.java)
        app.setAdditionalProfiles("prod")
        val args = (baseProperties + overrides)
            .map { (k, v) -> "--$k=$v" }
            .toTypedArray()
        app.run(*args).close()
    }

    private fun Throwable.rootCauseMessage(): String {
        var cause: Throwable = this
        while (cause.cause != null) cause = cause.cause!!
        return cause.message ?: ""
    }

    companion object {
        private val baseProperties: Map<String, Any> = mapOf(
            "spring.main.web-application-type" to "none",
            "spring.main.banner-mode" to "off",
            "spring.flyway.enabled" to "false",
            "spring.datasource.url" to "jdbc:h2:mem:testdb",
            "spring.datasource.driver-class-name" to "org.h2.Driver",
            "spring.datasource.username" to "sa",
            "spring.datasource.password" to "StrongPass123!",
            "spring.jpa.hibernate.ddl-auto" to "none",
            "JWT_SECRET" to "a-very-strong-secret-key-that-is-at-least-32-characters-long",
            "jwt.secrets[0]" to "a-very-strong-secret-key-that-is-at-least-32-characters-long",
            "auth.demo-mode" to "true",
            "spring.mail.password" to "",
            "spring.security.oauth2.client.registration.google.client-id" to "",
            "google.oauth.client-id" to "",
            "google.oauth.client-secret" to "",
            "polygon.sender-private-key" to "",
        )
    }

    @SpringBootConfiguration
    private class TestApplication
}
