package com.predata.settlement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.predata.settlement", "com.predata.common"])
@EnableFeignClients(basePackages = ["com.predata.common.client"])
class SettlementServiceApplication

fun main(args: Array<String>) {
    runApplication<SettlementServiceApplication>(*args)
}
