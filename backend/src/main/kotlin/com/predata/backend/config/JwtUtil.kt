package com.predata.backend.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(memberId: Long, email: String, role: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(memberId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(key)
            .compact()
    }

    fun validateAndParse(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun getMemberId(claims: Claims): Long {
        val subject = claims.subject ?: throw IllegalArgumentException("JWT subject (memberId) is missing")
        return subject.toLong()
    }

    fun getEmail(claims: Claims): String {
        return claims["email"] as? String ?: throw IllegalArgumentException("JWT email claim is missing")
    }

    fun getRole(claims: Claims): String {
        return claims["role"] as? String ?: throw IllegalArgumentException("JWT role claim is missing")
    }
}
