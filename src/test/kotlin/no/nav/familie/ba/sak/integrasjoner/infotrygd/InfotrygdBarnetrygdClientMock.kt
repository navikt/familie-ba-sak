package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class InfotrygdBarnetrygdClientMock {

    @Bean
    @Profile("mock-infotrygd-barnetrygd")
    @Primary
    fun mockInfotrygdBarnetrygd(): InfotrygdBarnetrygdClient {
        val mockk = mockk<InfotrygdBarnetrygdClient>(relaxed = true)
        every { mockk.harLøpendeSakIInfotrygd(any(), any()) } returns false
        every { mockk.hentSaker(any(), any()) } returns InfotrygdSøkResponse(emptyList(), emptyList())
        every { mockk.hentStønader(any(), any()) } returns InfotrygdSøkResponse(emptyList(), emptyList())

        return mockk
    }
}
