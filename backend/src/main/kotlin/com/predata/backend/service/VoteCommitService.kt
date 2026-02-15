package com.predata.backend.service

import com.predata.backend.domain.VoteCommit
import com.predata.backend.domain.VoteCommitStatus
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

        // 4. 투표 기간 확인 (추후 VOTE-003에서 votingPhase로 강화)
        if (question.votingEndAt.isBefore(LocalDateTime.now())) {
            return VoteCommitResponse(
                success = false,
                message = "투표 기간이 종료되었습니다."
            )
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

        // 7. VoteCommit 저장 (salt는 클라이언트가 나중에 reveal 시 제공)
        return try {
            val voteCommit = VoteCommit(
                memberId = memberId,
                questionId = request.questionId,
                commitHash = request.commitHash,
                salt = "",  // Reveal 시 클라이언트가 제공한 salt로 검증 후 저장
                status = VoteCommitStatus.COMMITTED
            )

            val saved = voteCommitRepository.save(voteCommit)
            logger.info("Vote commit successful: memberId=$memberId, questionId=${request.questionId}")

            VoteCommitResponse(
                success = true,
                message = "투표 커밋이 완료되었습니다.",
                voteCommitId = saved.id
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

        // 4. Reveal 기간 확인 (추후 VOTE-003에서 votingPhase로 강화)
        // 현재는 베팅 시작 전까지 reveal 가능
        if (question.bettingStartAt.isBefore(LocalDateTime.now())) {
            return VoteRevealResponse(
                success = false,
                message = "투표 공개 기간이 종료되었습니다."
            )
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

        // 6. Reveal 성공: 선택 공개
        voteCommit.revealedChoice = request.choice
        voteCommit.salt = request.salt
        voteCommit.revealedAt = LocalDateTime.now()
        voteCommit.status = VoteCommitStatus.REVEALED

        voteCommitRepository.save(voteCommit)
        logger.info("Vote reveal successful: memberId=$memberId, questionId=${request.questionId}, choice=${request.choice}")

        return VoteRevealResponse(
            success = true,
            message = "투표 공개가 완료되었습니다."
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
}
