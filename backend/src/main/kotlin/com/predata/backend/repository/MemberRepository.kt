package com.predata.backend.repository

import com.predata.backend.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByGoogleId(googleId: String): Optional<Member>
    fun findByWalletAddress(walletAddress: String): Optional<Member>
    fun findByReferralCode(referralCode: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
    fun existsByGoogleId(googleId: String): Boolean
    fun findByIsBannedTrue(): List<Member>
    fun findBySignupIp(ip: String): List<Member>
    fun findByLastIp(ip: String): List<Member>

    // === 성능 최적화용 배치 조회 메서드 ===

    // ID 목록으로 회원 일괄 조회 (N+1 문제 해결)
    fun findAllByIdIn(ids: Collection<Long>): List<Member>

    // 국가별 회원 ID 조회 (findAll() 대체)
    @Query("SELECT m.id FROM Member m WHERE m.countryCode = :countryCode")
    fun findIdsByCountryCode(countryCode: String): List<Long>

    // 직업별 회원 ID 조회
    @Query("SELECT m.id FROM Member m WHERE m.jobCategory = :jobCategory")
    fun findIdsByJobCategory(jobCategory: String): List<Long>

    // 연령대별 회원 ID 조회
    @Query("SELECT m.id FROM Member m WHERE m.ageGroup >= :minAge AND m.ageGroup < :maxAge")
    fun findIdsByAgeGroupBetween(minAge: Int, maxAge: Int): List<Long>

    // 국가별 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.countryCode = :countryCode")
    fun countByCountryCode(countryCode: String): Int

    // 직업별 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.jobCategory = :jobCategory")
    fun countByJobCategory(jobCategory: String): Int

    // 연령대별 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.ageGroup >= :minAge AND m.ageGroup < :maxAge")
    fun countByAgeGroupBetween(minAge: Int, maxAge: Int): Int
}
