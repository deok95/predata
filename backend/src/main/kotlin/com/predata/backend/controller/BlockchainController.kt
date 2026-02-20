package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.dto.BlockchainStatusResponse
import com.predata.backend.dto.QuestionOnChain
import com.predata.backend.exception.NotFoundException
import com.predata.backend.service.BlockchainService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 블록체인 관련 API
 */
@RestController
@RequestMapping("/api/blockchain")
class BlockchainController(
    private val blockchainService: BlockchainService
) {

    /**
     * 블록체인 상태 조회
     * GET /api/blockchain/status
     */
    @GetMapping("/status")
    fun getBlockchainStatus(): ResponseEntity<ApiEnvelope<BlockchainStatusResponse>> {
        val status = blockchainService.getBlockchainStatus()
        return ResponseEntity.ok(ApiEnvelope.ok(status))
    }

    /**
     * 온체인 질문 데이터 조회
     * GET /api/blockchain/question/{questionId}
     */
    @GetMapping("/question/{questionId}")
    fun getQuestionFromChain(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<QuestionOnChain>> {
        val onChainData = blockchainService.getQuestionFromChain(questionId)

        return if (onChainData != null) {
            ResponseEntity.ok(ApiEnvelope.ok(onChainData))
        } else {
            throw NotFoundException("Question not found.")
        }
    }
}
