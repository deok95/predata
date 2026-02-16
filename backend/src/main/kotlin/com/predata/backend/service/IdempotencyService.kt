package com.predata.backend.service

import com.predata.backend.domain.IdempotencyKey
import com.predata.backend.repository.IdempotencyKeyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

/**
 * 멱등성 서비스
 * - 중복 요청 감지 및 처리
 */
@Service
class IdempotencyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository
) {

    /**
     * 중복 요청 체크
     * - 기존 요청이 있으면 반환
     * - 없으면 null 반환
     */
    fun checkAndStore(
        idempotencyKey: String,
        memberId: Long,
        endpoint: String,
        requestBody: String
    ): IdempotencyKey? {
        val requestHash = hashRequest(requestBody)

        return idempotencyKeyRepository.findByIdempotencyKeyAndMemberIdAndEndpoint(
            idempotencyKey, memberId, endpoint
        )?.let { existing ->
            // 요청 본문이 다르면 에러
            if (existing.requestHash != requestHash) {
                throw IllegalStateException("Idempotency key conflict: request body mismatch")
            }
            existing  // 중복 요청 → 기존 응답 반환
        }
    }

    /**
     * 응답 저장
     */
    @Transactional
    fun store(
        idempotencyKey: String,
        memberId: Long,
        endpoint: String,
        requestBody: String,
        responseBody: String,
        responseStatus: Int
    ) {
        val requestHash = hashRequest(requestBody)

        idempotencyKeyRepository.save(
            IdempotencyKey(
                idempotencyKey = idempotencyKey,
                memberId = memberId,
                endpoint = endpoint,
                requestHash = requestHash,
                responseBody = responseBody,
                responseStatus = responseStatus
            )
        )
    }

    private fun hashRequest(requestBody: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(requestBody.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
