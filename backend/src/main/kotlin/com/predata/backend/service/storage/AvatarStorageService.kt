package com.predata.backend.service.storage

import org.springframework.web.multipart.MultipartFile

/**
 * 프로필 아바타 저장소 추상화.
 * local 프로필: LocalAvatarStorageService (Mac Mini 로컬 디스크)
 * prod/aws 전환 시: S3AvatarStorageService 구현체 추가 후 프로필 교체만으로 마이그레이션.
 */
interface AvatarStorageService {
    /** 파일 저장 후 공개 접근 가능한 URL 반환 */
    fun store(memberId: Long, file: MultipartFile): String

    /** 기존 아바타 파일 삭제 (없으면 무시) */
    fun delete(memberId: Long)
}
