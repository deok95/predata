package com.predata.backend.controller

import com.predata.backend.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class AuthController(
    private val authService: AuthService
) {

    /**
     * 인증 코드 발송
     * POST /api/auth/send-code
     */
    @PostMapping("/send-code")
    fun sendVerificationCode(@RequestBody request: SendCodeRequest): ResponseEntity<Any> {
        if (request.email.isBlank() || !request.email.contains("@")) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "유효한 이메일을 입력해주세요.")
            )
        }
        val result = authService.sendVerificationCode(request.email)
        return ResponseEntity.ok(result)
    }

    /**
     * 인증 코드 검증
     * POST /api/auth/verify-code
     */
    @PostMapping("/verify-code")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<Any> {
        if (request.email.isBlank() || request.code.isBlank()) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "이메일과 인증 코드를 입력해주세요.")
            )
        }
        val result = authService.verifyCode(request.email, request.code)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    data class SendCodeRequest(
        val email: String
    )

    data class VerifyCodeRequest(
        val email: String,
        val code: String
    )
}
