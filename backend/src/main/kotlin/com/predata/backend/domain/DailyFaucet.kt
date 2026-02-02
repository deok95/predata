package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "daily_faucets",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_faucet_member_date",
            columnNames = ["member_id", "reset_date"]
        )
    ]
)
data class DailyFaucet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faucet_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "claimed")
    var claimed: Boolean = false,

    @Column(name = "amount")
    val amount: Long = 100,

    @Column(name = "reset_date", nullable = false)
    val resetDate: LocalDate
)
