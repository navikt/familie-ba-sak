package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ClientMocks {

    private val søkerFnr = "12345678910"
    private val barnFnr = "01101800033"

    @Bean
    @Primary
    fun mockIntegrasjonOnBehalfClient(): IntegrasjonOnBehalfClient {

        val mockIntegrasjonOnBehalfClient = mockk<IntegrasjonOnBehalfClient>(relaxed = true)

        every {
            mockIntegrasjonOnBehalfClient.sjekkTilgangTilPersoner(any())
        } returns listOf(Tilgang(true, null))

        return mockIntegrasjonOnBehalfClient
    }

    @Bean
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {

        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = true)

        every {
            mockIntegrasjonClient.hentAktørId(any())
        } returns randomAktørId()

        every {
            mockIntegrasjonClient.journalFørVedtaksbrev(eq(søkerFnr), any(), any())
        } returns "Testrespons"

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(barnFnr))
        } returns Personinfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = "K", navn = "Barn Barnesen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(søkerFnr))
        } returns Personinfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = "K", navn = "Mor Moresen")

        return mockIntegrasjonClient
    }
}

