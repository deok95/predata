package com.predata.backend.domain

import com.predata.backend.sports.domain.QuestionPhase
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "questions")
data class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(length = 50)
    var category: String? = null,

    @Column(name = "category_weight", precision = 3, scale = 2)
    val categoryWeight: BigDecimal = BigDecimal("1.00"),

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var status: QuestionStatus = QuestionStatus.VOTING,

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var type: QuestionType = QuestionType.VERIFIABLE,

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", length = 20, nullable = false, columnDefinition = "VARCHAR(20)")
    var marketType: MarketType = MarketType.VERIFIABLE,

    @Column(name = "resolution_rule", nullable = false, updatable = false, columnDefinition = "TEXT")
    val resolutionRule: String = "기본 정산 규칙",

    @Column(name = "resolution_source", length = 500)
    var resolutionSource: String? = null,

    @Column(name = "resolve_at")
    var resolveAt: LocalDateTime? = null,

    @Column(name = "dispute_until")
    var disputeUntil: LocalDateTime? = null,

    @Column(name = "vote_result_settlement", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    var voteResultSettlement: Boolean = false,

    @Column(name = "voting_end_at", nullable = false)
    var votingEndAt: LocalDateTime,

    @Column(name = "betting_start_at", nullable = false)
    var bettingStartAt: LocalDateTime,

    @Column(name = "betting_end_at", nullable = false)
    var bettingEndAt: LocalDateTime,

    @Column(name = "total_bet_pool")
    var totalBetPool: Long = 0,

    @Column(name = "yes_bet_pool")
    var yesBetPool: Long = 0,

    @Column(name = "no_bet_pool")
    var noBetPool: Long = 0,

    @Column(name = "initial_yes_pool", nullable = false, columnDefinition = "BIGINT DEFAULT 500")
    var initialYesPool: Long = 500,

    @Column(name = "initial_no_pool", nullable = false, columnDefinition = "BIGINT DEFAULT 500")
    var initialNoPool: Long = 500,

    @Enumerated(EnumType.STRING)
    @Column(name = "final_result")
    var finalResult: FinalResult = FinalResult.PENDING,

    @Column(name = "source_url", columnDefinition = "TEXT")
    var sourceUrl: String? = null,

    @Column(name = "dispute_deadline")
    var disputeDeadline: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "voting_phase", length = 30, nullable = false, columnDefinition = "VARCHAR(30)")
    var votingPhase: VotingPhase = VotingPhase.VOTING_COMMIT_OPEN,

    @Column(name = "expired_at", nullable = false)
    var expiredAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "view_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    var viewCount: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 20)
    var phase: QuestionPhase? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    val match: com.predata.backend.sports.domain.Match? = null
)

enum class QuestionStatus {
    VOTING,     // 투표 진행 중
    BREAK,      // 투표 마감 후 대기 기간
    BETTING,    // 베팅 진행 중
    SETTLED,    // 정산 완료
    CANCELLED   // 블록체인 실패 등으로 취소됨
}

enum class QuestionType {
    VERIFIABLE,  // 검증 가능한 질문 (스포츠 결과 등)
    OPINION      // 의견 기반 질문
}

enum class MarketType {
    VERIFIABLE,  // 검증 가능한 시장 (스포츠, 주가 등 외부 데이터 기반)
    OPINION      // 의견 기반 시장 (투표 결과 기반)
}

enum class FinalResult {
    YES, NO, PENDING
}
