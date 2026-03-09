package com.predata.backend.service.market

import com.predata.backend.domain.market.BatchStatus
import com.predata.backend.domain.market.MarketOpenBatch
import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.domain.market.SelectionStatus
import com.predata.backend.domain.policy.BatchLifecyclePolicy
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.ErrorCode
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 마켓 배치 오케스트레이션 서비스
 *
 * 워크플로우:
 *  1. 배치 생성 (PENDING) — cutoff_slot_utc UNIQUE → 중복 실행 방지
 *  2. Top3 선별 (SELECTED)
 *  3. 일괄 오픈 (OPENING → 결과 집계)
 *  4. 상태 최종화 (COMPLETED | PARTIAL_FAILED | FAILED)
 */
@Service
class MarketBatchService(
    private val batchRepository: MarketOpenBatchRepository,
    private val candidateRepository: QuestionMarketCandidateRepository,
    private val selectionService: CategoryTop3SelectionService,
    private val openService: MarketOpenService,
) {
    private val logger = LoggerFactory.getLogger(MarketBatchService::class.java)

    /**
     * 배치 실행 (멱등성 보장).
     * 동일 cutoffSlot 배치가 이미 완료된 경우 스킵.
     */
    fun runBatch(cutoffSlot: LocalDateTime) {
        val existing = batchRepository.findByCutoffSlotUtc(cutoffSlot)
        if (BatchLifecyclePolicy.shouldSkipRun(existing?.status)) {
            logger.info("[MarketBatch] cutoffSlot=$cutoffSlot 이미 완료 (status=${existing?.status}). 스킵.")
            return
        }

        val batch = existing ?: createBatch(cutoffSlot) ?: return

        runPipeline(batch, cutoffSlot)
    }

    /**
     * 수동 배치 실행 (운영자 API).
     * 완료된 배치도 재실행 가능하도록 강제 실행.
     */
    @Transactional
    fun forceRun(cutoffSlot: LocalDateTime): MarketOpenBatch {
        val existing = batchRepository.findByCutoffSlotUtc(cutoffSlot)
        val batch = if (existing != null) {
            // 기존 후보 삭제 → selectAndSave 재실행 시 uk_qmc_batch_question 충돌 방지
            candidateRepository.deleteByBatchId(existing.id!!)
            // 배치 상태 초기화
            existing.status = BatchStatus.PENDING
            existing.totalCandidates = 0
            existing.selectedCount = 0
            existing.openedCount = 0
            existing.failedCount = 0
            existing.errorSummary = null
            batchRepository.save(existing)
        } else {
            batchRepository.save(
                MarketOpenBatch(
                    cutoffSlotUtc = cutoffSlot,
                    startedAt = LocalDateTime.now(ZoneOffset.UTC),
                )
            )
        }

        runPipeline(batch, cutoffSlot)
        return batchRepository.findById(batch.id!!).orElseThrow()
    }

    /**
     * OPEN_FAILED 후보만 재시도 (운영자 API).
     */
    @Transactional
    fun retryOpen(batchId: Long): MarketOpenBatch {
        val batch = batchRepository.findById(batchId).orElseThrow {
            NotFoundException(
                message = ErrorCode.BATCH_NOT_FOUND.message,
                code = ErrorCode.BATCH_NOT_FOUND.name,
            )
        }

        val failedCount = candidateRepository.countByBatchIdAndOpenStatus(batchId, OpenStatus.OPEN_FAILED)
        if (failedCount == 0) {
            throw ConflictException(
                message = ErrorCode.BATCH_NO_FAILED_CANDIDATES.message,
                code = ErrorCode.BATCH_NO_FAILED_CANDIDATES.name,
            )
        }

        openService.retryFailed(batchId)

        // 재시도 후 DB 실제 집계로 정확한 상태 계산
        val totalOpened = candidateRepository.countByBatchIdAndOpenStatus(batchId, OpenStatus.OPENED)
        val totalFailed = candidateRepository.countByBatchIdAndOpenStatus(batchId, OpenStatus.OPEN_FAILED)

        batch.openedCount = totalOpened
        batch.failedCount = totalFailed
        batch.status = BatchLifecyclePolicy.finalStatus(totalOpened.toInt(), totalFailed.toInt())
        batch.errorSummary = BatchLifecyclePolicy.openResultSummary(totalOpened.toInt(), totalFailed.toInt())
        batch.finishedAt = LocalDateTime.now(ZoneOffset.UTC)

        return batchRepository.save(batch)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // private
    // ─────────────────────────────────────────────────────────────────────────

    private fun createBatch(cutoffSlot: LocalDateTime): MarketOpenBatch? {
        return try {
            batchRepository.save(
                MarketOpenBatch(
                    cutoffSlotUtc = cutoffSlot,
                    startedAt = LocalDateTime.now(ZoneOffset.UTC),
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시 실행 경합 — 이미 생성됨
            logger.warn("[MarketBatch] cutoffSlot=$cutoffSlot 배치 이미 존재 (경합). 스킵.")
            null
        }
    }

    private fun runPipeline(batch: MarketOpenBatch, cutoffSlot: LocalDateTime) {
        logger.info("[MarketBatch#${batch.id}] 파이프라인 시작. cutoffSlot=$cutoffSlot")

        // Step 1: 선별
        val allCandidates = try {
            val saved = selectionService.selectAndSave(batch, cutoffSlot)
            batch.totalCandidates = saved.size
            batch.selectedCount = saved.count { it.selectionStatus == SelectionStatus.SELECTED_TOP3 }
            batch.status = BatchStatus.SELECTED
            batch.errorSummary = null
            batchRepository.save(batch)
            saved
        } catch (e: Exception) {
            logger.error("[MarketBatch#${batch.id}] 선별 실패: ${e.message}", e)
            batch.status = BatchStatus.FAILED
            batch.errorSummary = BatchLifecyclePolicy.selectionFailureSummary(e.message)
            batch.finishedAt = LocalDateTime.now(ZoneOffset.UTC)
            batchRepository.save(batch)
            return
        }

        if (!BatchLifecyclePolicy.hasSelectedTop3(batch.selectedCount)) {
            logger.info("[MarketBatch#${batch.id}] 선별된 Top3 없음. 배치 완료.")
            batch.status = BatchStatus.COMPLETED
            batch.errorSummary = null
            batch.finishedAt = LocalDateTime.now(ZoneOffset.UTC)
            batchRepository.save(batch)
            return
        }

        // Step 2: 오픈
        batch.status = BatchStatus.OPENING
        batchRepository.save(batch)

        val (opened, failed) = openService.openAll(allCandidates)

        // Step 3: 최종 상태 집계
        batch.openedCount = opened
        batch.failedCount = failed
        batch.status = BatchLifecyclePolicy.finalStatus(opened, failed)
        batch.errorSummary = BatchLifecyclePolicy.openResultSummary(opened, failed)
        batch.finishedAt = LocalDateTime.now(ZoneOffset.UTC)
        batchRepository.save(batch)

        logger.info(
            "[MarketBatch#${batch.id}] 완료. status=${batch.status}, " +
                "selected=${batch.selectedCount}, opened=$opened, failed=$failed"
        )
    }

}
