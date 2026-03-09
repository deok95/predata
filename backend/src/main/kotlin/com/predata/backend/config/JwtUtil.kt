package com.predata.backend.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.predata.backend.config.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

/**
 * JWT 발급 및 검증 유틸.
 *
 * 키 로테이션 지원:
 *  - secrets[0]: 현재 서명 키. 신규 토큰 발급에 사용.
 *  - secrets[1..]: 구 서명 키. 검증 전용 (무중단 로테이션).
 *
 * 각 토큰 헤더에 kid(Key ID)를 포함하며, 검증 시 kid 우선 매칭 후 전체 키 fallback.
 */
@Component
class JwtUtil(private val jwtProperties: JwtProperties) {

    private data class KeyEntry(val kid: String, val key: SecretKey)

    private val signingKeyEntry: KeyEntry
    private val verificationKeyMap: Map<String, SecretKey>

    init {
        require(jwtProperties.activeSecrets.isNotEmpty()) {
            "\n\n[STARTUP FAILURE] JWT_SECRET is not configured.\n" +
                "Generate a secret with:  openssl rand -hex 64\n" +
                "Then set it in your .env:  JWT_SECRET=<generated-value>\n"
        }

        val keyEntries = jwtProperties.activeSecrets.map { secret ->
            val kid = deriveKid(secret)
            KeyEntry(kid, Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8)))
        }

        signingKeyEntry = keyEntries.first()
        verificationKeyMap = keyEntries.associate { it.kid to it.key }
    }

    fun generateToken(memberId: Long, email: String, role: String): String {
        val now = Date()
        return Jwts.builder()
            .header().keyId(signingKeyEntry.kid).and()
            .subject(memberId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(Date(now.time + jwtProperties.expirationMs))
            .signWith(signingKeyEntry.key)
            .compact()
    }

    fun validateAndParse(token: String): Claims? =
        verifyWithKid(token) ?: verifyWithAllKeys(token)

    fun getMemberId(claims: Claims): Long =
        claims.subject?.toLongOrNull()
            ?: throw IllegalArgumentException("JWT subject (memberId) is missing or invalid")

    fun getEmail(claims: Claims): String =
        claims["email"] as? String
            ?: throw IllegalArgumentException("JWT email claim is missing")

    fun getRole(claims: Claims): String =
        claims["role"] as? String
            ?: throw IllegalArgumentException("JWT role claim is missing")

    private fun verifyWithKid(token: String): Claims? {
        val kid = extractKidFromHeader(token) ?: return null
        val key = verificationKeyMap[kid] ?: return null
        return parseSignedClaims(token, key)
    }

    private fun verifyWithAllKeys(token: String): Claims? =
        verificationKeyMap.values.firstNotNullOfOrNull { key ->
            parseSignedClaims(token, key)
        }

    private fun parseSignedClaims(token: String, key: SecretKey): Claims? =
        runCatching {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        }.getOrNull()

    private fun extractKidFromHeader(token: String): String? = runCatching {
        val headerEncoded = token.substringBefore('.')
        val padding = "=".repeat((4 - headerEncoded.length % 4) % 4)
        val headerBytes = Base64.getUrlDecoder().decode(headerEncoded + padding)
        @Suppress("UNCHECKED_CAST")
        val headerMap = objectMapper.readValue(headerBytes, Map::class.java) as Map<String, Any?>
        headerMap["kid"] as? String
    }.getOrNull()

    /**
     * 비밀키의 SHA-256 다이제스트 앞 8자를 kid로 사용.
     * 동일한 키는 항상 동일한 kid를 생성하므로 로테이션 중에도 안정적으로 매칭.
     */
    private fun deriveKid(secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(secret.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).take(8)
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}
