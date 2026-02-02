package com.predata.blockchain

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.predata.blockchain", "com.predata.common"])
@EnableFeignClients(basePackages = ["com.predata.common.client"])
class BlockchainServiceApplication

fun main(args: Array<String>) {
    runApplication<BlockchainServiceApplication>(*args)
}
