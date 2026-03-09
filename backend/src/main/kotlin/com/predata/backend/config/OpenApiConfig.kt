package com.predata.backend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun predataOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("PRE(D)ATA Backend API")
                    .version("v2")
                    .description("PRE(D)ATA V2 backend OpenAPI (service-grouped)")
                    .contact(Contact().name("PRE(D)ATA Backend"))
            )
            .servers(
                listOf(
                    Server().url("http://127.0.0.1:8080").description("Local")
                )
            )

    @Bean
    fun authApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("auth")
            .pathsToMatch("/api/auth/**")
            .build()

    @Bean
    fun memberSocialApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("member-social")
            .pathsToMatch(
                "/api/members/**",
                "/api/users/**",
                "/api/notifications/**",
                "/api/referrals/**",
                "/api/leaderboard/**",
                "/api/tiers/**",
                "/api/badges/**"
            )
            .build()

    @Bean
    fun questionApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("question")
            .pathsToMatch(
                "/api/questions/**",
                "/api/admin/questions/**",
                "/api/admin/settings/question-generator"
            )
            .build()

    @Bean
    fun votingApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("voting")
            .pathsToMatch(
                "/api/votes/**",
                "/api/activities/**",
                "/api/admin/voting/**",
                "/api/admin/vote-ops/**",
                "/api/tickets/**",
                "/api/voting-pass/**"
            )
            .build()

    @Bean
    fun marketAmmApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("market-amm")
            .pathsToMatch(
                "/api/swap/**",
                "/api/pool/**",
                "/api/admin/markets/**",
                "/api/questions/top3"
            )
            .build()

    @Bean
    fun settlementRewardApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("settlement-reward")
            .pathsToMatch(
                "/api/settlements/**",
                "/api/admin/settlements/**",
                "/api/rewards/**",
                "/api/admin/rewards/**",
                "/api/analysis/**",
                "/api/analytics/**",
                "/api/premium-data/**"
            )
            .build()

    @Bean
    fun financeWalletApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("finance-wallet")
            .pathsToMatch(
                "/api/payments/**",
                "/api/portfolio/**",
                "/api/transactions/**",
                "/api/blockchain/**",
                "/api/admin/finance/**"
            )
            .build()

    @Bean
    fun opsAdminApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("ops-admin")
            .pathsToMatch(
                "/api/admin/**",
                "/api/health",
                "/api/sports/**",
                "/api/betting/**"
            )
            .build()
}
