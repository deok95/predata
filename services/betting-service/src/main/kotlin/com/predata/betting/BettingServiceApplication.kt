package com.predata.betting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.predata.betting", "com.predata.common"])
@EnableFeignClients(basePackages = ["com.predata.common.client"])
class BettingServiceApplication

fun main(args: Array<String>) {
    runApplication<BettingServiceApplication>(*args)
}
