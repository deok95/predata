package com.predata.sports

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.predata.sports", "com.predata.common"])
@EnableFeignClients(basePackages = ["com.predata.common.client"])
class SportsServiceApplication

fun main(args: Array<String>) {
    runApplication<SportsServiceApplication>(*args)
}
