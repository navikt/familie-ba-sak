package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ClientMocks {

    @Bean
    @Primary
    fun mockIntegrasjonOnBehalfClient(): IntegrasjonOnBehalfClient {

        val mockIntegrasjonOnBehalfClient = mockk<IntegrasjonOnBehalfClient>(relaxed = false)

        every {
            mockIntegrasjonOnBehalfClient.sjekkTilgangTilPersoner(any<Set<Person>>())
        } returns listOf(Tilgang(true, null))

        every {
            mockIntegrasjonOnBehalfClient.sjekkTilgangTilPersoner(any<List<String>>())
        } returns listOf(Tilgang(true, null))

        every {
            mockIntegrasjonOnBehalfClient.hentPersoninfo(eq(søkerFnr[0]))
        } returns Personinfo(fødselsdato = LocalDate.of(1990, 2, 19),
                             kjønn = Kjønn.KVINNE,
                             navn = "Mor Moresen",
                             familierelasjoner = setOf(
                                     Familierelasjoner(personIdent = Personident(id = barnFnr[0]),
                                                       relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                                       navn = "Gutten Barnesen",
                                                       fødselsdato = LocalDate.of(2015, 10, 3)),
                                     Familierelasjoner(personIdent = Personident(id = søkerFnr[1]),
                                                       relasjonsrolle = FAMILIERELASJONSROLLE.MEDMOR)))

        every {
            mockIntegrasjonOnBehalfClient.hentPersoninfo(eq(søkerFnr[1]))
        } returns Personinfo(fødselsdato = LocalDate.of(1995, 2, 19),
                             kjønn = Kjønn.MANN,
                             navn = "Far Faresen",
                             familierelasjoner = setOf(
                                     Familierelasjoner(personIdent = Personident(id = barnFnr[0]),
                                                       relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                                       navn = "Barn Barney Barnesen",
                                                       fødselsdato = LocalDate.of(2017, 4, 13)),
                                     Familierelasjoner(personIdent = Personident(id = søkerFnr[0]),
                                                       relasjonsrolle = FAMILIERELASJONSROLLE.FAR)))

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
