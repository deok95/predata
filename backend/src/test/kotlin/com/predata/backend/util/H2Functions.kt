package com.predata.backend.util

import java.sql.Timestamp
import java.time.Instant

/**
 * H2 인메모리 DB용 MySQL 호환 함수 alias 구현체
 *
 * H2 2.x MySQL MODE가 지원하지 않는 MySQL 함수를 대체한다.
 * application-test.yml H2 INIT URL 파라미터를 통해 등록된다.
 *
 * 등록된 alias:
 *   UTC_TIMESTAMP(precision) → 현재 UTC 시각 반환 (JVM 타임존이 UTC임을 전제)
 */
object H2Functions {

    @JvmStatic
    fun utcTimestamp(precision: Int): Timestamp = Timestamp.from(Instant.now())
}
