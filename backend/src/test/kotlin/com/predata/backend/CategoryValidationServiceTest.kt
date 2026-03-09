package com.predata.backend

import com.predata.backend.exception.BadRequestException
import com.predata.backend.exception.ErrorCode
import com.predata.backend.service.CategoryValidationService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * CVS-001~005: CategoryValidationService 단위 동작 검증
 *
 * TestMarketCategoryInitializer 가 Spring 컨텍스트 시작 시
 * 8개 활성 카테고리 + TRENDING(is_active=false)을 시드.
 *
 * 검증:
 * 1. 정확한 코드(SPORTS) → normalizeAndValidate 성공, 코드 반환
 * 2. 소문자+공백(" sports ") → trim().uppercase() 정규화 후 성공
 * 3. 오타(SPORTZ) → BadRequestException(INVALID_CATEGORY)
 * 4. 비어있는 문자열("") → BadRequestException(INVALID_CATEGORY)
 * 5. TRENDING(is_active=false) → 사용자 입력으로는 INVALID_CATEGORY
 *    (AutoQuestionGenerationService는 직접 저장하여 이 검증을 우회함)
 */
@SpringBootTest
@ActiveProfiles("test")
class CategoryValidationServiceTest {

    @Autowired
    private lateinit var categoryValidationService: CategoryValidationService

    @Test
    fun `CVS-001 - 유효한 코드(SPORTS)는 정규화 없이 그대로 반환`() {
        val result = categoryValidationService.normalizeAndValidate("SPORTS")
        assertThat(result).isEqualTo("SPORTS")
    }

    @Test
    fun `CVS-002 - 소문자+공백은 trim+uppercase 정규화 후 통과`() {
        val result = categoryValidationService.normalizeAndValidate("  sports  ")
        assertThat(result).isEqualTo("SPORTS")
    }

    @Test
    fun `CVS-003 - 오타 카테고리(SPORTZ)는 INVALID_CATEGORY 400 예외`() {
        assertThatThrownBy { categoryValidationService.normalizeAndValidate("SPORTZ") }
            .isInstanceOf(BadRequestException::class.java)
            .satisfies({ ex ->
                val bex = ex as BadRequestException
                assertThat(bex.code).isEqualTo(ErrorCode.INVALID_CATEGORY.name)
                assertThat(bex.httpStatus).isEqualTo(400)
            })
    }

    @Test
    fun `CVS-004 - 빈 문자열은 INVALID_CATEGORY 400 예외`() {
        assertThatThrownBy { categoryValidationService.normalizeAndValidate("") }
            .isInstanceOf(BadRequestException::class.java)
            .satisfies({ ex ->
                val bex = ex as BadRequestException
                assertThat(bex.code).isEqualTo(ErrorCode.INVALID_CATEGORY.name)
            })
    }

    @Test
    fun `CVS-005 - TRENDING(is_active=false)은 사용자 입력으로 INVALID_CATEGORY 400 예외`() {
        // TRENDING은 market_categories에 존재하지만 is_active=false
        // → activeCodes()에 포함되지 않으므로 사용자 입력으로는 거부
        assertThatThrownBy { categoryValidationService.normalizeAndValidate("TRENDING") }
            .isInstanceOf(BadRequestException::class.java)
            .satisfies({ ex ->
                val bex = ex as BadRequestException
                assertThat(bex.code).isEqualTo(ErrorCode.INVALID_CATEGORY.name)
            })
    }
}
