package com.predata.backend.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment

/**
 * 프로필 오적용을 부팅 최초 단계(DB/Flyway 이전)에 차단한다.
 *
 * APP_ENV=prod 또는 staging 인데 local 프로필이 활성화된 경우 즉시 실패시켜
 * 프로덕션 환경에서 로컬 더미 시크릿이 사용되는 사고를 방지한다.
 */
class ProfileGuard : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication
    ) {
        val appEnv = (System.getenv("APP_ENV")
            ?: environment.getProperty("APP_ENV")
            ?: environment.getProperty("app.env"))
            ?.lowercase()
            ?: return
        val activeProfiles = environment.activeProfiles.map { it.lowercase() }
        val configuredProfiles = environment.getProperty("spring.profiles.active")
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val envProfiles = (System.getenv("SPRING_PROFILES_ACTIVE")
            ?: environment.getProperty("SPRING_PROFILES_ACTIVE"))
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val profiles = (activeProfiles + configuredProfiles + envProfiles).toSet()

        if (appEnv == "prod" && "local" in profiles) {
            throw IllegalStateException(
                "[FATAL] 프로필 불일치: APP_ENV=prod인데 " +
                "SPRING_PROFILES_ACTIVE에 'local'이 포함됨."
            )
        }
        if (appEnv == "staging" && "local" in profiles) {
            throw IllegalStateException(
                "[FATAL] 프로필 불일치: APP_ENV=staging인데 " +
                "SPRING_PROFILES_ACTIVE에 'local'이 포함됨."
            )
        }
    }
}
