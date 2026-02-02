package com.predata.member.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "daily_tickets",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_date",
            columnNames = ["member_id", "reset_date"]
        )
    ]
)
data class DailyTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "remaining_count")
    var remainingCount: Int = 5,

    @Column(name = "reset_date", nullable = false)
    val resetDate: LocalDate
)
