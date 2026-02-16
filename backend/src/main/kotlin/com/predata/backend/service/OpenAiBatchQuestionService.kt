package com.predata.backend.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.dto.LlmGeneratedQuestionDto
import com.predata.backend.dto.LlmQuestionGenerationRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class OpenAiBatchQuestionService(
    private val resourceLoader: ResourceLoader,
    @Value("\${openai.api.key:}") private val openAiApiKey: String,
    @Value("\${openai.api.model:gpt-4o-mini}") private val openAiModel: String,
    @Value("\${openai.api.base-url:https://api.openai.com/v1/chat/completions}")
    private val openAiBaseUrl: String
) {
    private val logger = LoggerFactory.getLogger(OpenAiBatchQuestionService::class.java)
    private val objectMapper = ObjectMapper()
    private val restTemplate = RestTemplate()

    fun generateQuestions(batchId: String, request: LlmQuestionGenerationRequest): List<LlmGeneratedQuestionDto> {
        if (openAiApiKey.isBlank()) {
            logger.info("[QuestionGen] OPENAI_API_KEY 미설정 - fallback 생성기 사용")
            return fallbackQuestions(request)
        }

        return try {
            val systemPrompt = loadText("classpath:prompts/auto_question_generation_system_prompt.md")
            val userTemplate = loadText("classpath:prompts/auto_question_generation_user_prompt.md")
            val schemaNode = loadJson("classpath:prompts/auto_question_generation_output_schema.json")

            val userPrompt = userTemplate
                .replace("{{subcategory}}", request.subcategory)
                .replace("{{region}}", request.region)
                .replace("{{targetDate}}", request.targetDate.toString())
                .replace("{{trendSignalsJson}}", objectMapper.writeValueAsString(request.signals))
                .replace("{{breakMinutes}}", request.breakMinutes.toString())
                .replace("{{votingHours}}", request.minVotingHours.toString())
                .replace("{{bettingHours}}", request.bettingHours.toString())

            val payload = mapOf(
                "model" to openAiModel,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to userPrompt)
                ),
                "response_format" to mapOf(
                    "type" to "json_schema",
                    "json_schema" to mapOf(
                        "name" to "auto_question_generation_output",
                        "strict" to true,
                        "schema" to schemaNode
                    )
                ),
                "temperature" to 0.3
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(openAiApiKey)
            }

            val entity = HttpEntity(objectMapper.writeValueAsString(payload), headers)
            val response = restTemplate.exchange(openAiBaseUrl, HttpMethod.POST, entity, String::class.java)
            val body = response.body.orEmpty()
            val root = objectMapper.readTree(body)

            val rawContent = extractAssistantContent(root)
            if (rawContent.isBlank()) {
                logger.warn("[QuestionGen] OpenAI 응답이 비어 있어 fallback 사용")
                return fallbackQuestions(request)
            }

            val parsed = objectMapper.readTree(rawContent)
            val questionsNode = parsed.path("questions")
            if (!questionsNode.isArray) {
                logger.warn("[QuestionGen] OpenAI JSON schema 응답 파싱 실패 - questions 배열 없음")
                return fallbackQuestions(request)
            }

            val generated = questionsNode.map { node -> toLlmDto(node) }
            if (generated.isEmpty()) {
                logger.warn("[QuestionGen] OpenAI 생성 질문이 0건 - fallback 사용")
                return fallbackQuestions(request)
            }

            logger.info("[QuestionGen] OpenAI 생성 완료 - batchId={}, subcategory={}, count={}", batchId, request.subcategory, generated.size)
            generated
        } catch (e: Exception) {
            logger.error("[QuestionGen] OpenAI 호출 실패 - fallback 전환: {}", e.message)
            fallbackQuestions(request)
        }
    }

    private fun toLlmDto(node: JsonNode): LlmGeneratedQuestionDto {
        val resolveAtRaw = node.path("resolveAt").asText("")
        return LlmGeneratedQuestionDto(
            title = node.path("title").asText(""),
            marketType = node.path("marketType").asText("VERIFIABLE"),
            questionType = node.path("questionType").asText(node.path("marketType").asText("VERIFIABLE")),
            voteResultSettlement = node.path("voteResultSettlement").asBoolean(false),
            resolutionRule = node.path("resolutionRule").asText("기본 정산 규칙"),
            resolutionSource = node.path("resolutionSource").takeIf { !it.isNull }?.asText(),
            resolveAt = parseResolveAt(resolveAtRaw),
            confidence = node.path("confidence").asDouble(0.5),
            rationale = node.path("rationale").asText("자동 생성된 질문"),
            references = node.path("references").takeIf { it.isArray }?.map { it.asText() } ?: emptyList()
        )
    }

    private fun parseResolveAt(raw: String?): LocalDateTime {
        if (raw.isNullOrBlank()) return LocalDateTime.now().plusDays(2)

        return runCatching { LocalDateTime.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toLocalDateTime() }
            .recoverCatching { Instant.parse(raw).atOffset(ZoneOffset.UTC).toLocalDateTime() }
            .recoverCatching { LocalDate.parse(raw).atStartOfDay() }
            .getOrElse {
                logger.warn("[QuestionGen] resolveAt 파싱 실패(raw={}), 기본값 사용", raw)
                LocalDateTime.now().plusDays(2)
            }
    }

    private fun extractAssistantContent(root: JsonNode): String {
        val contentNode = root.path("choices").firstOrNull()?.path("message")?.path("content")
        if (contentNode == null || contentNode.isMissingNode) return ""

        return when {
            contentNode.isTextual -> contentNode.asText()
            contentNode.isArray -> {
                // 일부 모델은 content를 배열로 반환
                val firstText = contentNode.firstOrNull()?.path("text")?.asText()
                firstText ?: ""
            }
            else -> ""
        }
    }

    private fun loadText(path: String): String {
        val resource = resourceLoader.getResource(path)
        return resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun loadJson(path: String): JsonNode {
        val resource = resourceLoader.getResource(path)
        resource.inputStream.use { input ->
            return objectMapper.readTree(input)
        }
    }

    private fun fallbackQuestions(request: LlmQuestionGenerationRequest): List<LlmGeneratedQuestionDto> {
        val keywords = request.signals.map { it.keyword }.ifEmpty {
            listOf("시장 심리", "주요 지표", "핵심 이슈")
        }

        val k1 = keywords.getOrElse(0) { "주요 지표" }
        val k2 = keywords.getOrElse(1) { "시장 변동성" }
        val k3 = keywords.getOrElse(2) { "정책 이슈" }

        val resolveAt = LocalDateTime.now().plusHours(request.minResolveHours.toLong() + 4)

        return listOf(
            LlmGeneratedQuestionDto(
                title = "$k1 관련 공식 지표가 다음 마감 시점에 기준치를 상회할까요?",
                marketType = "VERIFIABLE",
                questionType = "VERIFIABLE",
                voteResultSettlement = false,
                resolutionRule = "공식 공개 지표가 기준치 이상이면 YES, 미만이면 NO",
                resolutionSource = "https://trends.google.com",
                resolveAt = resolveAt,
                confidence = 0.72,
                rationale = "검색 트렌드 급등 신호 기반",
                references = listOf("https://trends.google.com")
            ),
            LlmGeneratedQuestionDto(
                title = "$k2 이슈가 마감 시점까지 추가 상승 흐름을 보일까요?",
                marketType = "VERIFIABLE",
                questionType = "VERIFIABLE",
                voteResultSettlement = false,
                resolutionRule = "사전 정의된 외부 데이터 소스에서 상승 조건 충족 시 YES",
                resolutionSource = "https://trends.google.com",
                resolveAt = resolveAt.plusHours(1),
                confidence = 0.68,
                rationale = "동일 카테고리 내 연관 키워드 상관 신호",
                references = listOf("https://trends.google.com")
            ),
            LlmGeneratedQuestionDto(
                title = "시장은 $k3 관련 이슈가 이번 라운드에서 확대될 거라고 생각할까요?",
                marketType = "OPINION",
                questionType = "OPINION",
                voteResultSettlement = true,
                resolutionRule = "투표 Reveal 결과에서 YES 득표가 NO 득표보다 많으면 YES",
                resolutionSource = null,
                resolveAt = resolveAt.plusHours(2),
                confidence = 0.60,
                rationale = "의견형 시장 질문 1개 고정 규칙",
                references = emptyList()
            )
        )
    }

    private fun JsonNode.firstOrNull(): JsonNode? {
        return if (isArray && size() > 0) this[0] else null
    }
}
