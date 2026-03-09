package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.repository.MemberRepository
import com.predata.backend.exception.NotFoundException
import com.predata.backend.service.storage.AvatarStorageService
import com.predata.backend.util.authenticatedMemberId
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@Tag(name = "member-social", description = "Social APIs")
@RequestMapping("/api/users/me")
class AvatarUploadController(
    private val avatarStorageService: AvatarStorageService,
    private val memberRepository: MemberRepository,
) {
    data class AvatarResponse(val avatarUrl: String)

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @RequestParam("file") file: MultipartFile,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<AvatarResponse>> {
        val memberId = request.authenticatedMemberId()
        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("Member not found.") }

        // 기존 아바타 삭제 후 신규 저장
        avatarStorageService.delete(memberId)
        val url = avatarStorageService.store(memberId, file)

        member.avatarUrl = url
        memberRepository.save(member)

        return ResponseEntity.ok(ApiEnvelope.ok(AvatarResponse(url)))
    }
}
