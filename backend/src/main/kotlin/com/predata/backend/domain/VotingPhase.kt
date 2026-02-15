package com.predata.backend.domain

/**
 * 투표 및 베팅 단계
 * - 질문의 전체 생명주기를 단계별로 제어
 * - FSM (Finite State Machine) 패턴 적용
 */
enum class VotingPhase {
    VOTING_COMMIT_OPEN,    // 투표 커밋 단계 (해시 제출 가능)
    VOTING_REVEAL_OPEN,    // 투표 공개 단계 (선택 공개 가능)
    BETTING_OPEN,          // 베팅 단계 (베팅 가능)
    SETTLEMENT_PENDING,    // 정산 대기
    SETTLED,               // 정산 완료
    REWARD_DISTRIBUTED     // 리워드 분배 완료
}
