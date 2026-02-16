package com.predata.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "trend_signals")
data class TrendSignal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "signal_date", nullable = false)
    val signalDate: LocalDate,

    @Column(name = "subcategory", nullable = false, length = 50)
    val subcategory: String,

    @Column(name = "keyword", nullable = false, length = 255)
    val keyword: String,

    @Column(name = "trend_score", nullable = false)
    val trendScore: Int,

    @Column(name = "region", nullable = false, length = 10)
    val region: String = "US",

    @Column(name = "source", nullable = false, length = 50)
    val source: String = "GOOGLE_TRENDS",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
