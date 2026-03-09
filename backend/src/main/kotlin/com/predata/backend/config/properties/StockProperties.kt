package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "stock.api")
data class StockProperties(
    val enabled: Boolean = true,
    val yahooBaseUrl: String = "https://query1.finance.yahoo.com",
)
