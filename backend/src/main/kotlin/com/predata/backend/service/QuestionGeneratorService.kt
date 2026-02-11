package com.predata.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.domain.SystemSettings
import com.predata.backend.dto.*
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.SystemSettingsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class QuestionGeneratorService(
    private val systemSettingsRepository: SystemSettingsRepository,
    private val questionRepository: QuestionRepository,
    private val blockchainService: BlockchainService,
    @Value("\${anthropic.api.key:}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(QuestionGeneratorService::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    companion object {
        const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
        const val CLAUDE_MODEL = "claude-sonnet-4-20250514"
        const val ANTHROPIC_VERSION = "2023-06-01"

        const val SETTING_ENABLED = "question_generator_enabled"
        const val SETTING_INTERVAL = "question_generator_interval"
        const val SETTING_CATEGORIES = "question_generator_categories"
        const val SETTING_LAST_GENERATED = "question_generator_last_generated"

        const val INITIAL_LIQUIDITY = 500L
        const val DEFAULT_VOTING_DURATION = 3600L
        const val DEFAULT_BETTING_DURATION = 3600L

        val VALID_CATEGORIES = listOf("ECONOMY", "TECH", "SPORTS", "POLITICS", "CULTURE")

        // 데모 모드용 샘플 질문
        val DEMO_QUESTIONS = mapOf(
            "ECONOMY" to listOf(
                "2026년 하반기에 미국 금리가 인하될까?",
                "비트코인이 연말까지 15만 달러를 돌파할까?",
                "한국 코스피가 3000을 넘길까?"
            ),
            "TECH" to listOf(
                "애플이 2026년 안에 폴더블 아이폰을 출시할까?",
                "OpenAI가 GPT-5를 올해 출시할까?",
                "테슬라 로보택시가 2026년에 상용화될까?"
            ),
            "SPORTS" to listOf(
                "손흥민이 이번 시즌 20골 이상 넣을까?",
                "한국이 2026 월드컵 16강에 진출할까?",
                "LA 다저스가 2026년 월드시리즈 우승할까?"
            ),
            "POLITICS" to listOf(
                "2026년 한국 지방선거에서 여당이 승리할까?",
                "미국이 2026년에 새로운 대중국 관세를 부과할까?"
            ),
            "CULTURE" to listOf(
                "넷플릭스 오징어게임3이 글로벌 1위를 할까?",
                "BTS가 2026년에 완전체 컴백할까?"
            )
        )
    }

    // ===== 설정 관리 =====

    @Transactional(readOnly = true)
    fun getSettings(): QuestionGeneratorSettingsResponse {
        val enabled = getSetting(SETTING_ENABLED, "false").toBoolean()
        val interval = getSetting(SETTING_INTERVAL, "3600").toLong()
        val categories = getSetting(SETTING_CATEGORIES, "ECONOMY,TECH,SPORTS,POLITICS,CULTURE")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val lastGenerated = getSetting(SETTING_LAST_GENERATED, null)

        return QuestionGeneratorSettingsResponse(
            enabled = enabled,
            intervalSeconds = interval,
            categories = categories,
            lastGeneratedAt = lastGenerated,
            isDemoMode = isDemoMode()
        )
    }

    @Transactional
    fun updateSettings(request: UpdateQuestionGeneratorSettingsRequest): QuestionGeneratorSettingsResponse {
        request.enabled?.let {
            saveSetting(SETTING_ENABLED, it.toString())
        }
        request.intervalSeconds?.let {
            saveSetting(SETTING_INTERVAL, it.toString())
        }
        request.categories?.let { categories ->
            val validCategories = categories.filter { it.uppercase() in VALID_CATEGORIES }
            if (validCategories.isEmpty()) {
                throw IllegalArgumentException("유효한 카테고리가 없습니다. 허용: $VALID_CATEGORIES")
            }
            saveSetting(SETTING_CATEGORIES, validCategories.joinToString(",") { it.uppercase() })
        }

        return getSettings()
    }

    fun isEnabled(): Boolean {
        return getSetting(SETTING_ENABLED, "false").toBoolean()
    }

    fun getIntervalSeconds(): Long {
        return getSetting(SETTING_INTERVAL, "3600").toLong()
    }

    fun getCategories(): List<String> {
        return getSetting(SETTING_CATEGORIES, "ECONOMY,TECH,SPORTS,POLITICS,CULTURE")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun isDemoMode(): Boolean = apiKey.isBlank()

    private fun getRandomDemoQuestion(category: String): String {
        val questions = DEMO_QUESTIONS[category] ?: DEMO_QUESTIONS["ECONOMY"]!!
        return questions.random()
    }

    private fun getSetting(key: String, default: String?): String {
        return systemSettingsRepository.findByKey(key)?.value ?: default ?: ""
    }

    private fun saveSetting(key: String, value: String) {
        val setting = systemSettingsRepository.findByKey(key)
        if (setting != null) {
            setting.value = value
            setting.updatedAt = LocalDateTime.now()
            systemSettingsRepository.save(setting)
        } else {
            systemSettingsRepository.save(
                SystemSettings(
                    key = key,
                    value = value,
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }

    // ===== 질문 생성 =====

    @Transactional
    fun generateQuestion(category: String? = null): QuestionGenerationResponse {
        val targetCategory = category?.uppercase()
            ?: getCategories().randomOrNull()
            ?: "ECONOMY"

        if (targetCategory !in VALID_CATEGORIES) {
            return QuestionGenerationResponse(
                success = false,
                message = "유효하지 않은 카테고리입니다: $targetCategory"
            )
        }

        val demoMode = isDemoMode()

        try {
            val generatedTitle: String

            if (demoMode) {
                // 데모 모드: 샘플 질문 사용
                logger.info("[DEMO MODE] API 키가 설정되지 않아 샘플 질문을 사용합니다.")
                generatedTitle = getRandomDemoQuestion(targetCategory)
                logger.info("[DEMO MODE] 샘플 질문 사용: $generatedTitle")
            } else {
                // 프로덕션 모드: Claude API 호출
                val apiResult = callClaudeApi(targetCategory)
                if (apiResult.isNullOrBlank()) {
                    return QuestionGenerationResponse(
                        success = false,
                        message = "질문 생성에 실패했습니다."
                    )
                }
                generatedTitle = apiResult
            }

            val question = createQuestion(generatedTitle, targetCategory)

            saveSetting(SETTING_LAST_GENERATED, LocalDateTime.now().toString())

            val logPrefix = if (demoMode) "[DEMO MODE]" else "[QuestionGenerator]"
            logger.info("$logPrefix 질문 생성 완료: ${question.title} (카테고리: $targetCategory)")

            return QuestionGenerationResponse(
                success = true,
                questionId = question.id,
                title = question.title,
                category = question.category,
                message = if (demoMode) "데모 모드로 질문이 생성되었습니다." else "질문이 성공적으로 생성되었습니다.",
                isDemoMode = demoMode
            )

        } catch (e: Exception) {
            logger.error("[QuestionGenerator] 질문 생성 실패: ${e.message}", e)
            return QuestionGenerationResponse(
                success = false,
                message = "질문 생성 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }

    private fun callClaudeApi(category: String): String? {
        val prompt = buildPrompt(category)

        val request = ClaudeApiRequest(
            model = CLAUDE_MODEL,
            max_tokens = 1024,
            messages = listOf(ClaudeMessage(role = "user", content = prompt))
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-api-key", apiKey)
            set("anthropic-version", ANTHROPIC_VERSION)
        }

        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        try {
            val response = restTemplate.exchange(
                CLAUDE_API_URL,
                HttpMethod.POST,
                entity,
                String::class.java
            )

            val jsonNode = objectMapper.readTree(response.body)
            val content = jsonNode.get("content")?.get(0)?.get("text")?.asText()

            return content?.trim()?.removeSurrounding("\"")

        } catch (e: Exception) {
            logger.error("[QuestionGenerator] Claude API 호출 실패: ${e.message}")
            throw e
        }
    }

    private fun buildPrompt(category: String): String {
        val categoryDescription = when (category) {
            "ECONOMY" -> "경제, 금융, 주식, 암호화폐, 금리, 환율 등"
            "TECH" -> "기술, IT, AI, 스타트업, 신기술 트렌드 등"
            "SPORTS" -> "스포츠, 축구, 야구, 농구, 올림픽, 월드컵 등"
            "POLITICS" -> "정치, 선거, 정책, 국제관계, 외교 등"
            "CULTURE" -> "문화, 연예, 영화, 음악, 예술, 트렌드 등"
            else -> category
        }

        return """
당신은 예측 시장 질문 생성 전문가입니다.
다음 조건에 맞는 한국어 YES/NO 예측 질문을 1개만 생성해주세요:

카테고리: $category ($categoryDescription)

조건:
1. 질문은 반드시 한국어로 작성
2. YES 또는 NO로 명확히 답할 수 있는 형식
3. 현재 시점에서 1시간~24시간 내에 결과를 알 수 있는 주제
4. 구체적이고 측정 가능한 내용
5. 시사적이고 관심을 끌 수 있는 주제
6. 질문만 출력 (부가 설명 없이)

예시 형식:
- "오늘 비트코인 가격이 5만 달러를 돌파할까?"
- "이번 주 코스피 지수가 2500을 넘을까?"
- "오늘 발표되는 고용지표가 예상치를 상회할까?"

질문:
        """.trimIndent()
    }

    private fun createQuestion(title: String, category: String): Question {
        val now = LocalDateTime.now()
        val votingEndAt = now.plusSeconds(DEFAULT_VOTING_DURATION)
        val bettingStartAt = votingEndAt.plusMinutes(5)
        val bettingEndAt = bettingStartAt.plusSeconds(DEFAULT_BETTING_DURATION)

        val question = Question(
            title = title,
            category = category,
            categoryWeight = BigDecimal.ONE,
            status = QuestionStatus.VOTING,
            type = QuestionType.OPINION,
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            totalBetPool = INITIAL_LIQUIDITY * 2,
            yesBetPool = INITIAL_LIQUIDITY,
            noBetPool = INITIAL_LIQUIDITY,
            initialYesPool = INITIAL_LIQUIDITY,
            initialNoPool = INITIAL_LIQUIDITY,
            finalResult = FinalResult.PENDING,
            expiredAt = bettingEndAt,
            createdAt = now
        )

        val savedQuestion = questionRepository.save(question)

        blockchainService.createQuestionOnChain(savedQuestion)

        return savedQuestion
    }
}
