package com.predata.backend.domain.policy

import com.predata.backend.domain.DraftStatus
import java.time.LocalDateTime

enum class DraftAccessResult {
    OK,
    NOT_FOUND,
    ALREADY_CONSUMED,
    EXPIRED,
}

enum class DraftSubmitStateResult {
    OK,
    ALREADY_CONSUMED,
    EXPIRED_OR_INVALID,
    EXPIRED_NEEDS_CLOSE,
}

object DraftLifecyclePolicy {
    fun evaluateAccess(
        ownerMatches: Boolean,
        status: DraftStatus,
        expiresAt: LocalDateTime,
        now: LocalDateTime,
    ): DraftAccessResult {
        if (!ownerMatches) return DraftAccessResult.NOT_FOUND
        if (status != DraftStatus.OPEN) return DraftAccessResult.ALREADY_CONSUMED
        if (expiresAt.isBefore(now)) return DraftAccessResult.EXPIRED
        return DraftAccessResult.OK
    }

    fun ownershipAndKeyValid(
        ownerMatches: Boolean,
        keyMatches: Boolean,
    ): Boolean = ownerMatches && keyMatches

    fun evaluateSubmitState(
        status: DraftStatus,
        expiresAt: LocalDateTime,
        now: LocalDateTime,
    ): DraftSubmitStateResult {
        if (status == DraftStatus.CONSUMED) return DraftSubmitStateResult.ALREADY_CONSUMED
        if (status != DraftStatus.OPEN) return DraftSubmitStateResult.EXPIRED_OR_INVALID
        if (expiresAt.isBefore(now)) return DraftSubmitStateResult.EXPIRED_NEEDS_CLOSE
        return DraftSubmitStateResult.OK
    }
}
