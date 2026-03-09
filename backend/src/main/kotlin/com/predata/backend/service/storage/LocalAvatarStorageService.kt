package com.predata.backend.service.storage

import com.predata.backend.config.properties.SystemProperties
import com.predata.backend.exception.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * 로컬 디스크 기반 아바타 저장소.
 * 저장 경로: {uploadDir}/avatars/{memberId}.{ext}
 * 공개 URL: {apiBaseUrl}/uploads/avatars/{memberId}.{ext}
 *
 * AWS 전환 시 이 클래스를 S3AvatarStorageService로 교체.
 */
@Service
class LocalAvatarStorageService(
    private val systemProperties: SystemProperties,
) : AvatarStorageService {

    private val logger = LoggerFactory.getLogger(LocalAvatarStorageService::class.java)

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    }

    override fun store(memberId: Long, file: MultipartFile): String {
        validateFile(file)

        val ext = resolveExtension(file)
        val avatarDir = resolveAvatarDir()
        val target: Path = avatarDir.resolve("$memberId.$ext")

        Files.copy(file.inputStream, target, StandardCopyOption.REPLACE_EXISTING)
        logger.info("Avatar saved: memberId=$memberId, path=$target")

        return "${systemProperties.apiBaseUrl}/uploads/avatars/$memberId.$ext"
    }

    override fun delete(memberId: Long) {
        val avatarDir = resolveAvatarDir()
        ALLOWED_EXTENSIONS.forEach { ext ->
            val path = avatarDir.resolve("$memberId.$ext")
            if (Files.deleteIfExists(path)) {
                logger.info("Avatar deleted: memberId=$memberId, path=$path")
            }
        }
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) throw BadRequestException("File is empty.")
        if (file.size > MAX_FILE_SIZE) throw BadRequestException("File size exceeds 5MB limit.")

        val contentType = file.contentType?.lowercase() ?: ""
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BadRequestException("Only JPEG, PNG, WEBP, GIF images are allowed.")
        }
    }

    private fun resolveExtension(file: MultipartFile): String {
        val name = file.originalFilename ?: ""
        val ext = name.substringAfterLast('.', "").lowercase()
        return if (ext in ALLOWED_EXTENSIONS) ext else "jpg"
    }

    private fun resolveAvatarDir(): Path {
        val dir: Path = Paths.get(systemProperties.uploadDir, "avatars")
        if (!Files.exists(dir)) Files.createDirectories(dir)
        return dir
    }
}
