package com.predata.backend.domain.policy

import com.predata.backend.domain.market.BatchStatus

object BatchLifecyclePolicy {
    private val TERMINAL_STATUSES = setOf(
        BatchStatus.COMPLETED,
        BatchStatus.PARTIAL_FAILED,
        BatchStatus.FAILED,
    )

    fun isTerminal(status: BatchStatus): Boolean = status in TERMINAL_STATUSES

    fun shouldSkipRun(existingStatus: BatchStatus?): Boolean =
        existingStatus != null && isTerminal(existingStatus)

    fun finalStatus(opened: Int, failed: Int): BatchStatus = when {
        failed == 0 -> BatchStatus.COMPLETED
        opened == 0 && failed > 0 -> BatchStatus.FAILED
        else -> BatchStatus.PARTIAL_FAILED
    }

    fun hasSelectedTop3(selectedCount: Int): Boolean = selectedCount > 0

    fun selectionFailureSummary(cause: String?): String {
        val detail = cause?.take(300) ?: "unknown"
        return "BATCH_SELECTION_FAILED: $detail"
    }

    fun openResultSummary(opened: Int, failed: Int): String? {
        if (failed == 0) return null
        return if (opened == 0) {
            "BATCH_OPEN_FAILED: opened=0, failed=$failed"
        } else {
            "BATCH_PARTIAL_FAILED: opened=$opened, failed=$failed"
        }
    }
}
