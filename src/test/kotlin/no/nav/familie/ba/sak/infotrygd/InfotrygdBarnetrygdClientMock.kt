package no.nav.familie.ba.sak.infotrygd

import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile


@TestConfiguration
class InfotrygdBarnetrygdConfig {

    @Bean
    @Profile("mock-infotrygd-barnetrygd")
    @Primary
    fun mockInfotrygdBarnetrygd(): InfotrygdBarnetrygdClient {
        val mockk = mockk<InfotrygdBarnetrygdClient>(relaxed = true)
        every { mockk.harLøpendeSakIInfotrygd(any(), any()) } returns false
        every { mockk.hentSaker(any(), any()) } returns InfotrygdSøkResponse(emptyList(), emptyList())

        return mockk
    }
}