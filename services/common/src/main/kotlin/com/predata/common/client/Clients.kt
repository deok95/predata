package com.predata.common.client

import com.predata.common.dto.*
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

// Member Service Client
@FeignClient(name = "member-service", url = "\${services.member.url:http://localhost:8081}")
interface MemberClient {

    @GetMapping("/api/members/{id}")
    fun getMember(@PathVariable id: Long): ApiResponse<MemberDto>

    @GetMapping("/api/members/by-email")
    fun getMemberByEmail(@RequestParam email: String): ApiResponse<MemberDto>

    @PostMapping("/api/members/{id}/deduct-points")
    fun deductPoints(@PathVariable id: Long, @RequestBody request: PointsRequest): ApiResponse<Unit>

    @PostMapping("/api/members/{id}/add-points")
    fun addPoints(@PathVariable id: Long, @RequestBody request: PointsRequest): ApiResponse<Unit>

    @GetMapping("/api/members/{id}/tickets")
    fun getTickets(@PathVariable id: Long): ApiResponse<Int>
}

// Question Service Client
@FeignClient(name = "question-service", url = "\${services.question.url:http://localhost:8082}")
interface QuestionClient {

    @GetMapping("/api/questions/{id}")
    fun getQuestion(@PathVariable id: Long): ApiResponse<QuestionDto>

    @GetMapping("/api/questions")
    fun getAllQuestions(): ApiResponse<List<QuestionDto>>

    @PostMapping("/api/questions/{id}/update-pool")
    fun updateBetPool(@PathVariable id: Long, @RequestBody request: UpdatePoolRequest): ApiResponse<Unit>

    @GetMapping("/api/questions/{id}/lock")
    fun getQuestionWithLock(@PathVariable id: Long): ApiResponse<QuestionDto>

    @PutMapping("/api/questions/{id}/status")
    fun updateStatus(@PathVariable id: Long, @RequestParam status: String): ApiResponse<Unit>

    @PutMapping("/api/questions/{id}/final-result")
    fun updateFinalResult(@PathVariable id: Long, @RequestBody request: FinalResultRequest): ApiResponse<Unit>
}

// Betting Service Client
@FeignClient(name = "betting-service", url = "\${services.betting.url:http://localhost:8083}")
interface BettingClient {

    @GetMapping("/api/activities/question/{questionId}")
    fun getActivitiesByQuestion(
        @PathVariable questionId: Long,
        @RequestParam(required = false) type: String?
    ): ApiResponse<List<ActivityDto>>

    @GetMapping("/api/activities/member/{memberId}")
    fun getActivitiesByMember(@PathVariable memberId: Long): ApiResponse<List<ActivityDto>>

    @GetMapping("/api/activities/member/{memberId}/question/{questionId}")
    fun getActivityByMemberAndQuestion(
        @PathVariable memberId: Long,
        @PathVariable questionId: Long
    ): ApiResponse<ActivityDto?>
}
