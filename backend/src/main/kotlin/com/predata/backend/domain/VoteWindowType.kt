package com.predata.backend.domain

import java.time.Duration

enum class VoteWindowType(val duration: Duration) {
    H3(Duration.ofHours(3)),
    H6(Duration.ofHours(6)),
    D1(Duration.ofDays(1)),
    D3(Duration.ofDays(3)),
}
