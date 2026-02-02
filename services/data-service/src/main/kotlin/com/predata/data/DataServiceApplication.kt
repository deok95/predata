package com.predata.data

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.predata.data", "com.predata.common"])
@EnableFeignClients(basePackages = ["com.predata.common.client"])
class DataServiceApplication

fun main(args: Array<String>) {
    runApplication<DataServiceApplication>(*args)
}
