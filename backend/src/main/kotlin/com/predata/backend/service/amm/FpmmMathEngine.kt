package com.predata.backend.service.amm

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * FPMM (Fixed Product Market Maker) 수식 계산 엔진
 *
 * Complete Set 기반 AMM 수식을 정확히 구현
 * - 모든 계산은 BigDecimal (scale 18, DECIMAL128)
 * - 상태를 가지지 않는 순수 함수
 */
object FpmmMathEngine {

	    private const val SCALE = 18
	    private val ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
	    private val ONE = BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)
	    private val TWO = BigDecimal("2")
	    private val FOUR = BigDecimal("4")
	    private val K_TOL = BigDecimal("0.0000000001") // 1e-10 relative tolerance
	    private val SQRT_MC = MathContext(80, RoundingMode.HALF_UP)

    data class Price(
        val pYes: BigDecimal,
        val pNo: BigDecimal
    )

    data class BuyResult(
        val sharesOut: BigDecimal,
        val fee: BigDecimal,
        val priceAfter: Price,
        val yAfter: BigDecimal,
        val nAfter: BigDecimal
    )

    data class SellResult(
        val usdcOut: BigDecimal,
        val fee: BigDecimal,
        val priceAfter: Price,
        val yAfter: BigDecimal,
        val nAfter: BigDecimal
    )

    enum class Outcome {
        YES, NO
    }

    /**
     * 현재 풀 가격 계산
     * p_yes = N / (Y + N)
     * p_no = 1 - p_yes (반올림 오차 방지)
     */
	    fun calculatePrice(yShares: BigDecimal, nShares: BigDecimal): Price {
        require(yShares > ZERO) { "YES shares must be positive" }
        require(nShares > ZERO) { "NO shares must be positive" }

        val sum = yShares.add(nShares)
	        val pYes = nShares.divide(sum, SCALE, RoundingMode.HALF_UP)
	        val pNo = ONE.subtract(pYes).setScale(SCALE, RoundingMode.HALF_UP)

        return Price(pYes, pNo)
    }

    /**
     * BUY 계산 (USDC → shares)
     *
     * BUY YES:
     *   fee = c_in * f
     *   c = c_in - fee
     *   Y1 = Y + c, N1 = N + c  (complete set mint)
     *   Y2 = K / N1              (불변식 유지)
     *   shares_out = Y1 - Y2
     *   풀 최종: Y = Y2, N = N1
     *
     * BUY NO:
     *   fee = c_in * f
     *   c = c_in - fee
     *   Y1 = Y + c, N1 = N + c
     *   N2 = K / Y1
     *   shares_out = N1 - N2
     *   풀 최종: Y = Y1, N = N2
     */
	    fun calculateBuy(
        yShares: BigDecimal,
        nShares: BigDecimal,
        k: BigDecimal,
        usdcIn: BigDecimal,
        feeRate: BigDecimal,
        outcome: Outcome
    ): BuyResult {
        require(yShares > ZERO) { "YES shares must be positive" }
        require(nShares > ZERO) { "NO shares must be positive" }
        require(k > ZERO) { "K must be positive" }
        require(usdcIn > ZERO) { "USDC input must be positive" }
        require(feeRate >= ZERO && feeRate < ONE) { "Fee rate must be in [0, 1)" }

	        // 불변식 검증 (입력 K가 현재 풀 상태와 일치하는지)
	        val kActual = yShares.multiply(nShares)
	        require((kActual.subtract(k).abs()).compareTo(k.multiply(K_TOL)) < 0) {
	            "K invariant violated: Y * N = $kActual != $k"
	        }

	        // 수수료 계산: 플랫폼 유리하게 CEILING
	        val fee = usdcIn.multiply(feeRate).setScale(SCALE, RoundingMode.UP)
	        val c = usdcIn.subtract(fee).setScale(SCALE, RoundingMode.HALF_UP)
	        require(c > ZERO) { "USDC input is too small after fee rounding" }

        val result = when (outcome) {
	            Outcome.YES -> {
	                // BUY YES
	                val y1 = yShares.add(c)
	                val n1 = nShares.add(c)
	                // K가 감소하지 않도록 y2는 CEILING (유저 수령 shares는 보수적으로 감소)
	                val y2 = k.divide(n1, SCALE, RoundingMode.CEILING)
	                val sharesOut = y1.subtract(y2).setScale(SCALE, RoundingMode.DOWN)

	                require(sharesOut > ZERO) { "Shares output must be positive" }
	                require(y2 >= ONE) { "Pool YES shares would be depleted (< 1.0)" }

                BuyResult(
                    sharesOut = sharesOut,
                    fee = fee,
                    priceAfter = calculatePrice(y2, n1),
                    yAfter = y2,
                    nAfter = n1
                )
            }
	            Outcome.NO -> {
	                // BUY NO
	                val y1 = yShares.add(c)
	                val n1 = nShares.add(c)
	                // K가 감소하지 않도록 n2는 CEILING
	                val n2 = k.divide(y1, SCALE, RoundingMode.CEILING)
	                val sharesOut = n1.subtract(n2).setScale(SCALE, RoundingMode.DOWN)

	                require(sharesOut > ZERO) { "Shares output must be positive" }
	                require(n2 >= ONE) { "Pool NO shares would be depleted (< 1.0)" }

                BuyResult(
                    sharesOut = sharesOut,
                    fee = fee,
                    priceAfter = calculatePrice(y1, n2),
                    yAfter = y1,
                    nAfter = n2
                )
            }
        }

	        // K 방어: 이론상 불변이지만, 반올림/나머지는 풀에 유리하게 남겨 K 감소를 방지한다.
	        // (newK < oldK) 는 유동성 고갈로 이어지므로 차단.
	        val kAfter = result.yAfter.multiply(result.nAfter)
	        require(kAfter >= k) {
	            "K decreased after buy: oldK=$k newK=$kAfter"
	        }

	        return result
	    }

    /**
     * SELL 계산 (shares → USDC)
     *
     * SELL YES:
     *   Y1 = Y + s_in
     *   c_out_gross = ((Y1 + N) - sqrt((Y1 + N)^2 - 4*N*s_in)) / 2
     *   fee = c_out_gross * f
     *   c_out_net = c_out_gross - fee
     *   풀 최종: Y = Y1 - c_out_gross, N = N - c_out_gross
     *
     * SELL NO:
     *   N1 = N + s_in
     *   c_out_gross = ((Y + N1) - sqrt((Y + N1)^2 - 4*Y*s_in)) / 2
     *   fee = c_out_gross * f
     *   c_out_net = c_out_gross - fee
     *   풀 최종: Y = Y - c_out_gross, N = N1 - c_out_gross
     */
	    fun calculateSell(
        yShares: BigDecimal,
        nShares: BigDecimal,
        k: BigDecimal,
        sharesIn: BigDecimal,
        feeRate: BigDecimal,
        outcome: Outcome
    ): SellResult {
        require(yShares > ZERO) { "YES shares must be positive" }
        require(nShares > ZERO) { "NO shares must be positive" }
        require(k > ZERO) { "K must be positive" }
        require(sharesIn > ZERO) { "Shares input must be positive" }
        require(feeRate >= ZERO && feeRate < ONE) { "Fee rate must be in [0, 1)" }

	        // 불변식 검증 (입력 K가 현재 풀 상태와 일치하는지)
	        val kActual = yShares.multiply(nShares)
	        require((kActual.subtract(k).abs()).compareTo(k.multiply(K_TOL)) < 0) {
	            "K invariant violated: Y * N = $kActual != $k"
	        }

        val result = when (outcome) {
	            Outcome.YES -> {
	                // SELL YES
	                val y1 = yShares.add(sharesIn)
	                val sum = y1.add(nShares)
	                val discriminant = sum.multiply(sum).subtract(FOUR.multiply(nShares).multiply(sharesIn))

	                require(discriminant >= ZERO) { "Invalid sell: discriminant negative" }

	                // sqrt는 과소추정하면(작게 나오면) c_out이 커져 유저에게 과지급될 수 있으므로 CEILING으로 보수적 처리
	                val sqrtDiscriminant = sqrt(discriminant)
	                // 유저 USDC 수령을 보수적으로: cOutGross는 DOWN, fee는 UP, net은 DOWN
	                val cOutGross = sum.subtract(sqrtDiscriminant).divide(TWO, SCALE, RoundingMode.DOWN)
	                val fee = cOutGross.multiply(feeRate).setScale(SCALE, RoundingMode.UP)
	                val cOutNet = cOutGross.subtract(fee).setScale(SCALE, RoundingMode.DOWN)

	                require(cOutNet > ZERO) { "USDC output must be positive" }

                val yFinal = y1.subtract(cOutGross)
                val nFinal = nShares.subtract(cOutGross)

                require(yFinal >= ONE) { "Pool YES shares would be depleted (< 1.0)" }
                require(nFinal >= ONE) { "Pool NO shares would be depleted (< 1.0)" }

                SellResult(
                    usdcOut = cOutNet,
                    fee = fee,
                    priceAfter = calculatePrice(yFinal, nFinal),
                    yAfter = yFinal,
                    nAfter = nFinal
                )
            }
	            Outcome.NO -> {
	                // SELL NO
	                val n1 = nShares.add(sharesIn)
	                val sum = yShares.add(n1)
	                val discriminant = sum.multiply(sum).subtract(FOUR.multiply(yShares).multiply(sharesIn))

	                require(discriminant >= ZERO) { "Invalid sell: discriminant negative" }

	                val sqrtDiscriminant = sqrt(discriminant)
	                val cOutGross = sum.subtract(sqrtDiscriminant).divide(TWO, SCALE, RoundingMode.DOWN)
	                val fee = cOutGross.multiply(feeRate).setScale(SCALE, RoundingMode.UP)
	                val cOutNet = cOutGross.subtract(fee).setScale(SCALE, RoundingMode.DOWN)

                require(cOutNet > ZERO) { "USDC output must be positive" }

                val yFinal = yShares.subtract(cOutGross)
                val nFinal = n1.subtract(cOutGross)

                require(yFinal >= ONE) { "Pool YES shares would be depleted (< 1.0)" }
                require(nFinal >= ONE) { "Pool NO shares would be depleted (< 1.0)" }

                SellResult(
                    usdcOut = cOutNet,
                    fee = fee,
                    priceAfter = calculatePrice(yFinal, nFinal),
                    yAfter = yFinal,
                    nAfter = nFinal
                )
            }
        }

	        // K 방어: newK가 감소하면 유동성 고갈/인솔벤시 위험. 감소는 차단한다.
	        val kAfter = result.yAfter.multiply(result.nAfter)
	        require(kAfter >= k) {
	            "K decreased after sell: oldK=$k newK=$kAfter"
	        }

	        return result
	    }

    /**
     * BigDecimal sqrt 구현 (Newton's method)
     *
     * x_{n+1} = (x_n + S/x_n) / 2
     */
	    private fun sqrt(value: BigDecimal): BigDecimal {
	        if (value.compareTo(BigDecimal.ZERO) == 0) return ZERO
	        require(value > BigDecimal.ZERO) { "Cannot take square root of negative number" }

	        // Java 9+ BigDecimal.sqrt(MathContext) 사용: double sqrt 사용 금지 + 큰 수에서도 안정적
	        // SELL 수식에서 sqrt 과소추정을 피하기 위해 CEILING으로 마무리한다.
	        return value.sqrt(SQRT_MC).setScale(SCALE, RoundingMode.CEILING)
	    }
}
