package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
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


    @Profile("mock-familie-integrasjoner")
    @Bean
    @Primary
    fun mockFamilieIntegrasjon(): IntegrasjonTjeneste {
        val mockIntegrasjon = mockk<IntegrasjonTjeneste>(relaxed = true)

        every{
            mockIntegrasjon.hentAktørId(any())
        } returns AktørId("1")

        return mockIntegrasjon
    }
}

