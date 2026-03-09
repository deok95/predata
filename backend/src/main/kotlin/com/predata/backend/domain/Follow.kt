package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "follows",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_follows_pair", columnNames = ["follower_id", "following_id"])
    ],
    indexes = [
        Index(name = "idx_follows_follower_created", columnList = "follower_id, created_at"),
        Index(name = "idx_follows_following_created", columnList = "following_id, created_at")
    ]
)
data class Follow(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    val id: Long? = null,

    @Column(name = "follower_id", nullable = false)
    val followerId: Long,

    @Column(name = "following_id", nullable = false)
    val followingId: Long,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
