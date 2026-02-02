package com.predata.question

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = ["com.predata.question", "com.predata.common"])
@EnableFeignClients(basePackages = ["com.predata.common.client"])
class QuestionServiceApplication

fun main(args: Array<String>) {
    runApplication<QuestionServiceApplication>(*args)
}
