package no.nav.familie.ba.sak.dokument

import io.mockk.mockk
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile


@TestConfiguration
class InfotrygdFeedConfig {

    @Bean
    @Profile("mock-infotrygd-feed", "e2e")
    @Primary
    fun mockInfotrygdFeed(): InfotrygdFeedClient {
        return mockk(relaxed = true)
    }
}