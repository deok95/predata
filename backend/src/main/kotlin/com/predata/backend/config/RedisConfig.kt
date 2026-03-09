package com.predata.backend.config

import com.predata.backend.config.properties.QuestionCreditProperties
import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

/**
 * Redis 설정
 * - 장애 탄력성: commandTimeout 500ms → 초과 시 예외, 서비스는 DB fallback으로 처리
 * - Redis 장애가 기능 차단으로 이어지지 않게 QuestionDraftService 내부에서 tryRedis{}로 감쌈
 */
@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(QuestionCreditProperties::class)
class RedisConfig(
    @Value("\${spring.data.redis.host:localhost}") private val host: String,
    @Value("\${spring.data.redis.port:6379}") private val port: Int,
) {

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val standaloneConfig = RedisStandaloneConfiguration(host, port)

        val socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(500))
            .build()

        val clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build()

        val clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(500))
            .clientOptions(clientOptions)
            .build()

        return LettuceConnectionFactory(standaloneConfig, clientConfig)
    }

    @Bean
    fun stringRedisTemplate(factory: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(factory)
}
