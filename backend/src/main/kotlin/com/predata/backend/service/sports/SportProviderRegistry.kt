package com.predata.backend.service.sports

import org.springframework.stereotype.Service

/**
 * 스포츠 프로바이더 레지스트리.
 * Spring이 모든 SportProvider 구현체를 자동 주입.
 * 새 종목 추가 시 @Component 클래스만 만들면 자동 등록됨.
 */
@Service
class SportProviderRegistry(providers: List<SportProvider>) {

    private val providerMap: Map<String, SportProvider> =
        providers.associateBy { it.sportType }

    fun getProvider(sportType: String): SportProvider? = providerMap[sportType]

    fun getAllProviders(): List<SportProvider> = providerMap.values.toList()

    fun getSupportedSports(): List<String> = providerMap.keys.toList()
}
