package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polygon")
data class PolygonProperties(
    val rpcUrl: String = "https://rpc-amoy.polygon.technology",
    val receiverWallet: String = "",
    val usdcContract: String = "",
    val chainId: Long = 80002L,
    val senderPrivateKey: String = "",
) {
    val isConfigured: Boolean
        get() = senderPrivateKey.isNotBlank() && receiverWallet.isNotBlank()
}
