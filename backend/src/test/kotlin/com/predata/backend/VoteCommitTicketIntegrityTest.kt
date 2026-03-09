package com.predata.backend

import com.predata.backend.domain.DailyTicket
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Member
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.exception.AlreadyVotedException
import com.predata.backend.exception.NotFoundException
import org.junit.jupiter.api.assertThrows
import com.predata.backend.repository.DailyTicketRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteCommitRepository
import com.predata.backend.service.TicketService
import com.predata.backend.service.VoteCommitService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * VoteCommitService 티켓 정합성 회귀 테스트
 *
 * 검증 목표:
 * 1. 검증 실패 분기(질문없음, 순차중복)에서 티켓이 차감되지 않는다.
 * 2. 경쟁 조건(동시 중복 커밋)에서 패배 트랜잭션이 롤백되어 티켓이 1개만 차감된다.
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteCommitTicketIntegrityTest {

    @Autowired private lateinit var voteCommitService: VoteCommitService
    @Autowired private lateinit var ticketService: TicketService
    @Autowired private lateinit var memberRepository: MemberRepository
    @Autowired private lateinit var questionRepository: QuestionRepository
    @Autowired private lateinit var dailyTicketRepository: DailyTicketRepository
    @Autowired private lateinit var voteCommitRepository: VoteCommitRepository

    private lateinit var member: Member
    private val createdQuestions = mutableListOf<Question>()

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(
            Member(
                email = "ticket-integrity-${UUID.randomUUID()}@test.com",
                countryCode = "KR",
                hasVotingPass = true
            )
        )
    }

    @AfterEach
    fun tearDown() {
        val memberId = member.id!!
        createdQuestions.mapNotNull { q ->
            q.id?.let { qid -> voteCommitRepository.findByMemberIdAndQuestionId(memberId, qid) }
        }.forEach { voteCommitRepository.delete(it) }
        dailyTicketRepository.findByMemberId(memberId)
            .forEach { dailyTicketRepository.delete(it) }
        createdQuestions.forEach { questionRepository.delete(it) }
        createdQuestions.clear()
        memberRepository.delete(member)
    }

    /**
     * 회귀 1: 질문 없음 → NotFoundException 예외 발생, 티켓 차감 없음
     *
     * 수정 전: ticketService.consumeTicket()이 질문 조회 전에 실행 → 누수
     * 수정 후: 티켓 차감이 모든 검증 통과 후에 실행 → 차감 없음
     */
    @Test
    fun `질문없음_예외발생시_티켓_차감없음`() {
        val memberId = member.id!!
        val initial = 5
        dailyTicketRepository.save(DailyTicket(memberId = memberId, remainingCount = initial, resetDate = LocalDate.now(ZoneOffset.UTC)))

        val nonExistentId = Long.MAX_VALUE
        val hash = computeHash(nonExistentId, memberId, "YES", "salt")

        assertThrows<NotFoundException> {
            voteCommitService.commit(memberId, VoteCommitRequest(nonExistentId, hash))
        }
        assertThat(ticketService.getRemainingTickets(memberId)).isEqualTo(initial)
    }

    /**
     * 회귀 2: 순차 중복 커밋 → 첫 번째만 티켓 차감, 두 번째는 AlreadyVotedException 예외 + 차감 없음
     *
     * 수정 전: 두 번째 커밋이 existsByMemberIdAndQuestionId 이전에 티켓을 차감 → 누수
     * 수정 후: exists 체크 통과 후에만 티켓 차감 → 두 번째 요청은 차감 없음
     */
    @Test
    fun `순차중복커밋_두번째시_티켓_추가차감없음`() {
        val memberId = member.id!!
        val initial = 5
        dailyTicketRepository.save(DailyTicket(memberId = memberId, remainingCount = initial, resetDate = LocalDate.now(ZoneOffset.UTC)))

        val question = createVoteResultQuestion()
        val hash = computeHash(question.id!!, memberId, "YES", "salt-seq-dup")

        val first = voteCommitService.commit(memberId, VoteCommitRequest(question.id!!, hash))
        assertThat(first.success).isTrue()
        assertThat(ticketService.getRemainingTickets(memberId)).isEqualTo(initial - 1)

        assertThrows<AlreadyVotedException> {
            voteCommitService.commit(memberId, VoteCommitRequest(question.id!!, hash))
        }
        assertThat(ticketService.getRemainingTickets(memberId)).isEqualTo(initial - 1)
    }

    /**
     * 경쟁 조건: 동일 질문에 동시 중복 커밋
     *
     * - 10 스레드가 같은 (memberId, questionId)로 동시에 commit() 호출
     * - existsByMemberIdAndQuestionId를 통과한 복수 스레드가 saveAndFlush() 경합
     * - 패배 스레드: DataIntegrityViolationException → AlreadyVotedException → @Transactional 롤백
     * - 결과: VoteCommit 1건, 티켓 1개 차감
     */
    @Test
    fun `경쟁조건_동시중복커밋_1개성공_나머지롤백_티켓1개차감`() {
        val memberId = member.id!!
        val initial = 10
        dailyTicketRepository.save(DailyTicket(memberId = memberId, remainingCount = initial, resetDate = LocalDate.now(ZoneOffset.UTC)))

        val question = createVoteResultQuestion()
        val questionId = question.id!!

        val threadCount = 10
        val successCount = AtomicInteger(0)
        val rejectedCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) { idx ->
            executor.submit {
                try {
                    val hash = computeHash(questionId, memberId, "YES", "salt-$idx")
                    val result = voteCommitService.commit(memberId, VoteCommitRequest(questionId, hash))
                    if (result.success) successCount.incrementAndGet()
                    else rejectedCount.incrementAndGet()
                } catch (e: AlreadyVotedException) {
                    // saveAndFlush → DataIntegrityViolationException → AlreadyVotedException
                    // 이 경로의 트랜잭션은 롤백 확정 (티켓 차감 없음)
                    rejectedCount.incrementAndGet()
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(completed).isTrue()
        assertThat(errorCount.get()).isEqualTo(0)
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(rejectedCount.get()).isEqualTo(threadCount - 1)

        // DB에 VoteCommit이 정확히 1건 존재
        assertThat(voteCommitRepository.existsByMemberIdAndQuestionId(memberId, questionId)).isTrue()

        // 패배 스레드 롤백 + exists 체크 통과 못한 스레드 차감 없음 → 총 1개 차감
        assertThat(ticketService.getRemainingTickets(memberId)).isEqualTo(initial - 1)
    }

    private fun createVoteResultQuestion(): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return questionRepository.save(
            Question(
                title = "티켓정합성 ${UUID.randomUUID()}",
                category = "TEST",
                status = QuestionStatus.VOTING,
                marketType = MarketType.VERIFIABLE,
                voteResultSettlement = true,
                votingPhase = VotingPhase.VOTING_COMMIT_OPEN,
                votingEndAt = now.plusHours(2),
                bettingStartAt = now.plusHours(3),
                bettingEndAt = now.plusHours(5),
                expiredAt = now.plusDays(1)
            )
        ).also { createdQuestions.add(it) }
    }

    private fun computeHash(questionId: Long, memberId: Long, choice: String, salt: String): String {
        val data = "$questionId:$memberId:$choice:$salt"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
