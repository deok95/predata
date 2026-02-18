package com.predata.backend.service.amm

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FpmmMathEngineTest {

    private fun bd(value: String): BigDecimal = BigDecimal(value).setScale(18, java.math.RoundingMode.HALF_UP)
    private fun bd(value: Double): BigDecimal = BigDecimal.valueOf(value).setScale(18, java.math.RoundingMode.HALF_UP)

    @Test
    fun `calculatePrice - 50-50 풀은 각각 0_5 가격이어야 함`() {
        val y = bd("1000.0")
        val n = bd("1000.0")

        val price = FpmmMathEngine.calculatePrice(y, n)

        assertEquals(bd("0.5"), price.pYes)
        assertEquals(bd("0.5"), price.pNo)
    }

    @Test
    fun `calculatePrice - YES가 많으면 YES 가격이 낮아야 함`() {
        val y = bd("1500.0")  // YES 많음
        val n = bd("500.0")   // NO 적음

        val price = FpmmMathEngine.calculatePrice(y, n)

        // p_yes = N / (Y + N) = 500 / 2000 = 0.25
        // p_no = Y / (Y + N) = 1500 / 2000 = 0.75
        assertEquals(bd("0.25"), price.pYes)
        assertEquals(bd("0.75"), price.pNo)
    }

    @Test
    fun `calculateBuy - YES 구매 시 shares 계산 (50-50 풀, 100 USDC, 1% fee)`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)  // 1,000,000
        val usdcIn = bd("100.0")
        val feeRate = bd("0.01")  // 1%

        val result = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.YES)

        // fee = 100 * 0.01 = 1.0
        // c = 100 - 1 = 99
        // Y1 = 1000 + 99 = 1099, N1 = 1000 + 99 = 1099
        // Y2 = K / N1 = 1,000,000 / 1099 ≈ 909.918...
        // shares_out = Y1 - Y2 = 1099 - 909.918 ≈ 189.08...

        assertEquals(bd("1.0"), result.fee)
        assertTrue(result.sharesOut > bd("189.0") && result.sharesOut < bd("190.0"))

        // 풀 상태 검증
        assertTrue(result.yAfter < y)  // YES shares 감소
        assertTrue(result.nAfter > n)  // NO shares 증가
        assertEquals(bd("1099.0"), result.nAfter)

        // K 방어: newK는 oldK보다 작아지면 안 됨 (반올림/나머지는 풀에 유리하게 남겨야 함)
        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
        val diff = (kAfter.subtract(k)).abs().divide(k, java.math.MathContext.DECIMAL128)
        assertTrue(diff < bd("0.0000000001"), "K drift가 허용치를 초과: $kAfter vs $k")

        // 가격 변화 검증 (YES 구매 후 YES 가격 상승)
        assertTrue(result.priceAfter.pYes > bd("0.5"))
    }

    @Test
    fun `calculateBuy - NO 구매 시 shares 계산 (50-50 풀, 100 USDC, 1% fee)`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)
        val usdcIn = bd("100.0")
        val feeRate = bd("0.01")

        val result = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.NO)

        // fee = 1.0, c = 99
        // Y1 = 1099, N1 = 1099
        // N2 = K / Y1 ≈ 909.918
        // shares_out ≈ 189.08

        assertEquals(bd("1.0"), result.fee)
        assertTrue(result.sharesOut > bd("189.0") && result.sharesOut < bd("190.0"))

        // 풀 상태 검증
        assertTrue(result.yAfter > y)  // YES shares 증가
        assertTrue(result.nAfter < n)  // NO shares 감소
        assertEquals(bd("1099.0"), result.yAfter)

        // K 방어: newK는 oldK보다 작아지면 안 됨
        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
        val diff = (kAfter.subtract(k)).abs().divide(k, java.math.MathContext.DECIMAL128)
        assertTrue(diff < bd("0.0000000001"))

        // 가격 변화 검증 (NO 구매 후 NO 가격 상승)
        assertTrue(result.priceAfter.pNo > bd("0.5"))
    }

    @Test
    fun `calculateBuy - 수수료 0%일 때 동작`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)
        val usdcIn = bd("50.0")
        val feeRate = bd("0.0")  // 0% fee

        val result = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.YES)

        assertEquals(bd("0.0"), result.fee)
        assertTrue(result.sharesOut > bd("0.0"))

        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
        val diff = (kAfter.subtract(k)).abs().divide(k, java.math.MathContext.DECIMAL128)
        assertTrue(diff < bd("0.0000000001"))
    }

    @Test
    fun `calculateSell - YES 판매 시 USDC 계산`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)
        val sharesIn = bd("50.0")
        val feeRate = bd("0.01")  // 1%

        val result = FpmmMathEngine.calculateSell(y, n, k, sharesIn, feeRate, FpmmMathEngine.Outcome.YES)

        // 판매 시 USDC 수령
        assertTrue(result.usdcOut > bd("0.0"))
        assertTrue(result.fee > bd("0.0"))

        // 풀 상태 검증
        assertTrue(result.yAfter > y)  // YES shares 증가
        assertTrue(result.nAfter < n)  // NO shares 감소

        // K 방어: newK는 oldK보다 작아지면 안 됨
        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
        val diff = (kAfter.subtract(k)).abs().divide(k, java.math.MathContext.DECIMAL128)
        assertTrue(diff < bd("0.0000000001"), "K drift가 허용치를 초과: $kAfter vs $k")

        // 가격 변화 검증 (YES 판매 후 YES 가격 하락)
        assertTrue(result.priceAfter.pYes < bd("0.5"))
    }

    @Test
    fun `calculateSell - NO 판매 시 USDC 계산`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)
        val sharesIn = bd("50.0")
        val feeRate = bd("0.01")

        val result = FpmmMathEngine.calculateSell(y, n, k, sharesIn, feeRate, FpmmMathEngine.Outcome.NO)

        assertTrue(result.usdcOut > bd("0.0"))
        assertTrue(result.fee > bd("0.0"))

        // 풀 상태 검증
        assertTrue(result.yAfter < y)  // YES shares 감소
        assertTrue(result.nAfter > n)  // NO shares 증가

        // K 방어: newK는 oldK보다 작아지면 안 됨
        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
        val diff = (kAfter.subtract(k)).abs().divide(k, java.math.MathContext.DECIMAL128)
        assertTrue(diff < bd("0.0000000001"))

        // 가격 변화 검증 (NO 판매 후 NO 가격 하락)
        assertTrue(result.priceAfter.pNo < bd("0.5"))
    }

    @Test
    fun `라운드트립 테스트 - BUY 후 SELL하면 손실은 수수료만`() {
        val y = bd("10000.0")
        val n = bd("10000.0")
        val k = y.multiply(n)
        val usdcIn = bd("100.0")
        val feeRate = bd("0.02")  // 2%

        // BUY YES
        val buyResult = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.YES)
        val sharesReceived = buyResult.sharesOut

        // SELL YES (같은 양)
        val sellResult = FpmmMathEngine.calculateSell(
            buyResult.yAfter,
            buyResult.nAfter,
            k,
            sharesReceived,
            feeRate,
            FpmmMathEngine.Outcome.YES
        )

        // 총 수수료 = BUY 수수료 + SELL 수수료
        val totalFees = buyResult.fee.add(sellResult.fee)
        val netLoss = usdcIn.subtract(sellResult.usdcOut)

        // 손실은 수수료와 거의 같아야 함 (작은 슬리피지 허용)
        val diff = (netLoss.subtract(totalFees)).abs()
        assertTrue(diff < bd("1.0"), "라운드트립 손실이 수수료와 차이남: $netLoss vs $totalFees")
    }

    @Test
    fun `K 불변식 - 다양한 거래 후에도 유지되어야 함`() {
        var y = bd("5000.0")
        var n = bd("5000.0")
        val k = y.multiply(n)  // 25,000,000
        val feeRate = bd("0.01")

        // 연속 거래
        val buy1 = FpmmMathEngine.calculateBuy(y, n, k, bd("200.0"), feeRate, FpmmMathEngine.Outcome.YES)
        y = buy1.yAfter
        n = buy1.nAfter

        val buy2 = FpmmMathEngine.calculateBuy(y, n, k, bd("150.0"), feeRate, FpmmMathEngine.Outcome.NO)
        y = buy2.yAfter
        n = buy2.nAfter

        val sell1 = FpmmMathEngine.calculateSell(y, n, k, bd("50.0"), feeRate, FpmmMathEngine.Outcome.YES)
        y = sell1.yAfter
        n = sell1.nAfter

        // 최종 K 검증
        val kFinal = y.multiply(n)
        assertTrue(kFinal >= k, "K가 감소하면 안 됨: $kFinal vs $k")
        val diff = (kFinal.subtract(k)).abs().divide(k, java.math.MathContext.DECIMAL128)
        assertTrue(diff < bd("0.0000000001"), "K drift가 허용치를 초과: $kFinal vs $k")
    }

    @Test
    fun `에러 - 음수 shares는 거부되어야 함`() {
        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculatePrice(bd("-100.0"), bd("100.0"))
        }
    }

    @Test
    fun `에러 - 0 shares는 거부되어야 함`() {
        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculatePrice(bd("0.0"), bd("100.0"))
        }
    }

    @Test
    fun `에러 - 음수 USDC input은 거부되어야 함`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)

        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculateBuy(y, n, k, bd("-50.0"), bd("0.01"), FpmmMathEngine.Outcome.YES)
        }
    }

    @Test
    fun `에러 - 수수료율이 1 이상이면 거부되어야 함`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)

        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculateBuy(y, n, k, bd("100.0"), bd("1.0"), FpmmMathEngine.Outcome.YES)
        }
    }

    @Test
    fun `에러 - K 불변식이 깨진 입력은 거부되어야 함`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = bd("999999.0")  // Y * N과 다름

        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculateBuy(y, n, k, bd("100.0"), bd("0.01"), FpmmMathEngine.Outcome.YES)
        }
    }

    @Test
    fun `풀 고갈 방지 - 너무 많은 구매는 거부되어야 함`() {
        val y = bd("100.0")
        val n = bd("100.0")
        val k = y.multiply(n)
        val usdcIn = bd("100000.0")  // 풀 크기보다 훨씬 큼 (K의 10배)

        // 풀이 고갈되어 shares < 1.0이 되므로 에러
        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculateBuy(y, n, k, usdcIn, bd("0.01"), FpmmMathEngine.Outcome.YES)
        }
    }

    @Test
    fun `풀 고갈 방지 - 너무 많은 판매는 거부되어야 함`() {
        val y = bd("100.0")
        val n = bd("100.0")
        val k = y.multiply(n)
        val sharesIn = bd("10000.0")  // 풀 크기보다 훨씬 큼

        // discriminant가 음수가 되거나 풀이 고갈되어 에러
        assertThrows<IllegalArgumentException> {
            FpmmMathEngine.calculateSell(y, n, k, sharesIn, bd("0.01"), FpmmMathEngine.Outcome.YES)
        }
    }

    @Test
    fun `대칭성 테스트 - YES와 NO는 대칭적이어야 함`() {
        val y = bd("2000.0")
        val n = bd("2000.0")
        val k = y.multiply(n)
        val usdcIn = bd("100.0")
        val feeRate = bd("0.01")

        val buyYes = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.YES)
        val buyNo = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.NO)

        // 50:50 풀에서는 YES와 NO 구매 결과가 동일해야 함
        assertEquals(buyYes.sharesOut, buyNo.sharesOut)
        assertEquals(buyYes.fee, buyNo.fee)
    }

    @Test
    fun `가격 합 - p_yes + p_no = 1이어야 함`() {
        val y = bd("1234.567")
        val n = bd("8765.432")

        val price = FpmmMathEngine.calculatePrice(y, n)

        val sum = price.pYes.add(price.pNo)
        assertEquals(bd("1.0"), sum)
    }

    @Test
    fun `극단적 가격 - 99대 1 풀`() {
        val y = bd("99.0")
        val n = bd("9901.0")  // K = 99 * 9901 = 980,199
        val k = y.multiply(n)

        val price = FpmmMathEngine.calculatePrice(y, n)

        // p_yes = N / (Y + N) = 9901 / 10000 = 0.9901
        // p_no = Y / (Y + N) = 99 / 10000 = 0.0099
        assertTrue(price.pYes > bd("0.99"))
        assertTrue(price.pNo < bd("0.01"))

        // 이런 극단적 풀에서도 거래 가능해야 함
        val buyResult = FpmmMathEngine.calculateBuy(y, n, k, bd("10.0"), bd("0.01"), FpmmMathEngine.Outcome.YES)
        assertTrue(buyResult.sharesOut > bd("0.0"))
    }

    @Test
    fun `수수료 반올림 - BUY 수수료는 CEILING(UP) 처리되어야 함`() {
        val y = bd("1000.0")
        val n = bd("1000.0")
        val k = y.multiply(n)
        val usdcIn = BigDecimal("1.0000000000000000001") // 19dp
        val feeRate = bd("0.01")

        val result = FpmmMathEngine.calculateBuy(y, n, k, usdcIn, feeRate, FpmmMathEngine.Outcome.YES)

        // 1.0000000000000000001 * 0.01 = 0.01000000000000000001 -> scale(18) UP = 0.010000000000000001
        assertEquals(bd("0.010000000000000001"), result.fee)
    }

    @Test
    fun `보수적 반올림 - BUY sharesOut은 FLOOR(DOWN)되어야 함`() {
        val y = bd("10.0")
        val n = bd("10.0")
        val k = y.multiply(n) // 100

        val result = FpmmMathEngine.calculateBuy(
            yShares = y,
            nShares = n,
            k = k,
            usdcIn = bd("2.0"),
            feeRate = bd("0.0"),
            outcome = FpmmMathEngine.Outcome.YES
        )

        // y1=n1=12, y2=ceil(100/12, 18dp)=8.333333333333333334
        // sharesOut = floor(12 - y2, 18dp)=3.666666666666666666
        assertEquals(bd("3.666666666666666666"), result.sharesOut)
        assertTrue(result.sharesOut < bd("3.666666666666666667"))
    }

    @Test
    fun `sqrt 정밀도 - 매우 큰 풀에서도 SELL 계산이 오버플로우 없이 동작해야 함`() {
        val y = bd("1000000000000000000000000") // 1e24
        val n = bd("1000000000000000000000000") // 1e24
        val k = y.multiply(n) // 1e48

        val result = FpmmMathEngine.calculateSell(
            yShares = y,
            nShares = n,
            k = k,
            sharesIn = bd("1000.0"),
            feeRate = bd("0.01"),
            outcome = FpmmMathEngine.Outcome.YES
        )

        assertTrue(result.usdcOut > bd("0.0"))
        assertTrue(result.fee > bd("0.0"))

        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
    }

    @Test
    fun `극단적 비대칭 풀 - overflow 없이 BUY NO가 동작해야 함`() {
        val y = bd("1.0")
        val n = bd("999999.0")
        val k = y.multiply(n)

        val result = FpmmMathEngine.calculateBuy(
            yShares = y,
            nShares = n,
            k = k,
            usdcIn = bd("50.0"),
            feeRate = bd("0.01"),
            outcome = FpmmMathEngine.Outcome.NO
        )

        assertTrue(result.sharesOut > bd("0.0"))
        val kAfter = result.yAfter.multiply(result.nAfter)
        assertTrue(kAfter >= k, "K가 감소하면 안 됨: $kAfter vs $k")
    }
}
