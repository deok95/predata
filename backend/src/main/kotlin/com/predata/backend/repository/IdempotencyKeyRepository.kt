package com.predata.backend.repository

import com.predata.backend.domain.IdempotencyKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface IdempotencyKeyRepository : JpaRepository<IdempotencyKey, Long> {
    /**
     * 멱등성 키 조회 (중복 요청 체크)
     */
    fun findByIdempotencyKeyAndMemberIdAndEndpoint(
        idempotencyKey: String,
        memberId: Long,
        endpoint: String
    ): IdempotencyKey?

    /**
     * 만료된 키 삭제 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :now")
    fun deleteExpired(now: LocalDateTime)
}
