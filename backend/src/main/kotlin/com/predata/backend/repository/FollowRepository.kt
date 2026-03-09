package com.predata.backend.repository

import com.predata.backend.domain.Follow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FollowRepository : JpaRepository<Follow, Long> {
    fun existsByFollowerIdAndFollowingId(followerId: Long, followingId: Long): Boolean

    fun countByFollowerId(followerId: Long): Long

    fun countByFollowingId(followingId: Long): Long

    fun findByFollowerIdOrderByCreatedAtDesc(followerId: Long, pageable: Pageable): Page<Follow>

    fun findByFollowingIdOrderByCreatedAtDesc(followingId: Long, pageable: Pageable): Page<Follow>

    fun deleteByFollowerIdAndFollowingId(followerId: Long, followingId: Long): Long

    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :followerId")
    fun findFollowingIdsByFollowerId(@Param("followerId") followerId: Long): List<Long>
}
