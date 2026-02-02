package com.predata.backend.repository

import com.predata.backend.domain.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface EmailVerificationRepository : JpaRepository<EmailVerification, Long> {
    fun findTopByEmailOrderByCreatedAtDesc(email: String): Optional<EmailVerification>
}
