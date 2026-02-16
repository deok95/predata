package com.predata.backend.service

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.SportsMatch
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.SportsMatchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class QuestionAutoGenerationService(
    private val sportsApiService: SportsApiService,
    private val sportsMatchRepository: SportsMatchRepository,
    private val questionRepository: QuestionRepository,
    private val bettingSuspensionService: BettingSuspensionService,
    private val settlementService: SettlementService
) {

    /**
     * 스포츠 경기 자동 질문 생성
     */
    @Transactional
    fun generateSportsQuestions(): GenerationResult {
        println("[AutoGen] 스포츠 경기 자동 질문 생성 시작")
        
        // 1. API에서 다가오는 경기 가져오기
        val upcomingMatches = sportsApiService.fetchUpcomingMatches()
        println("[AutoGen] 가져온 경기 수: ${upcomingMatches.size}")
        
        var createdCount = 0
        var skippedCount = 0
        
        for (match in upcomingMatches) {
            try {
                // 2. 이미 저장된 경기인지 확인
                val existingMatch = sportsMatchRepository.findByExternalApiId(match.externalApiId)
                if (existingMatch != null) {
                    println("[AutoGen] 이미 존재하는 경기: ${match.homeTeam} vs ${match.awayTeam}")
                    skippedCount++
                    continue
                }
                
                // 3. 질문 생성
                val question = createQuestionFromMatch(match)
                val savedQuestion = questionRepository.save(question)
                
                // 4. 경기 정보 저장 (질문 ID 연결)
                val matchToSave = match.copy(questionId = savedQuestion.id)
                sportsMatchRepository.save(matchToSave)
                
                println("[AutoGen] ✅ 생성됨: ${match.homeTeam} vs ${match.awayTeam} (Question ID: ${savedQuestion.id})")
                createdCount++
                
            } catch (e: Exception) {
                println("[AutoGen] ❌ 오류: ${match.homeTeam} vs ${match.awayTeam} - ${e.message}")
                skippedCount++
            }
        }
        
        println("[AutoGen] 완료 - 생성: $createdCount, 스킵: $skippedCount")
        return GenerationResult(createdCount, skippedCount)
    }

    /**
     * 경기 정보로 질문 생성 (실시간 베팅 지원)
     */
    private fun createQuestionFromMatch(match: SportsMatch): Question {
        // 질문 제목 생성 (리그명 포함)
        val title = when (match.sportType) {
            "FOOTBALL" -> "[${match.leagueName}] Will ${match.homeTeam} beat ${match.awayTeam}?"
            "BASKETBALL" -> "[${match.leagueName}] Will ${match.homeTeam} win against ${match.awayTeam}?"
            "BASEBALL" -> "[${match.leagueName}] Will ${match.homeTeam} defeat ${match.awayTeam}?"
            else -> "[${match.leagueName}] ${match.homeTeam} vs ${match.awayTeam} - Who will win?"
        }
        
        // 마감 시간: 경기 종료 시간 (실시간 베팅 허용)
        // EPL 경기는 보통 90분 + 추가시간 = 약 2시간
        val expiredAt = match.matchDate.plusHours(2)
        val votingEndAt = match.matchDate.minusHours(1)
        val bettingStartAt = votingEndAt.plusMinutes(5)
        val bettingEndAt = expiredAt

        return Question(
            title = title,
            category = "SPORTS",
            categoryWeight = BigDecimal("1.00"),
            status = QuestionStatus.VOTING,
            totalBetPool = 0,
            yesBetPool = 0,
            noBetPool = 0,
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            finalResult = FinalResult.PENDING,
            expiredAt = expiredAt,
            createdAt = LocalDateTime.now()
        )
    }

    /**
     * 완료된 경기 결과 업데이트 (LIVE 상태 포함)
     */
    @Transactional
    fun updateFinishedMatches(): UpdateResult {
        println("[AutoGen] 완료된 경기 결과 업데이트 시작")
        
        // 1. SCHEDULED 또는 LIVE 상태의 경기 중 경기 시작 시간이 지난 것들
        val now = LocalDateTime.now()
        val scheduledMatches = sportsMatchRepository.findByStatusAndMatchDateBefore("SCHEDULED", now)
        val liveMatches = sportsMatchRepository.findByStatus("LIVE")
        val matchesToCheck = scheduledMatches + liveMatches
        
        var updatedCount = 0
        
        for (match in matchesToCheck) {
            try {
                // 2. 이전 스코어 저장 (스코어 변경 감지용)
                val oldHomeScore = match.homeScore
                val oldAwayScore = match.awayScore
                
                // 3. API에서 경기 결과 가져오기
                val result = sportsApiService.fetchMatchResult(match.externalApiId)
                
                if (result != null) {
                    // 4. 경기 결과 업데이트
                    match.homeScore = result.homeScore
                    match.awayScore = result.awayScore
                    match.result = result.result
                    match.status = result.status
                    sportsMatchRepository.save(match)
                    
                    // 5. ⚡ 스코어 변경 감지 & 베팅 일시 중지
                    bettingSuspensionService.suspendBettingOnScoreChange(match, oldHomeScore, oldAwayScore)
                    
                    if (result.status == "FINISHED") {
                        println("[AutoGen] ✅ 경기 종료: ${match.homeTeam} ${result.homeScore} - ${result.awayScore} ${match.awayTeam}")
                    } else if (result.status == "LIVE") {
                        println("[AutoGen] 🔴 LIVE: ${match.homeTeam} ${result.homeScore} - ${result.awayScore} ${match.awayTeam}")
                    }
                    updatedCount++
                }
                
                // API 호출 제한 방지 (다중 리그 동시 라이브 경기 대응)
                Thread.sleep(400)
                
            } catch (e: Exception) {
                println("[AutoGen] ❌ 결과 업데이트 실패: ${match.homeTeam} vs ${match.awayTeam} - ${e.message}")
            }
        }
        
        println("[AutoGen] 결과 업데이트 완료: $updatedCount 건")
        return UpdateResult(updatedCount)
    }

    /**
     * 완료된 경기의 질문 자동 정산
     */
    fun autoSettleSportsQuestions(): SportsSettlementResult {
        println("[AutoGen] 스포츠 질문 자동 정산 시작")
        
        // 1. 완료된 경기 중 정산되지 않은 질문 찾기
        val finishedMatches = sportsMatchRepository.findByStatus("FINISHED")
        
        var settledCount = 0
        
        for (match in finishedMatches) {
            if (match.questionId == null) continue
            
            try {
                val question = questionRepository.findById(match.questionId!!).orElse(null)
                if (question == null || question.status == QuestionStatus.SETTLED) {
                    continue
                }

                // 2. 경기 결과에 따라 질문 정산
                val finalResult = when (match.result) {
                    "HOME_WIN" -> FinalResult.YES  // YES = 홈팀 승리
                    "AWAY_WIN" -> FinalResult.NO   // NO = 원정팀 승리
                    "DRAW" -> FinalResult.NO       // 무승부는 NO로 처리 (홈팀이 이기지 못함)
                    else -> continue
                }

                val sourceUrl = "https://api.football-data.org/v4/matches/${match.externalApiId}"

                // 정산 단일 경로 사용: 지급/원장 settled 마킹/감사 로그/온체인 호출을 모두 보장
                settlementService.initiateSettlement(
                    questionId = question.id!!,
                    finalResult = finalResult,
                    sourceUrl = sourceUrl
                )
                settlementService.finalizeSettlement(
                    questionId = question.id!!,
                    skipDeadlineCheck = true
                )
                
                println("[AutoGen] ✅ 정산 완료: ${question.title} -> $finalResult")
                settledCount++
                
            } catch (e: Exception) {
                println("[AutoGen] ❌ 정산 실패: Question ID ${match.questionId} - ${e.message}")
            }
        }
        
        println("[AutoGen] 자동 정산 완료: $settledCount 건")
        return SportsSettlementResult(settledCount)
    }
}

data class GenerationResult(
    val created: Int,
    val skipped: Int
)

data class UpdateResult(
    val updated: Int
)

data class SportsSettlementResult(
    val settled: Int
)
