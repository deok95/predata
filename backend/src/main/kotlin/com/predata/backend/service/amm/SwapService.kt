package com.predata.backend.service.amm

import com.predata.backend.domain.*
import com.predata.backend.dto.amm.*
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.amm.SwapHistoryRepository
import com.predata.backend.repository.amm.UserSharesRepository
import com.predata.backend.service.FeePoolService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class SwapService(
    private val marketPoolRepository: MarketPoolRepository,
    private val userSharesRepository: UserSharesRepository,
    private val swapHistoryRepository: SwapHistoryRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val feePoolService: FeePoolService
) {
    private val logger = LoggerFactory.getLogger(SwapService::class.java)

    companion object {
        private val SCALE = 18
        private val ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
        private val ONE = BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)
        private val MIN_AMOUNT = ONE // 최소 거래 금액 1.0
    }

    /**
     * 스왑 실행 (BUY 또는 SELL)
     */
    @Transactional
    fun executeSwap(memberId: Long, request: SwapRequest): SwapResponse {
        // 1. 기본 검증
        val question = questionRepository.findById(request.questionId).orElseThrow {
            IllegalArgumentException("질문을 찾을 수 없습니다: ${request.questionId}")
        }

        require(question.executionModel == ExecutionModel.AMM_FPMM) {
            "해당 질문은 AMM 실행 모델이 아닙니다."
        }

        require(question.status == QuestionStatus.BETTING) {
            "베팅 기간이 아닙니다. 현재 상태: ${question.status}"
        }

        val pool = marketPoolRepository.findByIdWithLock(request.questionId).orElseThrow {
            IllegalArgumentException("마켓 풀을 찾을 수 없습니다.")
        }

        require(pool.status == PoolStatus.ACTIVE) {
            "마켓 풀이 활성화되지 않았습니다. 현재 상태: ${pool.status}"
        }

        val member = memberRepository.findById(memberId).orElseThrow {
            IllegalArgumentException("회원을 찾을 수 없습니다.")
        }

        // 2. 액션별 처리
        return when (request.action) {
            SwapAction.BUY -> executeBuy(member, pool, request)
            SwapAction.SELL -> executeSell(member, pool, request)
        }
    }

    private fun executeBuy(member: Member, pool: MarketPool, request: SwapRequest): SwapResponse {
        val usdcIn = request.usdcIn ?: throw IllegalArgumentException("BUY 시 usdcIn은 필수입니다.")

        require(usdcIn >= MIN_AMOUNT) {
            "최소 거래 금액은 $MIN_AMOUNT USDC입니다."
        }

        require(member.usdcBalance >= usdcIn) {
            "USDC 잔액이 부족합니다. 보유: ${member.usdcBalance}, 필요: $usdcIn"
        }

        // Before 스냅샷 캡처 (pool 업데이트 전)
        val yesBefore = pool.yesShares
        val noBefore = pool.noShares
        val priceBefore = FpmmMathEngine.calculatePrice(yesBefore, noBefore)
        val k = yesBefore.multiply(noBefore)

        // 스왑 계산
        val outcome = when (request.outcome) {
            ShareOutcome.YES -> FpmmMathEngine.Outcome.YES
            ShareOutcome.NO -> FpmmMathEngine.Outcome.NO
        }

        val buyResult = FpmmMathEngine.calculateBuy(
            yShares = pool.yesShares,
            nShares = pool.noShares,
            k = k,
            usdcIn = usdcIn,
            feeRate = pool.feeRate,
            outcome = outcome
        )

        // 슬리피지 보호
        request.minSharesOut?.let { minShares ->
            require(buyResult.sharesOut >= minShares) {
                "슬리피지 초과: 예상 최소 $minShares shares, 실제 ${buyResult.sharesOut} shares"
            }
        }

        // 상태 업데이트
        pool.yesShares = buyResult.yAfter
        pool.noShares = buyResult.nAfter
        val cNet = usdcIn.subtract(buyResult.fee)
        pool.collateralLocked = pool.collateralLocked.add(cNet)
        pool.totalVolumeUsdc = pool.totalVolumeUsdc.add(usdcIn)
        pool.totalFeesUsdc = pool.totalFeesUsdc.add(buyResult.fee)
        pool.updatedAt = LocalDateTime.now()

        marketPoolRepository.save(pool)

        // 회원 USDC 차감
        member.usdcBalance = member.usdcBalance.subtract(usdcIn)
        memberRepository.save(member)

        // UserShares 업데이트
        val userSharesId = UserSharesId(member.id!!, request.questionId, request.outcome)
        val userShares = userSharesRepository.findById(userSharesId).orElse(
            UserShares(
                memberId = member.id,
                questionId = request.questionId,
                outcome = request.outcome
            )
        )

        userShares.shares = userShares.shares.add(buyResult.sharesOut)
        userShares.costBasisUsdc = userShares.costBasisUsdc.add(usdcIn)
        userShares.updatedAt = LocalDateTime.now()
        userSharesRepository.save(userShares)

        // 수수료 분배 (FeePoolService 활용)
        if (buyResult.fee > ZERO) {
            feePoolService.collectFee(request.questionId, buyResult.fee)
        }

        // SwapHistory 기록
        val swapHistory = SwapHistory(
            memberId = member.id,
            questionId = request.questionId,
            action = SwapAction.BUY,
            outcome = request.outcome,
            usdcIn = usdcIn,
            usdcOut = ZERO,
            sharesIn = ZERO,
            sharesOut = buyResult.sharesOut,
            feeUsdc = buyResult.fee,
            priceBeforeYes = priceBefore.pYes.setScale(4, RoundingMode.HALF_UP),
            priceAfterYes = buyResult.priceAfter.pYes.setScale(4, RoundingMode.HALF_UP),
            yesBefore = yesBefore,
            noBefore = noBefore,
            yesAfter = buyResult.yAfter,
            noAfter = buyResult.nAfter
        )
        swapHistoryRepository.save(swapHistory)

        logger.info(
            "BUY executed: memberId=${member.id}, questionId=${request.questionId}, " +
            "outcome=${request.outcome}, usdcIn=$usdcIn, sharesOut=${buyResult.sharesOut}, fee=${buyResult.fee}"
        )

        // 응답 생성
        return buildSwapResponse(
            sharesAmount = buyResult.sharesOut,
            usdcAmount = usdcIn,
            effectivePrice = usdcIn.divide(buyResult.sharesOut, SCALE, RoundingMode.HALF_UP),
            fee = buyResult.fee,
            priceBefore = priceBefore,
            priceAfter = buyResult.priceAfter,
            poolState = PoolSnapshot(buyResult.yAfter, buyResult.nAfter, pool.collateralLocked),
            memberId = member.id,
            questionId = request.questionId
        )
    }

    private fun executeSell(member: Member, pool: MarketPool, request: SwapRequest): SwapResponse {
        val sharesIn = request.sharesIn ?: throw IllegalArgumentException("SELL 시 sharesIn은 필수입니다.")

        require(sharesIn >= MIN_AMOUNT) {
            "최소 거래 금액은 $MIN_AMOUNT shares입니다."
        }

        // UserShares 확인
        val userSharesId = UserSharesId(member.id!!, request.questionId, request.outcome)
        val userShares = userSharesRepository.findById(userSharesId).orElseThrow {
            IllegalArgumentException("보유한 shares가 없습니다.")
        }

        require(userShares.shares >= sharesIn) {
            "shares 잔액이 부족합니다. 보유: ${userShares.shares}, 필요: $sharesIn"
        }

        // Before 스냅샷 캡처 (pool 업데이트 전)
        val yesBefore = pool.yesShares
        val noBefore = pool.noShares
        val priceBefore = FpmmMathEngine.calculatePrice(yesBefore, noBefore)
        val k = yesBefore.multiply(noBefore)

        // 스왑 계산
        val outcome = when (request.outcome) {
            ShareOutcome.YES -> FpmmMathEngine.Outcome.YES
            ShareOutcome.NO -> FpmmMathEngine.Outcome.NO
        }

        val sellResult = FpmmMathEngine.calculateSell(
            yShares = pool.yesShares,
            nShares = pool.noShares,
            k = k,
            sharesIn = sharesIn,
            feeRate = pool.feeRate,
            outcome = outcome
        )

        // 슬리피지 보호
        request.minUsdcOut?.let { minUsdc ->
            require(sellResult.usdcOut >= minUsdc) {
                "슬리피지 초과: 예상 최소 $minUsdc USDC, 실제 ${sellResult.usdcOut} USDC"
            }
        }

        // c_out_gross 계산 (net + fee)
        val cOutGross = sellResult.usdcOut.add(sellResult.fee)

        // 상태 업데이트
        pool.yesShares = sellResult.yAfter
        pool.noShares = sellResult.nAfter
        pool.collateralLocked = pool.collateralLocked.subtract(cOutGross)
        pool.totalVolumeUsdc = pool.totalVolumeUsdc.add(cOutGross)
        pool.totalFeesUsdc = pool.totalFeesUsdc.add(sellResult.fee)
        pool.updatedAt = LocalDateTime.now()

        marketPoolRepository.save(pool)

        // 회원 USDC 증가
        member.usdcBalance = member.usdcBalance.add(sellResult.usdcOut)
        memberRepository.save(member)

        // UserShares 업데이트
        userShares.shares = userShares.shares.subtract(sharesIn)

        // costBasis 비례 차감
        val sellRatio = sharesIn.divide(userShares.shares.add(sharesIn), SCALE, RoundingMode.HALF_UP)
        val costBasisReduction = userShares.costBasisUsdc.multiply(sellRatio).setScale(SCALE, RoundingMode.HALF_UP)
        userShares.costBasisUsdc = userShares.costBasisUsdc.subtract(costBasisReduction)

        userShares.updatedAt = LocalDateTime.now()
        userSharesRepository.save(userShares)

        // 수수료 분배
        if (sellResult.fee > ZERO) {
            feePoolService.collectFee(request.questionId, sellResult.fee)
        }

        // SwapHistory 기록
        val swapHistory = SwapHistory(
            memberId = member.id,
            questionId = request.questionId,
            action = SwapAction.SELL,
            outcome = request.outcome,
            usdcIn = ZERO,
            usdcOut = sellResult.usdcOut,
            sharesIn = sharesIn,
            sharesOut = ZERO,
            feeUsdc = sellResult.fee,
            priceBeforeYes = priceBefore.pYes.setScale(4, RoundingMode.HALF_UP),
            priceAfterYes = sellResult.priceAfter.pYes.setScale(4, RoundingMode.HALF_UP),
            yesBefore = yesBefore,
            noBefore = noBefore,
            yesAfter = sellResult.yAfter,
            noAfter = sellResult.nAfter
        )
        swapHistoryRepository.save(swapHistory)

        logger.info(
            "SELL executed: memberId=${member.id}, questionId=${request.questionId}, " +
            "outcome=${request.outcome}, sharesIn=$sharesIn, usdcOut=${sellResult.usdcOut}, fee=${sellResult.fee}"
        )

        // 응답 생성
        return buildSwapResponse(
            sharesAmount = sharesIn,
            usdcAmount = sellResult.usdcOut,
            effectivePrice = sellResult.usdcOut.divide(sharesIn, SCALE, RoundingMode.HALF_UP),
            fee = sellResult.fee,
            priceBefore = priceBefore,
            priceAfter = sellResult.priceAfter,
            poolState = PoolSnapshot(sellResult.yAfter, sellResult.nAfter, pool.collateralLocked),
            memberId = member.id,
            questionId = request.questionId
        )
    }

    private fun buildSwapResponse(
        sharesAmount: BigDecimal,
        usdcAmount: BigDecimal,
        effectivePrice: BigDecimal,
        fee: BigDecimal,
        priceBefore: FpmmMathEngine.Price,
        priceAfter: FpmmMathEngine.Price,
        poolState: PoolSnapshot,
        memberId: Long,
        questionId: Long
    ): SwapResponse {
        // 현재 유저의 전체 shares 조회
        val userSharesList = userSharesRepository.findByMemberIdAndQuestionId(memberId, questionId)

        val yesShares = userSharesList.find { it.outcome == ShareOutcome.YES }
        val noShares = userSharesList.find { it.outcome == ShareOutcome.NO }

        return SwapResponse(
            sharesAmount = sharesAmount,
            usdcAmount = usdcAmount,
            effectivePrice = effectivePrice,
            fee = fee,
            priceBefore = PriceSnapshot(priceBefore.pYes, priceBefore.pNo),
            priceAfter = PriceSnapshot(priceAfter.pYes, priceAfter.pNo),
            poolState = poolState,
            myShares = MySharesSnapshot(
                yesShares = yesShares?.shares ?: ZERO,
                noShares = noShares?.shares ?: ZERO,
                yesCostBasis = yesShares?.costBasisUsdc ?: ZERO,
                noCostBasis = noShares?.costBasisUsdc ?: ZERO
            )
        )
    }

    /**
     * 스왑 시뮬레이션 (DB 변경 없음)
     */
    @Transactional(readOnly = true)
    fun simulateSwap(
        questionId: Long,
        action: SwapAction,
        outcome: ShareOutcome,
        amount: BigDecimal
    ): SwapSimulationResponse {
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("질문을 찾을 수 없습니다.")
        }

        require(question.executionModel == ExecutionModel.AMM_FPMM) {
            "해당 질문은 AMM 실행 모델이 아닙니다."
        }

        require(question.status == QuestionStatus.BETTING) {
            "베팅 기간이 아닙니다. 현재 상태: ${question.status}"
        }

        val pool = marketPoolRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("마켓 풀을 찾을 수 없습니다.")
        }

        require(pool.status == PoolStatus.ACTIVE) {
            "마켓 풀이 활성화되지 않았습니다. 현재 상태: ${pool.status}"
        }

        val priceBefore = FpmmMathEngine.calculatePrice(pool.yesShares, pool.noShares)
        val k = pool.yesShares.multiply(pool.noShares)

        val fpmmOutcome = when (outcome) {
            ShareOutcome.YES -> FpmmMathEngine.Outcome.YES
            ShareOutcome.NO -> FpmmMathEngine.Outcome.NO
        }

        return when (action) {
            SwapAction.BUY -> {
                val result = FpmmMathEngine.calculateBuy(
                    yShares = pool.yesShares,
                    nShares = pool.noShares,
                    k = k,
                    usdcIn = amount,
                    feeRate = pool.feeRate,
                    outcome = fpmmOutcome
                )

                val effectivePrice = amount.divide(result.sharesOut, SCALE, RoundingMode.HALF_UP)
                val slippage = calculateSlippage(priceBefore, result.priceAfter, outcome)

                SwapSimulationResponse(
                    sharesOut = result.sharesOut,
                    usdcOut = null,
                    effectivePrice = effectivePrice,
                    slippage = slippage,
                    fee = result.fee,
                    minReceived = result.sharesOut,
                    priceBefore = PriceSnapshot(priceBefore.pYes, priceBefore.pNo),
                    priceAfter = PriceSnapshot(result.priceAfter.pYes, result.priceAfter.pNo)
                )
            }
            SwapAction.SELL -> {
                val result = FpmmMathEngine.calculateSell(
                    yShares = pool.yesShares,
                    nShares = pool.noShares,
                    k = k,
                    sharesIn = amount,
                    feeRate = pool.feeRate,
                    outcome = fpmmOutcome
                )

                val effectivePrice = result.usdcOut.divide(amount, SCALE, RoundingMode.HALF_UP)
                val slippage = calculateSlippage(priceBefore, result.priceAfter, outcome)

                SwapSimulationResponse(
                    sharesOut = null,
                    usdcOut = result.usdcOut,
                    effectivePrice = effectivePrice,
                    slippage = slippage,
                    fee = result.fee,
                    minReceived = result.usdcOut,
                    priceBefore = PriceSnapshot(priceBefore.pYes, priceBefore.pNo),
                    priceAfter = PriceSnapshot(result.priceAfter.pYes, result.priceAfter.pNo)
                )
            }
        }
    }

    private fun calculateSlippage(
        priceBefore: FpmmMathEngine.Price,
        priceAfter: FpmmMathEngine.Price,
        outcome: ShareOutcome
    ): BigDecimal {
        val before = if (outcome == ShareOutcome.YES) priceBefore.pYes else priceBefore.pNo
        val after = if (outcome == ShareOutcome.YES) priceAfter.pYes else priceAfter.pNo

        return if (before > ZERO) {
            after.subtract(before).divide(before, SCALE, RoundingMode.HALF_UP).abs()
        } else {
            ZERO
        }
    }

    /**
     * 풀 초기화 (ADMIN 전용)
     */
    @Transactional
    fun seedPool(request: SeedPoolRequest): SeedPoolResponse {
        require(request.seedUsdc > ZERO) {
            "시드 금액은 0보다 커야 합니다."
        }

        require(request.feeRate >= ZERO && request.feeRate < ONE) {
            "수수료율은 [0, 1) 범위여야 합니다."
        }

        // 이미 풀이 있는지 확인
        val existingPool = marketPoolRepository.findById(request.questionId)
        if (existingPool.isPresent) {
            throw IllegalStateException("해당 질문에 이미 마켓 풀이 존재합니다.")
        }

        val question = questionRepository.findById(request.questionId).orElseThrow {
            IllegalArgumentException("질문을 찾을 수 없습니다.")
        }

        require(question.status != QuestionStatus.SETTLED && question.status != QuestionStatus.CANCELLED) {
            "정산 완료되거나 취소된 질문에는 풀을 생성할 수 없습니다."
        }

        // 풀 생성
        val yShares = request.seedUsdc
        val nShares = request.seedUsdc
        val k = yShares.multiply(nShares)

        val pool = MarketPool(
            questionId = request.questionId,
            yesShares = yShares,
            noShares = nShares,
            feeRate = request.feeRate,
            collateralLocked = request.seedUsdc,
            status = PoolStatus.ACTIVE
        )

        marketPoolRepository.save(pool)

        // Question의 executionModel 업데이트
        question.executionModel = ExecutionModel.AMM_FPMM
        questionRepository.save(question)

        val currentPrice = FpmmMathEngine.calculatePrice(yShares, nShares)

        logger.info(
            "Pool seeded: questionId=${request.questionId}, seedUsdc=${request.seedUsdc}, " +
            "feeRate=${request.feeRate}, K=$k"
        )

        return SeedPoolResponse(
            questionId = request.questionId,
            yesShares = yShares,
            noShares = nShares,
            collateralLocked = request.seedUsdc,
            feeRate = request.feeRate,
            k = k,
            currentPrice = PriceSnapshot(currentPrice.pYes, currentPrice.pNo)
        )
    }

    /**
     * 풀 상태 조회
     */
    @Transactional(readOnly = true)
    fun getPoolState(questionId: Long): PoolStateResponse {
        val pool = marketPoolRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("마켓 풀을 찾을 수 없습니다.")
        }

        val k = pool.yesShares.multiply(pool.noShares)
        val currentPrice = FpmmMathEngine.calculatePrice(pool.yesShares, pool.noShares)

        return PoolStateResponse(
            questionId = questionId,
            status = pool.status,
            yesShares = pool.yesShares,
            noShares = pool.noShares,
            k = k,
            feeRate = pool.feeRate,
            collateralLocked = pool.collateralLocked,
            totalVolumeUsdc = pool.totalVolumeUsdc,
            totalFeesUsdc = pool.totalFeesUsdc,
            currentPrice = PriceSnapshot(currentPrice.pYes, currentPrice.pNo),
            version = pool.version
        )
    }

    /**
     * 내 포지션 조회
     */
    @Transactional(readOnly = true)
    fun getMyShares(memberId: Long, questionId: Long): MySharesSnapshot {
        val rows = userSharesRepository.findByMemberIdAndQuestionId(memberId, questionId)

        val yes = rows.firstOrNull { it.outcome == ShareOutcome.YES }
        val no = rows.firstOrNull { it.outcome == ShareOutcome.NO }

        return MySharesSnapshot(
            yesShares = yes?.shares ?: ZERO,
            noShares = no?.shares ?: ZERO,
            yesCostBasis = yes?.costBasisUsdc ?: ZERO,
            noCostBasis = no?.costBasisUsdc ?: ZERO
        )
    }

    /**
     * 가격 히스토리 조회
     * swap_history에서 가격 변동 데이터를 가져옴
     */
    @Transactional(readOnly = true)
    fun getPriceHistory(questionId: Long, limit: Int = 100): List<PricePointResponse> {
        // 질문 존재 여부 확인 - 없으면 빈 리스트 반환
        val question = questionRepository.findById(questionId).orElse(null)
            ?: return emptyList()

        // 풀 존재 여부 확인 - 없으면 빈 리스트 반환 (ORDERBOOK_LEGACY 등)
        val pool = marketPoolRepository.findById(questionId).orElse(null)
            ?: return emptyList()

        // swap_history에서 오름차순으로 스왑 조회
        val pageable = PageRequest.of(0, limit)
        val swapHistory = swapHistoryRepository.findByQuestionIdOrderByCreatedAtAsc(questionId, pageable)

        val pricePoints = mutableListOf<PricePointResponse>()

        // 시드 시점 (첫 포인트)
        pricePoints.add(
            PricePointResponse(
                timestamp = pool.createdAt,
                yesPrice = BigDecimal("0.50"),
                noPrice = BigDecimal("0.50")
            )
        )

        // 각 스왑 후 가격 추가 (오름차순으로 정렬됨)
        swapHistory.content.forEach { swap ->
            val yesPrice = swap.priceAfterYes
            val noPrice = ONE.subtract(yesPrice)

            pricePoints.add(
                PricePointResponse(
                    timestamp = swap.createdAt,
                    yesPrice = yesPrice,
                    noPrice = noPrice
                )
            )
        }

        return pricePoints
    }
}
