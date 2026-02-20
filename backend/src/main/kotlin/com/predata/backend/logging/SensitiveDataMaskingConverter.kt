package com.predata.backend.logging

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * 로그 메시지에서 민감값 패턴을 탐지하여 [REDACTED] 로 대체한다.
 *
 * 대상 패턴: secret, password, token, key, private-key (대소문자 무관, = 또는 : 구분자)
 */
class SensitiveDataMaskingConverter : MessageConverter() {

    override fun convert(event: ILoggingEvent): String =
        SENSITIVE_PATTERN.replace(event.formattedMessage, "$1[REDACTED]")

    companion object {
        private val SENSITIVE_PATTERN = Regex(
            """(?i)((?:secret|password|token|(?:private[-_]?)?key)\s*[=:]\s*)\S+"""
        )
    }
}
