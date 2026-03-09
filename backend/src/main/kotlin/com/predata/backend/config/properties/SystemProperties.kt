package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * 시스템 계정 및 파일 업로드 설정
 * - predataAdminMemberId: 어드민 생성 질문의 소유자로 표시될 predata 공식 계정 ID
 * - uploadDir: 업로드 파일 저장 루트 디렉토리
 * - apiBaseUrl: 업로드 파일 공개 접근 베이스 URL (avatarUrl 생성에 사용)
 */
@ConfigurationProperties(prefix = "app.system")
@Validated
data class SystemProperties(
    val predataAdminMemberId: Long = 1L,
    val uploadDir: String = "./uploads",
    val apiBaseUrl: String = "http://localhost:8080",
)
