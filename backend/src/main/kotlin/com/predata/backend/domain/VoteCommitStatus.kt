package com.predata.backend.domain

/**
 * 투표 커밋 상태
 * - COMMITTED: 커밋만 완료 (해시 저장됨, 선택지 미공개)
 * - REVEALED: 공개 완료 (선택지 공개됨)
 * - EXPIRED: 기한 만료 (reveal 하지 않음)
 */
enum class VoteCommitStatus {
    COMMITTED,  // 커밋만 완료
    REVEALED,   // 공개 완료
    EXPIRED     // 기한 만료
}
