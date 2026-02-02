package com.predata.sports.repository

import com.predata.sports.domain.SportsMatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SportsMatchRepository : JpaRepository<SportsMatch, Long> {
    
    // 외부 API ID로 경기 찾기 (중복 방지)
    fun findByExternalApiId(externalApiId: String): SportsMatch?
    
    // 상태별 경기 조회
    fun findByStatus(status: String): List<SportsMatch>
    
    // 특정 날짜 이후의 예정된 경기
    fun findByStatusAndMatchDateAfter(status: String, date: LocalDateTime): List<SportsMatch>
    
    // 특정 날짜 이전의 완료된 경기 (정산 대상)
    fun findByStatusAndMatchDateBefore(status: String, date: LocalDateTime): List<SportsMatch>
    
    // 질문 ID로 경기 찾기
    fun findByQuestionId(questionId: Long): SportsMatch?
    
    // 리그별 경기 조회
    fun findByLeagueName(leagueName: String): List<SportsMatch>
}
