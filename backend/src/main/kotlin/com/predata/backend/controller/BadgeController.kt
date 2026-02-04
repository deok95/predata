package com.predata.backend.controller

import com.predata.backend.domain.BadgeDefinition
import com.predata.backend.repository.BadgeDefinitionRepository
import com.predata.backend.service.BadgeService
import com.predata.backend.service.BadgeWithProgressResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/badges")
class BadgeController(
    private val badgeService: BadgeService,
    private val badgeDefinitionRepository: BadgeDefinitionRepository
) {

    @GetMapping("/definitions")
    fun getAllDefinitions(): ResponseEntity<List<BadgeDefinition>> {
        return ResponseEntity.ok(badgeDefinitionRepository.findAllByOrderBySortOrderAsc())
    }

    @GetMapping("/member/{memberId}")
    fun getMemberBadges(@PathVariable memberId: Long): ResponseEntity<List<BadgeWithProgressResponse>> {
        return ResponseEntity.ok(badgeService.getMemberBadges(memberId))
    }

    @GetMapping("/member/{memberId}/earned")
    fun getEarnedBadges(@PathVariable memberId: Long): ResponseEntity<List<BadgeWithProgressResponse>> {
        return ResponseEntity.ok(badgeService.getEarnedBadges(memberId))
    }
}
