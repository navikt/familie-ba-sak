package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
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

    @Bean
    @Primary
    fun mockIntegrasjonOnBehalfClient(): IntegrasjonOnBehalfClient {

        val mockIntegrasjonOnBehalfClient = mockk<IntegrasjonOnBehalfClient>(relaxed = true)

        every {
            mockIntegrasjonOnBehalfClient.sjekkTilgangTilPersoner(any())
        } returns listOf(Tilgang(true, null))

        every {
            mockIntegrasjonOnBehalfClient.hentPersoninfo(any())
        } returns Personinfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")

        return mockIntegrasjonOnBehalfClient
    }

    @Bean
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {

        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = false)

        every {
            mockIntegrasjonClient.hentAktørId(any())
        } returns randomAktørId()

        every {
            mockIntegrasjonClient.journalFørVedtaksbrev(eq(søkerFnr[0]), any(), any())
        } returns "Testrespons"

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(barnFnr[0]))
        } returns Personinfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "Jenta Barnesen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(barnFnr[1]))
        } returns Personinfo(fødselsdato = LocalDate.of(2019, 5, 1), kjønn = Kjønn.MANN, navn = "Gutten Barnesen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(søkerFnr[0]))
        } returns Personinfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(søkerFnr[1]))
        } returns Personinfo(fødselsdato = LocalDate.of(1991, 2, 20), kjønn = Kjønn.MANN, navn = "Far Faresen")

        return mockIntegrasjonClient
    }

    companion object {
        val søkerFnr = arrayOf("12345678910", "11223344556")
        val barnFnr = arrayOf("01101800033", "01101900033")
    }
}

fun mockHentPersoninfoForMedIdenter(mockIntegrasjonClient: IntegrasjonClient, søkerFnr: String, barnFnr: String) {
    every {
        mockIntegrasjonClient.hentPersoninfoFor(eq(barnFnr))
    } returns Personinfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "Barn Barnesen")

    every {
        mockIntegrasjonClient.hentPersoninfoFor(eq(søkerFnr))
    } returns Personinfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")
}
