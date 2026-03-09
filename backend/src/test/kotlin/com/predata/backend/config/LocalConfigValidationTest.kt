package com.predata.backend.config

import com.predata.backend.config.properties.JwtProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

/**
 * local 프로필에서 더미 시크릿으로 컨텍스트가 정상 기동하는지 검증한다.
 *
 * 완료조건: local 프로필 + application-local.yml 더미값으로 컨텍스트 로드 성공.
 */
class LocalConfigValidationTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)
        .withPropertyValues(
            "spring.profiles.active=local",
            "spring.flyway.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:testdb",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=none",
            "jwt.secrets[0]=dummy-local-jwt-secret-with-minimum-32-bytes",
        )

    @Test
    fun `local profile loads successfully with dummy secrets`() {
        contextRunner.run { context ->
            assertThat(context.startupFailure).isNull()
            val jwtProperties = context.getBean(JwtProperties::class.java)
            assertThat(jwtProperties.activeSecrets).isNotEmpty
            assertThat(jwtProperties.activeSecrets.first()).isNotBlank()
        }
    }

    @Test
    fun `local JWT secret meets minimum length for HMAC-SHA256`() {
        contextRunner.run { context ->
            assertThat(context.startupFailure).isNull()
            val signingSecret = context.getBean(JwtProperties::class.java).activeSecrets.first()
            assertThat(signingSecret.toByteArray(Charsets.UTF_8).size)
                .describedAs("JWT secret must be at least 32 bytes for HMAC-SHA256")
                .isGreaterThanOrEqualTo(32)
        }
    }

    @Configuration
    @EnableConfigurationProperties(JwtProperties::class)
    private class TestConfig
}
