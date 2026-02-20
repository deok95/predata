package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.dto.CompleteGoogleRegistrationRequest
import com.predata.backend.dto.CompleteSignupRequest
import com.predata.backend.dto.GoogleAuthRequest
import com.predata.backend.dto.GoogleAuthResponse
import com.predata.backend.dto.LoginRequest
import com.predata.backend.dto.SendCodeRequest
import com.predata.backend.dto.VerifyCodeRequest
import com.predata.backend.service.AuthService
import com.predata.backend.service.GoogleOAuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val googleOAuthService: GoogleOAuthService
) {

    /**
     * Step 1: 이메일로 인증 코드 발송
     * POST /api/auth/send-code
     */
    @PostMapping("/send-code")
    fun sendCode(@RequestBody request: SendCodeRequest): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        if (request.email.isBlank() || !request.email.contains("@")) {
            throw IllegalArgumentException("Please enter a valid email address.")
        }
        val result = authService.sendVerificationCode(request.email)
        if (result["success"] != true) {
            throw IllegalArgumentException(result["message"] as? String ?: "Failed to send verification code.")
        }
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * Step 2: 인증 코드 검증 (회원 생성 안 함)
     * POST /api/auth/verify-code
     */
    @PostMapping("/verify-code")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        if (request.email.isBlank() || request.code.isBlank()) {
            throw IllegalArgumentException("Please enter email and verification code.")
        }
        val result = authService.verifyCode(request.email, request.code)
        if (result["success"] != true) {
            throw IllegalArgumentException(result["message"] as? String ?: "Verification failed.")
        }
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * Step 3: 비밀번호 설정 및 회원 생성
     * POST /api/auth/complete-signup
     */
    @PostMapping("/complete-signup")
    fun completeSignup(@RequestBody request: CompleteSignupRequest): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        if (request.email.isBlank() || request.code.isBlank()) {
            throw IllegalArgumentException("Please enter email and verification code.")
        }
        if (request.password.isBlank() || request.password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters.")
        }
        if (request.password != request.passwordConfirm) {
            throw IllegalArgumentException("Passwords do not match.")
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
        if (result["success"] != true) {
            throw IllegalArgumentException(result["message"] as? String ?: "Signup failed.")
        }
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 로그인
     * POST /api/auth/login
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        if (request.email.isBlank() || request.password.isBlank()) {
            throw IllegalArgumentException("Please enter email and password.")
        }
        val result = authService.login(request.email, request.password)
        if (result["success"] != true) {
            throw IllegalArgumentException(result["message"] as? String ?: "Login failed.")
        }
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * Google OAuth 로그인
     * POST /api/auth/google
     */
    @PostMapping("/google")
    fun googleLogin(@RequestBody request: GoogleAuthRequest): ResponseEntity<ApiEnvelope<GoogleAuthResponse>> {
        if (request.googleToken.isBlank()) {
            throw IllegalArgumentException("Google token is required")
        }
        val response = googleOAuthService.authenticate(request)
        if (!response.success) {
            throw IllegalArgumentException(response.message ?: "Google authentication failed.")
        }
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * Google OAuth 회원가입 완료 (추가 정보 입력)
     * POST /api/auth/google/complete-registration
     */
    @PostMapping("/google/complete-registration")
    fun completeGoogleRegistration(@RequestBody request: CompleteGoogleRegistrationRequest): ResponseEntity<ApiEnvelope<GoogleAuthResponse>> {
        if (request.googleId.isBlank() || request.email.isBlank() || request.countryCode.isBlank()) {
            throw IllegalArgumentException("Required fields are missing")
        }
        val response = googleOAuthService.completeRegistration(request)
        if (!response.success) {
            throw IllegalArgumentException(response.message ?: "Registration failed.")
        }
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }
}
