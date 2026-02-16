package com.predata.backend.service

import com.predata.backend.domain.SystemSettings
import com.predata.backend.repository.SystemSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QuestionGenerationSettingsService(
    private val systemSettingsRepository: SystemSettingsRepository
) {

    companion object {
        const val SETTING_AUTO_ENABLED = "question_generator_auto_enabled"
        const val SETTING_SUBCATEGORIES = "question_generator_subcategories"
        const val SETTING_REGION = "question_generator_region"
        const val SETTING_DAILY_COUNT = "question_generator_daily_count"
        const val SETTING_OPINION_COUNT = "question_generator_opinion_count"
        const val SETTING_VOTING_HOURS = "question_generator_voting_hours"
        const val SETTING_BETTING_HOURS = "question_generator_betting_hours"
        const val SETTING_BREAK_MINUTES = "question_generator_break_minutes"
        const val SETTING_REVEAL_MINUTES = "question_generator_reveal_minutes"
        const val SETTING_DUPLICATE_THRESHOLD = "question_generator_duplicate_threshold"
    }

    @Transactional(readOnly = true)
    fun get(): QuestionGenerationSettings {
        return QuestionGenerationSettings(
            autoEnabled = getBoolean(SETTING_AUTO_ENABLED, false),
            subcategories = getString(SETTING_SUBCATEGORIES, "ECONOMY,TECH,SPORTS,POLITICS,CULTURE,CRYPTO")
                .split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() },
            region = getString(SETTING_REGION, "US"),
            dailyCount = getInt(SETTING_DAILY_COUNT, 3),
            opinionCount = getInt(SETTING_OPINION_COUNT, 1),
            votingHours = getInt(SETTING_VOTING_HOURS, 24),
            bettingHours = getInt(SETTING_BETTING_HOURS, 24),
            breakMinutes = getInt(SETTING_BREAK_MINUTES, 30),
            revealMinutes = getInt(SETTING_REVEAL_MINUTES, 30),
            duplicateThreshold = getDouble(SETTING_DUPLICATE_THRESHOLD, 0.90)
        )
    }

    private fun getString(key: String, default: String): String {
        return systemSettingsRepository.findByKey(key)?.value ?: default
    }

    private fun getInt(key: String, default: Int): Int {
        return systemSettingsRepository.findByKey(key)?.value?.toIntOrNull() ?: default
    }

    private fun getDouble(key: String, default: Double): Double {
        return systemSettingsRepository.findByKey(key)?.value?.toDoubleOrNull() ?: default
    }

    private fun getBoolean(key: String, default: Boolean): Boolean {
        return systemSettingsRepository.findByKey(key)?.value?.toBooleanStrictOrNull() ?: default
    }

    @Transactional
    fun saveDefaultsIfMissing() {
        val defaults = mapOf(
            SETTING_AUTO_ENABLED to "false",
            SETTING_SUBCATEGORIES to "ECONOMY,TECH,SPORTS,POLITICS,CULTURE,CRYPTO",
            SETTING_REGION to "US",
            SETTING_DAILY_COUNT to "3",
            SETTING_OPINION_COUNT to "1",
            SETTING_VOTING_HOURS to "24",
            SETTING_BETTING_HOURS to "24",
            SETTING_BREAK_MINUTES to "30",
            SETTING_REVEAL_MINUTES to "30",
            SETTING_DUPLICATE_THRESHOLD to "0.90"
        )

        defaults.forEach { (key, value) ->
            if (systemSettingsRepository.findByKey(key) == null) {
                systemSettingsRepository.save(
                    SystemSettings(
                        key = key,
                        value = value,
                        updatedAt = LocalDateTime.now()
                    )
                )
            }
        }
    }
}

data class QuestionGenerationSettings(
    val autoEnabled: Boolean,
    val subcategories: List<String>,
    val region: String,
    val dailyCount: Int,
    val opinionCount: Int,
    val votingHours: Int,
    val bettingHours: Int,
    val breakMinutes: Int,
    val revealMinutes: Int,
    val duplicateThreshold: Double
)
