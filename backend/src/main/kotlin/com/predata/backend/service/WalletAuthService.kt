package com.predata.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.data.redis.core.StringRedisTemplate
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class WalletAuthService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val NONCE_TTL_SECONDS = 300L
        private val WALLET_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
        private const val REDIS_KEY_PREFIX = "wallet:nonce:"
    }

    enum class WalletAuthPurpose {
        LOGIN,
        LINK
    }

    data class WalletNonceRecord(
        val nonce: String,
        val walletAddress: String,
        val purpose: WalletAuthPurpose,
        val message: String,
        val memberId: Long?,
        val expiresAt: Instant,
    )

    data class WalletVerificationResult(
        val walletAddress: String,
        val memberId: Long?,
    )

    private val logger = LoggerFactory.getLogger(WalletAuthService::class.java)
    private val nonceStore = ConcurrentHashMap<String, WalletNonceRecord>()

    fun issueNonce(
        walletAddress: String,
        purpose: WalletAuthPurpose,
        memberId: Long? = null
    ): Map<String, Any> {
        val normalizedWallet = normalizeWallet(walletAddress)
        if (!WALLET_REGEX.matches(normalizedWallet)) {
            throw IllegalArgumentException("Invalid wallet address.")
        }

        val nonce = UUID.randomUUID().toString().replace("-", "")
        val now = Instant.now()
        val expiresAt = now.plusSeconds(NONCE_TTL_SECONDS)
        val message = buildMessage(normalizedWallet, purpose, nonce, now)

        nonceStore[nonce] = WalletNonceRecord(
            nonce = nonce,
            walletAddress = normalizedWallet,
            purpose = purpose,
            message = message,
            memberId = memberId,
            expiresAt = expiresAt,
        )
        saveToRedis(nonce, nonceStore[nonce]!!)

        return mapOf(
            "walletAddress" to normalizedWallet,
            "purpose" to purpose.name,
            "nonce" to nonce,
            "message" to message,
            "expiresInSeconds" to NONCE_TTL_SECONDS,
            "expiresAt" to expiresAt.toString(),
        )
    }

    fun verifyAndConsumeNonce(
        walletAddress: String,
        nonce: String,
        message: String,
        signature: String,
        expectedPurpose: WalletAuthPurpose,
        expectedMemberId: Long? = null
    ): WalletVerificationResult {
        val normalizedWallet = normalizeWallet(walletAddress)
        val normalizedNonce = nonce.trim()
        val redisRecord = consumeFromRedis(normalizedNonce)
        if (redisRecord != null) {
            nonceStore.remove(normalizedNonce)
        }
        val record = redisRecord ?: nonceStore.remove(normalizedNonce)
            ?: throw IllegalArgumentException("Wallet verification nonce is invalid or expired.")

        if (Instant.now().isAfter(record.expiresAt)) {
            throw IllegalArgumentException("Wallet verification nonce expired. Please try again.")
        }
        if (record.purpose != expectedPurpose) {
            throw IllegalArgumentException("Wallet verification purpose mismatch.")
        }
        if (!record.walletAddress.equals(normalizedWallet, ignoreCase = true)) {
            throw IllegalArgumentException("Wallet address does not match issued nonce.")
        }
        if (record.message != message) {
            throw IllegalArgumentException("Wallet verification message mismatch.")
        }
        if (expectedMemberId != null && record.memberId != expectedMemberId) {
            throw IllegalArgumentException("Wallet verification member mismatch.")
        }

        val recoveredWallet = recoverWalletAddress(message, signature)
        if (!recoveredWallet.equals(normalizedWallet, ignoreCase = true)) {
            throw IllegalArgumentException("Wallet signature verification failed.")
        }

        return WalletVerificationResult(
            walletAddress = normalizedWallet,
            memberId = record.memberId,
        )
    }

    private fun buildMessage(
        walletAddress: String,
        purpose: WalletAuthPurpose,
        nonce: String,
        issuedAt: Instant
    ): String = buildString {
        append("PRE(D)ATA Wallet ")
        append(if (purpose == WalletAuthPurpose.LOGIN) "Login" else "Link")
        append("\nAddress: ").append(walletAddress)
        append("\nNonce: ").append(nonce)
        append("\nIssued At: ").append(issuedAt)
        append("\nExpires In: ").append(NONCE_TTL_SECONDS).append("s")
    }

    private fun recoverWalletAddress(message: String, signatureHex: String): String {
        val signature = Numeric.cleanHexPrefix(signatureHex)
        if (signature.length != 130) {
            throw IllegalArgumentException("Invalid wallet signature.")
        }

        val r = Numeric.hexStringToByteArray(signature.substring(0, 64))
        val s = Numeric.hexStringToByteArray(signature.substring(64, 128))
        var v = signature.substring(128, 130).toInt(16)
        if (v < 27) {
            v += 27
        }

        val sigData = Sign.SignatureData(v.toByte(), r, s)
        val publicKey = Sign.signedPrefixedMessageToKey(message.toByteArray(Charsets.UTF_8), sigData)
        val recoveredAddress = "0x${Keys.getAddress(publicKey)}"
        return normalizeWallet(recoveredAddress)
    }

    private fun normalizeWallet(raw: String): String {
        val trimmed = raw.trim()
        val withPrefix = if (trimmed.startsWith("0x", ignoreCase = true)) trimmed else "0x$trimmed"
        return withPrefix.lowercase()
    }

    private fun saveToRedis(nonce: String, record: WalletNonceRecord) {
        try {
            redisTemplate.opsForValue().set(
                "$REDIS_KEY_PREFIX$nonce",
                objectMapper.writeValueAsString(record),
                NONCE_TTL_SECONDS,
                TimeUnit.SECONDS,
            )
        } catch (e: Exception) {
            logger.warn("Wallet nonce Redis write failed, using in-memory fallback. nonce={}", nonce, e)
        }
    }

    private fun consumeFromRedis(nonce: String): WalletNonceRecord? {
        return try {
            val key = "$REDIS_KEY_PREFIX$nonce"
            val raw = redisTemplate.opsForValue().getAndDelete(key) ?: return null
            objectMapper.readValue(raw, WalletNonceRecord::class.java)
        } catch (e: Exception) {
            logger.warn("Wallet nonce Redis read failed, falling back to in-memory. nonce={}", nonce, e)
            null
        }
    }
}
