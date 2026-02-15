package com.predata.backend.service

import com.predata.backend.domain.Choice
import com.predata.backend.domain.VoteCommit
import com.predata.backend.domain.VoteCommitStatus
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteCommitResponse
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.VoteRevealResponse
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
    private val votingConfig: com.predata.backend.config.VotingConfig
) {
    private val logger = LoggerFactory.getLogger(VoteCommitService::class.java)

    /**
     * 1단계: Commit (해시 저장)
     * - commitHash = SHA-256(choice + salt) 저장
     * - UNIQUE 제약으로 중복 투표 방지
     */
    @Transactional
    fun commit(memberId: Long, request: VoteCommitRequest): VoteCommitResponse {
        // 1. 회원 존재 확인
        val member = memberRepository.findById(memberId).orElse(null)
            ?: return VoteCommitResponse(
                success = false,
                message = "회원을 찾을 수 없습니다."
            )

        // 2. 밴 체크
        if (member.isBanned) {
            return VoteCommitResponse(
                success = false,
                message = "계정이 정지되었습니다. 사유: ${member.banReason ?: "이용약관 위반"}"
            )
        }

        // 3. 질문 존재 확인
        val question = questionRepository.findById(request.questionId).orElse(null)
            ?: return VoteCommitResponse(
                success = false,
                message = "질문을 찾을 수 없습니다."
            )

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

        // 7. 서버 salt 생성
        val serverSalt = generateSalt()

        // 8. VoteCommit 저장
        return try {
            val voteCommit = VoteCommit(
                memberId = memberId,
                questionId = request.questionId,
                commitHash = request.commitHash,
                salt = serverSalt,
                status = VoteCommitStatus.COMMITTED
            )

            val saved = voteCommitRepository.save(voteCommit)
            logger.info("Vote commit successful: memberId=$memberId, questionId=${request.questionId}")

            VoteCommitResponse(
                success = true,
                message = "투표 커밋이 완료되었습니다.",
                voteCommitId = saved.id,
                salt = serverSalt  // 클라이언트가 reveal 시 사용
            )
        } catch (e: DataIntegrityViolationException) {
            logger.warn("Duplicate vote commit attempt: memberId=$memberId, questionId=${request.questionId}")
            VoteCommitResponse(
                success = false,
                message = "이미 투표하셨습니다."
            )
        }
    }

    /**
     * 2단계: Reveal (선택 공개 및 검증)
     * - SHA-256(choice + salt) == commitHash 검증
     * - 검증 성공 시 revealedChoice 저장
     */
    @Transactional
    fun reveal(memberId: Long, request: VoteRevealRequest): VoteRevealResponse {
        // 1. VoteCommit 조회
        val voteCommit = voteCommitRepository.findByMemberIdAndQuestionId(memberId, request.questionId)
            ?: return VoteRevealResponse(
                success = false,
                message = "커밋된 투표를 찾을 수 없습니다."
            )

        // 2. 이미 공개했는지 확인
        if (voteCommit.status == VoteCommitStatus.REVEALED) {
            return VoteRevealResponse(
                success = false,
                message = "이미 투표를 공개하셨습니다."
            )
        }

        // 3. 질문 존재 확인
        val question = questionRepository.findById(request.questionId).orElse(null)
            ?: return VoteRevealResponse(
                success = false,
                message = "질문을 찾을 수 없습니다."
            )

        // 4. 투표 단계 확인 (VOTING_REVEAL_OPEN만 가능)
        if (question.votingPhase != VotingPhase.VOTING_REVEAL_OPEN) {
            throw IllegalStateException("현재 투표 공개 기간이 아닙니다.")
        }

        // 5. commitHash 검증: SHA-256(choice + salt) == commitHash
        val computedHash = hashChoiceWithSalt(request.choice.name, request.salt)
        if (computedHash != voteCommit.commitHash) {
            logger.warn("Vote reveal hash mismatch: memberId=$memberId, questionId=${request.questionId}")
            return VoteRevealResponse(
                success = false,
                message = "투표 검증에 실패했습니다. (해시 불일치)"
            )
        }

        // 6. Reveal 성공: 선택 공개 (UTC 시각)
        voteCommit.revealedChoice = request.choice
        voteCommit.salt = request.salt
        voteCommit.revealedAt = LocalDateTime.now(ZoneOffset.UTC)
        voteCommit.status = VoteCommitStatus.REVEALED

        voteCommitRepository.save(voteCommit)
        logger.info("Vote reveal successful: memberId=$memberId, questionId=${request.questionId}, choice=${request.choice}")

        return VoteRevealResponse(
            success = true,
            message = "투표 공개가 완료되었습니다."
        )
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
     * SHA-256 해싱 (VoteRecordService 패턴 참고)
     */
    private fun hashChoiceWithSalt(choice: String, salt: String): String {
        val data = choice + salt
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * 서버 salt 생성 (SecureRandom 사용)
     */
    private fun generateSalt(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
