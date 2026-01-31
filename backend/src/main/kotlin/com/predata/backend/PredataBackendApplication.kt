package com.predata.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling  // 스케줄링 활성화
class PredataBackendApplication

fun main(args: Array<String>) {
    runApplication<PredataBackendApplication>(*args)
}
