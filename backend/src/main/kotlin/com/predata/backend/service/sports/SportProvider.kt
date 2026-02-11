package com.predata.backend.service.sports

import com.predata.backend.domain.SportsMatch
import com.predata.backend.service.MatchResult

/**
 * 스포츠 종목별 데이터 프로바이더 인터페이스.
 * 새로운 종목(농구, 야구, UFC 등) 추가 시 이 인터페이스를 구현하고
 * @Component 어노테이션을 붙이면 SportProviderRegistry에 자동 등록됨.
 */
interface SportProvider {
    /** 종목 타입 식별자 (예: "FOOTBALL", "BASKETBALL") */
    val sportType: String

    /** 다가오는 경기/이벤트 가져오기 */
    fun fetchUpcomingMatches(): List<SportsMatch>

    /** 특정 경기/이벤트의 결과 가져오기 */
    fun fetchMatchResult(externalApiId: String): MatchResult?

    /** 경기 정보로 질문 제목 생성 */
    fun generateQuestionTitle(match: SportsMatch): String

    /** 예상 경기 시간 (시간 단위, 만료 시간 계산용) */
    fun getMatchDurationHours(): Long

    /** 실시간 스코어 추적 지원 여부 */
    fun supportsLiveTracking(): Boolean

    /** 경기 결과를 YES/NO로 매핑 */
    fun determineSettlement(matchResult: String): String
}
