package com.predata.backend.controller

import com.predata.backend.service.BlockchainService
import com.predata.backend.service.QuestionOnChain
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/blockchain")
@CrossOrigin(origins = ["http://localhost:3000"])
class BlockchainController(
    private val blockchainService: BlockchainService
) {
    
    /**
     * 온체인 질문 데이터 조회 (검증용)
     * GET /api/blockchain/question/{id}
     */
    @GetMapping("/question/{id}")
    fun getQuestionFromChain(@PathVariable id: Long): ResponseEntity<QuestionOnChain> {
        val onChainData = blockchainService.getQuestionFromChain(id)
        
        return if (onChainData != null) {
            ResponseEntity.ok(onChainData)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 블록체인 서비스 상태 확인
     * GET /api/blockchain/status
     */
    @GetMapping("/status")
    fun getBlockchainStatus(): ResponseEntity<BlockchainStatus> {
        return ResponseEntity.ok(
            BlockchainStatus(
                enabled = blockchainService.toString().contains("활성화"),
                network = "Base Sepolia",
                contractAddress = System.getenv("BLOCKCHAIN_CONTRACT_ADDRESS") ?: "Not configured"
            )
        )
    }
}

data class BlockchainStatus(
    val enabled: Boolean,
    val network: String,
    val contractAddress: String
)
