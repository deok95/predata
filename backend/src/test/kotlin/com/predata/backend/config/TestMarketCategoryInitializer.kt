package com.predata.backend.config

import com.predata.backend.domain.market.MarketCategory
import com.predata.backend.repository.market.MarketCategoryRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 테스트 환경에서 Flyway가 비활성화되어 V39/V41 시드 데이터가 없으므로,
 * Spring 컨텍스트 시작 시 market_categories 기본 데이터를 시드한다.
 * - 멱등: 이미 데이터가 있으면 스킵
 * - 공유 컨텍스트 덕에 1회만 실행됨
 * - TRENDING: is_active=false (자동생성 전용, 마켓 선별 제외)
 */
@Component
@Profile("test")
class TestMarketCategoryInitializer(
    private val categoryRepository: MarketCategoryRepository,
) : InitializingBean {

    override fun afterPropertiesSet() {
        if (categoryRepository.count() > 0L) return

        categoryRepository.saveAll(
            listOf(
                MarketCategory("SPORTS",        "스포츠",              isActive = true,  sortOrder = 1),
                MarketCategory("POLITICS",       "정치",                isActive = true,  sortOrder = 2),
                MarketCategory("ECONOMY",        "경제",                isActive = true,  sortOrder = 3),
                MarketCategory("TECH",           "테크",                isActive = true,  sortOrder = 4),
                MarketCategory("ENTERTAINMENT",  "연예",                isActive = true,  sortOrder = 5),
                MarketCategory("CULTURE",        "문화",                isActive = true,  sortOrder = 6),
                MarketCategory("INTERNATIONAL",  "국제",                isActive = true,  sortOrder = 7),
                MarketCategory("GENERAL",        "일반",                isActive = true,  sortOrder = 99),
                // 시스템 내부 카테고리: FK 만족용, 마켓 선별 제외
                MarketCategory("TRENDING",       "트렌딩 (자동생성 전용)", isActive = false, sortOrder = 100),
            )
        )
    }
}
