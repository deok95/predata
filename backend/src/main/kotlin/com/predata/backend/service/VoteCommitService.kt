package com.predata.backend.service

import com.predata.backend.domain.Choice
import com.predata.backend.domain.VoteCommit
import com.predata.backend.domain.VoteCommitStatus
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteCommitResponse
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.VoteRevealResponse
import com.predata.backend.exception.ServiceUnavailableException
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteCommitRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Commit-Reveal 투표 서비스
 * - commit(): 투표 해시 저장
 * - reveal(): 선택 공개 및 검증
 */
@Service
class VoteCommitService(
    private val voteCommitRepository: VoteCommitRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val votingConfig: com.predata.backend.config.VotingConfig,
    private val auditService: AuditService,
    private val pauseService: PauseService,
    private val circuitBreaker: VotingCircuitBreaker,
    private val ticketService: TicketService
) {
    private val logger = LoggerFactory.getLogger(VoteCommitService::class.java)

    /**
     * 1단계: Commit (해시 저장)
     * - commitHash = SHA-256(choice + salt) 저장
     * - UNIQUE 제약으로 중복 투표 방지
     */
    @Transactional
    fun commit(memberId: Long, request: VoteCommitRequest): VoteCommitResponse {
        // 0. 투표 중지 상태 체크
        if (pauseService.isPaused(request.questionId) || circuitBreaker.isOpen()) {
            throw ServiceUnavailableException("시스템 점검 중입니다.")
        }

        // 1. 회원 존재 확인
        val member = memberRepository.findById(memberId).orElse(null)
            ?: run {
                return VoteCommitResponse(
                    success = false,
                    message = "회원을 찾을 수 없습니다."
                )
            }

        // 2. 밴 체크
        if (member.isBanned) {
            return VoteCommitResponse(
                success = false,
                message = "계정이 정지되었습니다. 사유: ${member.banReason ?: "이용약관 위반"}"
            )
        }

        // 2-1. 투표 패스 확인 (미구매 계정은 투표 불가)
        if (!member.hasVotingPass) {
            return VoteCommitResponse(
                success = false,
                message = "투표 패스가 필요합니다. 마이페이지에서 구매해주세요."
            )
        }

        // 2-2. 티켓 차감 (5개 제한)
        val ticketConsumed = ticketService.consumeTicket(memberId)
        if (!ticketConsumed) {
            throw IllegalStateException("오늘 투표 가능 횟수를 모두 사용했습니다")
        }

        // 3. 질문 존재 확인
        val question = questionRepository.findById(request.questionId).orElse(null)
            ?: run {
                return VoteCommitResponse(
                    success = false,
                    message = "질문을 찾을 수 없습니다."
                )
            }

        // 4. 투표 단계 확인 (VOTING_COMMIT_OPEN만 가능)
        if (question.votingPhase != VotingPhase.VOTING_COMMIT_OPEN) {
            throw IllegalStateException("현재 투표 접수 기간이 아닙니다.")
        }

        // 5. 중복 투표 체크 (UNIQUE 제약으로 자동 방지)
        if (voteCommitRepository.existsByMemberIdAndQuestionId(memberId, request.questionId)) {
            return VoteCommitResponse(
                success = false,
                message = "이미 투표하셨습니다."
            )
        }

        // 6. 일일 투표 한도 체크 (UTC 기준)
        val nowUTC = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
        val startOfDayUTC = nowUTC.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toLocalDateTime()
        val endOfDayUTC = startOfDayUTC.plusDays(1)

        val todayCount = voteCommitRepository.countByMemberIdAndCommittedAtBetween(
            memberId, startOfDayUTC, endOfDayUTC
        )

        if (todayCount >= votingConfig.dailyLimit) {
            logger.warn("Daily vote limit exceeded: memberId=$memberId, count=$todayCount, limit=${votingConfig.dailyLimit}")
            return VoteCommitResponse(
                success = false,
                message = "일일 투표 한도(${votingConfig.dailyLimit}개)를 초과했습니다."
            )
        }

        // 7. VoteCommit 저장 (salt는 클라이언트가 보관)
        return try {
            val voteCommit = VoteCommit(
                memberId = memberId,
                questionId = request.questionId,
                commitHash = request.commitHash,
                status = VoteCommitStatus.COMMITTED
            )

            val saved = voteCommitRepository.save(voteCommit)
            logger.info("Vote commit successful: memberId=$memberId, questionId=${request.questionId}")

            // 감사 로그 기록
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.VOTE_COMMIT,
                entityType = "VoteCommit",
                entityId = saved.id,
                detail = "투표 커밋: questionId=${request.questionId}"
            )

            // 서킷브레이커 성공 기록
            circuitBreaker.recordSuccess()

            // 남은 티켓 조회
            val remainingTickets = ticketService.getRemainingTickets(memberId)

            VoteCommitResponse(
                success = true,
                message = "투표 커밋이 완료되었습니다.",
                voteCommitId = saved.id,
                remainingTickets = remainingTickets
            )
        } catch (e: DataIntegrityViolationException) {
            logger.warn("Duplicate vote commit attempt: memberId=$memberId, questionId=${request.questionId}")
            VoteCommitResponse(
                success = false,
                message = "이미 투표하셨습니다."
            )
        } catch (e: Exception) {
            circuitBreaker.recordFailure()
            throw e
        }
    }

    /**
     * 2단계: Reveal (선택 공개 및 검증)
     * - SHA-256(choice + salt) == commitHash 검증
     * - 검증 성공 시 revealedChoice 저장
     */
    @Transactional
    fun reveal(memberId: Long, request: VoteRevealRequest): VoteRevealResponse {
        // 0. 투표 중지 상태 체크
        if (pauseService.isPaused(request.questionId) || circuitBreaker.isOpen()) {
            throw ServiceUnavailableException("시스템 점검 중입니다.")
        }

        // 0-1. 투표 패스 확인 (미구매 계정은 투표 불가)
        val member = memberRepository.findById(memberId).orElse(null)
            ?: run {
                return VoteRevealResponse(
                    success = false,
                    message = "회원을 찾을 수 없습니다."
                )
            }
        if (!member.hasVotingPass) {
            return VoteRevealResponse(
                success = false,
                message = "투표 패스가 필요합니다. 마이페이지에서 구매해주세요."
            )
        }

        // 1. VoteCommit 조회
        val voteCommit = voteCommitRepository.findByMemberIdAndQuestionId(memberId, request.questionId)
            ?: run {
                return VoteRevealResponse(
                    success = false,
                    message = "커밋된 투표를 찾을 수 없습니다."
                )
            }

        // 2. 이미 공개했는지 확인
        if (voteCommit.status == VoteCommitStatus.REVEALED) {
            return VoteRevealResponse(
                success = false,
                message = "이미 투표를 공개하셨습니다."
            )
        }

        // 3. 질문 존재 확인
        val question = questionRepository.findById(request.questionId).orElse(null)
            ?: run {
                return VoteRevealResponse(
                    success = false,
                    message = "질문을 찾을 수 없습니다."
                )
            }

        // 4. 투표 단계 확인 (VOTING_REVEAL_OPEN만 가능)
        if (question.votingPhase != VotingPhase.VOTING_REVEAL_OPEN) {
            throw IllegalStateException("현재 투표 공개 기간이 아닙니다.")
        }

        // 5. commitHash 검증: SHA-256(questionId:memberId:choice:salt) == commitHash
        val computedHash = hashChoiceWithSalt(request.questionId, memberId, request.choice.name, request.salt)
        if (computedHash != voteCommit.commitHash) {
            logger.warn("Vote reveal hash mismatch: memberId=$memberId, questionId=${request.questionId}")
            return VoteRevealResponse(
                success = false,
                message = "투표 검증에 실패했습니다. (해시 불일치)"
            )
        }

        return try {
            // 6. Reveal 성공: 선택 공개 (UTC 시각)
            voteCommit.revealedChoice = request.choice
            voteCommit.revealedAt = LocalDateTime.now(ZoneOffset.UTC)
            voteCommit.status = VoteCommitStatus.REVEALED

            voteCommitRepository.save(voteCommit)
            logger.info("Vote reveal successful: memberId=$memberId, questionId=${request.questionId}, status=${voteCommit.status}")

            // 감사 로그 기록
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.VOTE_REVEAL,
                entityType = "VoteCommit",
                entityId = voteCommit.id,
                detail = "투표 공개: questionId=${request.questionId}"
            )

            // 서킷브레이커 성공 기록
            circuitBreaker.recordSuccess()

            VoteRevealResponse(
                success = true,
                message = "투표 공개가 완료되었습니다."
            )
        } catch (e: Exception) {
            circuitBreaker.recordFailure()
            throw e
        }
    }

    /**
     * 투표 결과 조회 (Reveal 종료 후에만 공개)
     */
    fun getResults(questionId: Long): Map<String, Any> {
        val question = questionRepository.findById(questionId).orElse(null)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        // Reveal 종료 전에는 공개 금지
        if (question.votingPhase.ordinal < VotingPhase.BETTING_OPEN.ordinal) {
            throw IllegalStateException("투표 결과는 공개 전입니다.")
        }

        val yesCount = voteCommitRepository.countByQuestionIdAndRevealedChoice(questionId, Choice.YES)
        val noCount = voteCommitRepository.countByQuestionIdAndRevealedChoice(questionId, Choice.NO)

        return mapOf(
            "success" to true,
            "message" to "투표 결과 조회 성공",
            "yesCount" to yesCount,
            "noCount" to noCount,
            "totalCount" to (yesCount + noCount)
        )
    }

    /**
     * 투표 상태 조회 (phase, 참여자 수만 공개)
     */
    fun getStatus(questionId: Long): Map<String, Any> {
        val question = questionRepository.findById(questionId).orElse(null)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        val totalParticipants = voteCommitRepository.countByQuestionIdAndStatus(
            questionId, VoteCommitStatus.REVEALED
        )

        return mapOf(
            "success" to true,
            "phase" to question.votingPhase,
            "totalParticipants" to totalParticipants,
            "canCommit" to (question.votingPhase == VotingPhase.VOTING_COMMIT_OPEN),
            "canReveal" to (question.votingPhase == VotingPhase.VOTING_REVEAL_OPEN)
        )
    }

    /**
     * SHA-256 해싱
     * 형식: SHA-256(questionId:memberId:choice:salt)
     * 클라이언트와 동일한 형식 사용
     */
    private fun hashChoiceWithSalt(questionId: Long, memberId: Long, choice: String, salt: String): String {
        val data = "$questionId:$memberId:$choice:$salt"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
