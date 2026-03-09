package com.predata.backend.domain

import com.predata.backend.sports.domain.QuestionPhase
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

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

    @Deprecated("Use marketType instead. Kept for DB column compatibility.")
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

    @Deprecated("AMM_FPMM에서는 market_pools 테이블 사용. 레거시 집계 참조용으로만 유지.")
    @Column(name = "total_bet_pool")
    var totalBetPool: Long = 0,

    @Deprecated("AMM_FPMM에서는 market_pools 테이블 사용. 레거시 집계 참조용으로만 유지.")
    @Column(name = "yes_bet_pool")
    var yesBetPool: Long = 0,

    @Deprecated("AMM_FPMM에서는 market_pools 테이블 사용. 레거시 집계 참조용으로만 유지.")
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

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null,

    @Column(name = "tags_json", columnDefinition = "TEXT")
    var tagsJson: String? = null,

    @Column(name = "source_links_json", columnDefinition = "TEXT")
    var sourceLinksJson: String? = null,

    @Column(name = "boost_enabled", nullable = false)
    var boostEnabled: Boolean = false,

    @Column(name = "dispute_deadline")
    var disputeDeadline: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "voting_phase", length = 30, nullable = false, columnDefinition = "VARCHAR(30)")
    var votingPhase: VotingPhase = VotingPhase.VOTING_COMMIT_OPEN,

    /** VOTE_RESULT 질문의 reveal 마감 시각. OBJECTIVE_RULE 질문은 null. */
    @Column(name = "reveal_window_end_at")
    var revealWindowEndAt: LocalDateTime? = null,

    /** 질문 생성 시 지정한 투표 기간 타입. H6/D1/D3. */
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_window_type", length = 10)
    var voteWindowType: VoteWindowType? = null,

    @Column(name = "expired_at", nullable = false)
    var expiredAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "view_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    var viewCount: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 20)
    var phase: QuestionPhase? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_model", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'AMM_FPMM'")
    var executionModel: ExecutionModel = ExecutionModel.AMM_FPMM,

    /** 크레딧 기반 생성자. 어드민 생성 질문은 null. */
    @Column(name = "creator_member_id")
    var creatorMemberId: Long? = null,

    /** 중복 질문 방지용 정규화 해시. normalize(title):category */
    @Column(name = "question_normalized_hash", length = 255)
    var questionNormalizedHash: String? = null,

    /** 플랫폼 수수료 비율 (고정 20%) */
    @Column(name = "platform_fee_share", precision = 5, scale = 4, nullable = false)
    var platformFeeShare: BigDecimal = BigDecimal("0.2000"),

    /** 질문 생성자 수수료 비율 */
    @Column(name = "creator_fee_share", precision = 5, scale = 4, nullable = false)
    var creatorFeeShare: BigDecimal = BigDecimal("0.4000"),

    /** 투표자 수수료 비율 */
    @Column(name = "voter_fee_share", precision = 5, scale = 4, nullable = false)
    var voterFeeShare: BigDecimal = BigDecimal("0.4000"),

    /** 80% 풀 내부에서 생성자 비율(0~100, 10단위) */
    @Column(name = "creator_split_in_pool", nullable = false)
    var creatorSplitInPool: Int = 50,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    val match: com.predata.backend.sports.domain.Match? = null,
) {
    /** 투표 결과 공개 여부 (DB 컬럼 없음, 도메인 정책 파생) */
    val voteVisibility: VoteVisibility
        get() = when {
            !voteResultSettlement -> VoteVisibility.OPEN
            votingPhase.ordinal >= VotingPhase.VOTING_REVEAL_CLOSED.ordinal -> VoteVisibility.REVEALED
            else -> VoteVisibility.HIDDEN_UNTIL_REVEAL
        }

    /** 정산 모드 (DB 컬럼 없음, voteResultSettlement 플래그 파생) */
    val settlementMode: SettlementMode
        get() = if (voteResultSettlement) SettlementMode.VOTE_RESULT else SettlementMode.OBJECTIVE_RULE
}

enum class QuestionStatus {
    VOTING,     // 투표 진행 중
    BREAK,      // 투표 마감 후 대기 기간
    BETTING,    // 베팅 진행 중
    SETTLED,    // 정산 완료
    CANCELLED   // 블록체인 실패 등으로 취소됨
}

@Deprecated("Use MarketType instead.")
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

enum class ExecutionModel {
    AMM_FPMM,                       // AMM (Fixed Product Market Maker) 실행 모델
    @Deprecated("Orderbook model removed. All markets use AMM_FPMM.")
    ORDERBOOK_LEGACY                // 레거시 오더북 실행 모델 (제거됨)
}
