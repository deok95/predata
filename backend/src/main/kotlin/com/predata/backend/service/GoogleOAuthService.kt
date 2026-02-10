package com.predata.backend.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.predata.backend.config.JwtUtil
import com.predata.backend.domain.Member
import com.predata.backend.dto.GoogleAuthRequest
import com.predata.backend.dto.GoogleAuthResponse
import com.predata.backend.dto.CompleteGoogleRegistrationRequest
import com.predata.backend.repository.MemberRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GoogleOAuthService(
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil,
    @Value("\${google.oauth.client-id}") private val googleClientId: String
) {
    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
        .setAudience(listOf(googleClientId))
        .build()

    fun authenticate(request: GoogleAuthRequest): GoogleAuthResponse {
        try {
            // 1. Google ID Token 검증
            val idToken = verifier.verify(request.googleToken)
                ?: return GoogleAuthResponse(
                    success = false,
                    message = "Invalid Google token"
                )

            val payload = idToken.payload
            val googleId = payload.subject  // Google User ID (sub claim)
            val email = payload.email.lowercase()

            // 2. 기존 Google 사용자 확인
            val existingByGoogleId = memberRepository.findByGoogleId(googleId)
            if (existingByGoogleId.isPresent) {
                // 기존 Google 사용자 → 자동 로그인
                val member = existingByGoogleId.get()
                val jwt = jwtUtil.generateToken(
                    member.id!!,
                    member.email,
                    member.role
                )
                return GoogleAuthResponse(
                    success = true,
                    message = "Login successful",
                    token = jwt,
                    memberId = member.id
                )
            }

            // 3. 이메일로 기존 계정 확인 (계정 연동)
            val existingByEmail = memberRepository.findByEmail(email)
            if (existingByEmail.isPresent) {
                // 기존 이메일 사용자 → Google ID 연결
                val member = existingByEmail.get()
                member.googleId = googleId
                memberRepository.save(member)

                val jwt = jwtUtil.generateToken(
                    member.id!!,
                    member.email,
                    member.role
                )
                return GoogleAuthResponse(
                    success = true,
                    message = "Google account linked successfully",
                    token = jwt,
                    memberId = member.id
                )
            }

            // 4. 신규 사용자 → 추가 정보 필요 여부 확인
            if (request.countryCode == null) {
                return GoogleAuthResponse(
                    success = true,
                    message = "Additional information required",
                    needsAdditionalInfo = true
                )
            }

            // 5. 신규 회원 생성
            val newMember = Member(
                email = email,
                googleId = googleId,
                passwordHash = null,  // Google 사용자는 비밀번호 없음
                countryCode = request.countryCode,
                jobCategory = request.jobCategory,
                ageGroup = request.ageGroup,
                tier = "BRONZE",
                pointBalance = 10000,  // 가입 보너스
                role = "USER"
            )
            val saved = memberRepository.save(newMember)

            val jwt = jwtUtil.generateToken(saved.id!!, saved.email, saved.role)
            return GoogleAuthResponse(
                success = true,
                message = "Signup successful",
                token = jwt,
                memberId = saved.id
            )
        } catch (e: Exception) {
            return GoogleAuthResponse(
                success = false,
                message = "Google authentication failed: ${e.message}"
            )
        }
    }

    fun completeRegistration(request: CompleteGoogleRegistrationRequest): GoogleAuthResponse {
        try {
            val email = request.email.lowercase()

            // 1. 이미 등록된 사용자인지 확인
            val existingByGoogleId = memberRepository.findByGoogleId(request.googleId)
            if (existingByGoogleId.isPresent) {
                return GoogleAuthResponse(
                    success = false,
                    message = "User already registered"
                )
            }

            val existingByEmail = memberRepository.findByEmail(email)
            if (existingByEmail.isPresent) {
                // 기존 이메일 사용자 → Google ID 연결
                val member = existingByEmail.get()
                member.googleId = request.googleId
                memberRepository.save(member)

                val jwt = jwtUtil.generateToken(member.id!!, member.email, member.role)
                return GoogleAuthResponse(
                    success = true,
                    message = "Google account linked successfully",
                    token = jwt,
                    memberId = member.id
                )
            }

            // 2. 신규 회원 생성
            val newMember = Member(
                email = email,
                googleId = request.googleId,
                passwordHash = null,  // Google 사용자는 비밀번호 없음
                countryCode = request.countryCode,
                jobCategory = request.jobCategory,
                ageGroup = request.ageGroup,
                tier = "BRONZE",
                pointBalance = 10000,  // 가입 보너스
                role = "USER"
            )
            val saved = memberRepository.save(newMember)

            val jwt = jwtUtil.generateToken(saved.id!!, saved.email, saved.role)
            return GoogleAuthResponse(
                success = true,
                message = "Registration successful",
                token = jwt,
                memberId = saved.id
            )
        } catch (e: Exception) {
            return GoogleAuthResponse(
                success = false,
                message = "Registration failed: ${e.message}"
            )
        }
    }
}
