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
    @Value("\${anthropic.api.key:}") private val apiKey: String,
    @Value("\${gemini.api.key:}") private val geminiApiKey: String
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
        const val SETTING_AUTO_ENABLED = "question_generator_auto_enabled"
        const val SETTING_REGION = "question_generator_region"
        const val SETTING_DAILY_COUNT = "question_generator_daily_count"
        const val SETTING_OPINION_COUNT = "question_generator_opinion_count"
        const val SETTING_VOTING_HOURS = "question_generator_voting_hours"
        const val SETTING_BETTING_HOURS = "question_generator_betting_hours"
        const val SETTING_BREAK_MINUTES = "question_generator_break_minutes"
        const val SETTING_REVEAL_MINUTES = "question_generator_reveal_minutes"

        const val INITIAL_LIQUIDITY = 500L
        const val DEFAULT_VOTING_DURATION = 3600L
        const val DEFAULT_BETTING_DURATION = 3600L

        val VALID_CATEGORIES = listOf("ECONOMY", "TECH", "SPORTS", "POLITICS", "CULTURE", "CRYPTO")

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
            ),
            "CRYPTO" to listOf(
                "비트코인이 이번 주 안에 150,000달러를 돌파할까?",
                "이더리움이 이번 라운드 마감 전 5% 이상 상승할까?",
                "솔라나 네트워크 일일 활성 지갑 수가 이번 주 최고치를 경신할까?"
            )
        )
    }

    // ===== 설정 관리 =====

    @Transactional(readOnly = true)
    fun getSettings(): QuestionGeneratorSettingsResponse {
        val enabled = getSetting(SETTING_ENABLED, "false").toBoolean()
        val interval = getSetting(SETTING_INTERVAL, "3600").toLong()
        val categories = getSetting(SETTING_CATEGORIES, "ECONOMY,TECH,SPORTS,POLITICS,CULTURE,CRYPTO")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val lastGenerated = getSetting(SETTING_LAST_GENERATED, null)
        val region = getSetting(SETTING_REGION, "US")
        val dailyCount = getSetting(SETTING_DAILY_COUNT, "3").toIntOrNull() ?: 3
        val opinionCount = getSetting(SETTING_OPINION_COUNT, "1").toIntOrNull() ?: 1
        val votingHours = getSetting(SETTING_VOTING_HOURS, "24").toIntOrNull() ?: 24
        val bettingHours = getSetting(SETTING_BETTING_HOURS, "24").toIntOrNull() ?: 24
        val breakMinutes = getSetting(SETTING_BREAK_MINUTES, "30").toIntOrNull() ?: 30
        val revealMinutes = getSetting(SETTING_REVEAL_MINUTES, "30").toIntOrNull() ?: 30

        return QuestionGeneratorSettingsResponse(
            enabled = enabled,
            intervalSeconds = interval,
            categories = categories,
            region = region,
            dailyCount = dailyCount,
            opinionCount = opinionCount,
            votingHours = votingHours,
            bettingHours = bettingHours,
            breakMinutes = breakMinutes,
            revealMinutes = revealMinutes,
            lastGeneratedAt = lastGenerated,
            isDemoMode = isDemoMode()
        )
    }

    @Transactional
    fun updateSettings(request: UpdateQuestionGeneratorSettingsRequest): QuestionGeneratorSettingsResponse {
        // 설정값 교차 검증: opinionCount <= dailyCount
        val currentDailyCount = getSetting(SETTING_DAILY_COUNT, "3").toIntOrNull() ?: 3
        val currentOpinionCount = getSetting(SETTING_OPINION_COUNT, "1").toIntOrNull() ?: 1

        val newDailyCount = request.dailyCount ?: currentDailyCount
        val newOpinionCount = request.opinionCount ?: currentOpinionCount

        if (newDailyCount < 1) {
            throw IllegalArgumentException("dailyCount must be at least 1")
        }

        if (newOpinionCount > newDailyCount) {
            throw IllegalArgumentException("opinionCount must not exceed dailyCount (opinionCount: $newOpinionCount, dailyCount: $newDailyCount)")
        }

        request.enabled?.let {
            saveSetting(SETTING_ENABLED, it.toString())
            saveSetting(SETTING_AUTO_ENABLED, it.toString()) // 신규 배치 생성기 토글과 동기화
        }
        request.intervalSeconds?.let {
            saveSetting(SETTING_INTERVAL, it.toString())
        }
        request.categories?.let { categories ->
            val validCategories = categories.filter { it.uppercase() in VALID_CATEGORIES }
            if (validCategories.isEmpty()) {
                throw IllegalArgumentException("No valid categories. Allowed: $VALID_CATEGORIES")
            }
            saveSetting(SETTING_CATEGORIES, validCategories.joinToString(",") { it.uppercase() })
        }
        request.region?.let {
            saveSetting(SETTING_REGION, it.uppercase())
        }
        request.dailyCount?.let {
            saveSetting(SETTING_DAILY_COUNT, it.toString())
        }
        request.opinionCount?.let {
            saveSetting(SETTING_OPINION_COUNT, it.toString())
        }
        request.votingHours?.let {
            saveSetting(SETTING_VOTING_HOURS, it.toString())
        }
        request.bettingHours?.let {
            saveSetting(SETTING_BETTING_HOURS, it.toString())
        }
        request.breakMinutes?.let {
            saveSetting(SETTING_BREAK_MINUTES, it.toString())
        }
        request.revealMinutes?.let {
            saveSetting(SETTING_REVEAL_MINUTES, it.toString())
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
        return getSetting(SETTING_CATEGORIES, "ECONOMY,TECH,SPORTS,POLITICS,CULTURE,CRYPTO")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun isDemoMode(): Boolean = apiKey.isBlank() && geminiApiKey.isBlank()

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
        logger.warn("[QuestionGenerator] Legacy generator is disabled. Use AutoQuestionGenerationService.generateDailyTrendQuestion().")
        return QuestionGenerationResponse(
            success = false,
            message = "Legacy generator is disabled. Use the daily trend generator."
        )
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
            logger.error("[QuestionGenerator] Claude API call failed: ${e.message}")
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
            "CRYPTO" -> "암호화폐, 블록체인, 비트코인, 이더리움, 온체인 데이터, 디파이 등"
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
            marketType = com.predata.backend.domain.MarketType.OPINION,
            resolutionRule = "투표 Reveal 결과에서 YES 득표가 NO 득표보다 많으면 YES",
            voteResultSettlement = true,
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
