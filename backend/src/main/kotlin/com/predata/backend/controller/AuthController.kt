package com.predata.backend.controller

import com.predata.backend.dto.SendCodeRequest
import com.predata.backend.dto.VerifyCodeRequest
import com.predata.backend.dto.CompleteSignupRequest
import com.predata.backend.dto.LoginRequest
import com.predata.backend.dto.GoogleAuthRequest
import com.predata.backend.dto.GoogleAuthResponse
import com.predata.backend.dto.CompleteGoogleRegistrationRequest
import com.predata.backend.service.AuthService
import com.predata.backend.service.GoogleOAuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class AuthController(
    private val authService: AuthService,
    private val googleOAuthService: GoogleOAuthService
) {

    /**
     * Step 1: 이메일로 인증 코드 발송
     * POST /api/auth/send-code
     */
    @PostMapping("/send-code")
    fun sendCode(@RequestBody request: SendCodeRequest): ResponseEntity<Any> {
        if (request.email.isBlank() || !request.email.contains("@")) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "Please enter a valid email address.")
            )
        }
        val result = authService.sendVerificationCode(request.email)
        return ResponseEntity.ok(result)
    }

    /**
     * Step 2: 인증 코드 검증 (회원 생성 안 함)
     * POST /api/auth/verify-code
     */
    @PostMapping("/verify-code")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<Any> {
        if (request.email.isBlank() || request.code.isBlank()) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "Please enter email and verification code.")
            )
        }
        val result = authService.verifyCode(request.email, request.code)
        return if (result["success"] == true) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Step 3: 비밀번호 설정 및 회원 생성
     * POST /api/auth/complete-signup
     */
    @PostMapping("/complete-signup")
    fun completeSignup(@RequestBody request: CompleteSignupRequest): ResponseEntity<Any> {
        if (request.email.isBlank() || request.code.isBlank()) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "Please enter email and verification code.")
            )
        }
        if (request.password.isBlank() || request.password.length < 6) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "Password must be at least 6 characters.")
            )
        }
        if (request.password != request.passwordConfirm) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "Passwords do not match.")
            )
        }
        val result = authService.completeSignup(
            request.email,
            request.code,
            request.password,
            request.countryCode,
            request.gender,
            request.birthDate,
            request.jobCategory,
            request.ageGroup
        )
        return if (result["success"] == true) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * 로그인
     * POST /api/auth/login
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        if (request.email.isBlank() || request.password.isBlank()) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "Please enter email and password.")
            )
        }
        val result = authService.login(request.email, request.password)
        return if (result["success"] == true) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Google OAuth 로그인
     * POST /api/auth/google
     */
    @PostMapping("/google")
    fun googleLogin(@RequestBody request: GoogleAuthRequest): ResponseEntity<GoogleAuthResponse> {
        if (request.googleToken.isBlank()) {
            return ResponseEntity.badRequest().body(
                GoogleAuthResponse(
                    success = false,
                    message = "Google token is required"
                )
            )
        }

        val response = googleOAuthService.authenticate(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    /**
     * Google OAuth 회원가입 완료 (추가 정보 입력)
     * POST /api/auth/google/complete-registration
     */
    @PostMapping("/google/complete-registration")
    fun completeGoogleRegistration(@RequestBody request: CompleteGoogleRegistrationRequest): ResponseEntity<GoogleAuthResponse> {
        if (request.googleId.isBlank() || request.email.isBlank() || request.countryCode.isBlank()) {
            return ResponseEntity.badRequest().body(
                GoogleAuthResponse(
                    success = false,
                    message = "Required fields are missing"
                )
            )
        }

        val response = googleOAuthService.completeRegistration(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
}
