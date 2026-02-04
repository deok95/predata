package com.predata.backend.repository

import com.predata.backend.domain.MemberBadge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberBadgeRepository : JpaRepository<MemberBadge, Long> {
    fun findByMemberId(memberId: Long): List<MemberBadge>
    fun findByMemberIdAndBadgeId(memberId: Long, badgeId: String): Optional<MemberBadge>
    fun findByMemberIdAndAwardedAtIsNotNull(memberId: Long): List<MemberBadge>
    fun countByMemberIdAndAwardedAtIsNotNull(memberId: Long): Long
}
