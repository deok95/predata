package com.predata.backend.service

import com.predata.backend.exception.BadRequestException
import com.predata.backend.exception.ErrorCode
import com.predata.backend.repository.market.MarketCategoryRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 질문 카테고리 유효성 검증 서비스
 *
 * - market_categories.is_active=true 코드셋을 Caffeine 캐시로 유지 (hot path 보호)
 * - normalizeAndValidate: trim().uppercase() 후 허용 코드셋 확인
 * - 허용되지 않으면 INVALID_CATEGORY(400) 발생 → GlobalExceptionHandler 처리
 */
@Service
class CategoryValidationService(
    private val categoryRepository: MarketCategoryRepository,
) {
    /**
     * 활성 카테고리 코드셋 조회 (캐시됨)
     *
     * 캐시 무효화는 market_categories 변경 시 수동 evict 또는 재시작.
     * 운영상 카테고리 추가/비활성화 빈도가 매우 낮아 TTL-based evict 불필요.
     */
    @Cacheable("activeMarketCategories")
    fun activeCodes(): Set<String> = categoryRepository
        .findByIsActiveTrueOrderBySortOrderAsc()
        .map { it.code }
        .toSet()

    /**
     * 카테고리 정규화 + 유효성 검증
     *
     * @param raw 입력값 (대소문자/공백 허용)
     * @return 정규화된 카테고리 코드 (uppercase, trim 적용)
     * @throws BadRequestException 활성 카테고리에 없는 값일 경우
     */
    fun normalizeAndValidate(raw: String): String {
        val normalized = raw.trim().uppercase()
        val allowed = activeCodes()
        if (normalized !in allowed) {
            throw BadRequestException(
                message = "유효하지 않은 카테고리: '$raw'. 허용값: $allowed",
                code = ErrorCode.INVALID_CATEGORY.name,
            )
        }
        return normalized
    }
}
