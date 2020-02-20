package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
class ClientMocks {


    @Bean
    @Primary
    fun mockIntegrasjonOnBehalfClient(): IntegrasjonOnBehalfClient {

        val mockIntegrasjonOnBehalfClient = mockk<IntegrasjonOnBehalfClient>(relaxed = true)

        every {
            mockIntegrasjonOnBehalfClient.sjekkTilgangTilPersoner(any())
        } returns listOf(Tilgang(true, null))
        return mockIntegrasjonOnBehalfClient
    }


}

