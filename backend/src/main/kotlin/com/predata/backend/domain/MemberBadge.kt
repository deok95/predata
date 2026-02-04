package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "member_badges")
data class MemberBadge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "badge_id", nullable = false, length = 40)
    val badgeId: String,
    var progress: Int = 0,
    val target: Int = 1,
    @Column(name = "awarded_at")
    var awardedAt: LocalDateTime? = null,
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
