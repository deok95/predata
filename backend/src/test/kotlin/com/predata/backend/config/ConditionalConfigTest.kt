package com.predata.backend.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration

class ConditionalConfigTest {

    @Test
    fun `demo-mode=true 이면 MAIL_PASSWORD 누락을 허용한다`() {
        assertDoesNotThrow {
            runWithProd(
                mapOf(
                    "auth.demo-mode" to "true",
                    "spring.mail.password" to "",
                ),
            )
        }
    }

    @Test
    fun `demo-mode=false 이면 MAIL_PASSWORD 누락 시 실패한다`() {
        val exception = assertThrows<Exception> {
            runWithProd(
                mapOf(
                    "auth.demo-mode" to "false",
                    "spring.mail.password" to "",
                ),
            )
        }

        val message = exception.rootCauseMessage()
        assertThat(message).containsIgnoringCase("mail")
        assertThat(message).containsIgnoringCase("password")
    }

    @Test
    fun `Polygon private key 미설정 시 형식 검증을 건너뛴다`() {
        assertDoesNotThrow {
            runWithProd(
                mapOf(
                    "auth.demo-mode" to "true",
                    "spring.mail.password" to "",
                    "polygon.sender-private-key" to "",
                ),
            )
        }
    }

    @Test
    fun `잘못된 형식의 Polygon private key 는 prod 에서 실패한다`() {
        val exception = assertThrows<Exception> {
            runWithProd(
                mapOf(
                    "auth.demo-mode" to "true",
                    "spring.mail.password" to "",
                    "polygon.sender-private-key" to "not-a-valid-hex-key",
                ),
            )
        }

        val message = exception.rootCauseMessage()
        assertThat(message).containsIgnoringCase("sender_private_key")
        assertThat(message).containsIgnoringCase("hex")
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
            "DB_PASSWORD" to "StrongPass123!",
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
