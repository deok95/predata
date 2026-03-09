package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal

@ConfigurationProperties(prefix = "app.finance")
@Validated
data class FinanceProperties(
    val withdrawal: Withdrawal = Withdrawal(),
    val depositIndexer: DepositIndexer = DepositIndexer(),
) {
    data class Withdrawal(
        val minUsdc: BigDecimal = BigDecimal("1"),
        val maxUsdc: BigDecimal = BigDecimal("100"),
        val feeUsdc: BigDecimal = BigDecimal("0.10"),
    )

    data class DepositIndexer(
        val enabled: Boolean = true,
        val pollIntervalMs: Long = 30000,
        val confirmations: Long = 12,
        val scanWindowBlocks: Long = 500,
        val minDepositUsdc: BigDecimal = BigDecimal("1"),
    )
}

