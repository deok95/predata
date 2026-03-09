package com.predata.backend.domain.market

import jakarta.persistence.*

/**
 * 마켓 카테고리 단일 소스
 * - Top3 선별 기준 카테고리 정의
 * - code(PK)는 questions.category 값과 대응
 */
@Entity
@Table(name = "market_categories")
class MarketCategory(
    @Id
    @Column(length = 50)
    val code: String,

    @Column(name = "display_name", nullable = false, length = 100)
    var displayName: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
)
