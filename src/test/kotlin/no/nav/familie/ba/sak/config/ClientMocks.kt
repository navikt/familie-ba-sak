package no.nav.familie.ba.sak.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.domene.*
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

@Component
class ClientMocks {

    @Bean
    @Profile("!test-søk")
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {

        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = false)

        every { mockIntegrasjonClient.hentJournalpost(any()) } returns success(lagTestJournalpost(søkerFnr[0], "1234"))

        every { mockIntegrasjonClient.hentBehandlendeEnhet(any(), any()) } returns
                listOf(Arbeidsfordelingsenhet("4820", "Drammen"))

        every { mockIntegrasjonClient.finnOppgaveMedId(any()) } returns
                success(lagTestOppgaveDTO(1L))

        every { mockIntegrasjonClient.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(any(), any(), any(), any()) } returns
                listOf(lagTestOppgaveDTO(1L))

        every { mockIntegrasjonClient.opprettOppgave(any()) } returns "1"

        every { mockIntegrasjonClient.ferdigstillOppgave(any()) } just Runs

        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(any<Set<Person>>())
        } returns listOf(Tilgang(true, null))

        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(any<List<String>>())
        } returns listOf(Tilgang(true, null))

        every { mockIntegrasjonClient.hentPersonIdent(any()) } returns PersonIdent(søkerFnr[0])

        every {
            mockIntegrasjonClient.hentAktørId(any())
        } returns randomAktørId()

        every {
            mockIntegrasjonClient.journalFørVedtaksbrev(eq(søkerFnr[0]), any(), any())
        } returns "Testrespons"

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(barnFnr[0]))
        } returns personInfo.getValue(barnFnr[0])

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(barnFnr[1]))
        } returns personInfo.getValue(barnFnr[0])

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(søkerFnr[0]))
        } returns personInfo.getValue(søkerFnr[0]).copy(
                familierelasjoner = setOf(
                        Familierelasjoner(personIdent = Personident(id = barnFnr[0]),
                                          relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                          navn = personInfo.getValue(barnFnr[0]).navn,
                                          fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato),
                        Familierelasjoner(personIdent = Personident(id = barnFnr[1]),
                                          relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                          navn = personInfo.getValue(barnFnr[1]).navn,
                                          fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato),
                        Familierelasjoner(personIdent = Personident(id = søkerFnr[1]),
                                          relasjonsrolle = FAMILIERELASJONSROLLE.MEDMOR)))

        every {
            mockIntegrasjonClient.hentPersoninfoFor(eq(søkerFnr[1]))
        } returns personInfo.getValue(søkerFnr[1])

        return mockIntegrasjonClient
    }

    @Profile("test-søk")
    @Primary
    @Bean
    fun mockPDL(): IntegrasjonClient {
        val mockIntegrasjonClient = mockk<IntegrasjonClient>()

        val farId = "12345678910"
        val morId = "21345678910"
        val barnId = "31245678910"

        every {
            mockIntegrasjonClient.hentPersoninfoFor(farId)
        } returns Personinfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(morId)
        } returns Personinfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(barnId)
        } returns Personinfo(fødselsdato = LocalDate.of(2009, 5, 1), kjønn = Kjønn.MANN, navn = "Barn Mocksen",
                             familierelasjoner = setOf(
                                     Familierelasjoner(Personident(farId),
                                                       FAMILIERELASJONSROLLE.FAR,
                                                       "Far Mocksen",
                                                       LocalDate.of(1969, 5, 1)),
                                     Familierelasjoner(Personident(morId),
                                                       FAMILIERELASJONSROLLE.MOR,
                                                       "Mor Mocksen",
                                                       LocalDate.of(1979, 5, 1))
                             ))

        every {
            mockIntegrasjonClient.hentAktørId(farId)
        } returns AktørId(farId)

        every {
            mockIntegrasjonClient.hentAktørId(barnId)
        } returns AktørId(barnId)

        val ukjentId= "43125678910"
        every {
            mockIntegrasjonClient.hentPersoninfoFor(ukjentId)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "ikke funnet")

        val feilId = "41235678910"
        every {
            mockIntegrasjonClient.hentPersoninfoFor(feilId)
        } throws IntegrasjonException("feil id")

        return mockIntegrasjonClient
    }

    companion object {
        val søkerFnr = arrayOf("12345678910", "11223344556")
        val barnFnr = arrayOf("01101800033", "01101900033")
        val personInfo = mapOf(
                søkerFnr[0] to Personinfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen"),
                søkerFnr[1] to Personinfo(fødselsdato = LocalDate.of(1995, 2, 19), kjønn = Kjønn.MANN, navn = "Far Faresen"),
                barnFnr[0] to Personinfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.MANN, navn = "Gutten Barnesen"),
                barnFnr[1] to Personinfo(fødselsdato = LocalDate.of(2019, 5, 1), kjønn = Kjønn.KVINNE, navn = "Jenta Barnesen")
        )
    }

    @Profile("dev")
    @Bean
    @Primary
    fun mockSatsService(): SatsService {
        val satsRepository = mockk<SatsRepository>()

        every { satsRepository.finnAlleSatserFor(SatsType.SMA) } returns listOf(Sats(1, SatsType.SMA, 660, null, null))
        every { satsRepository.finnAlleSatserFor(SatsType.TILLEGG_ORBA) } returns listOf(Sats(2,
                                                                                              SatsType.TILLEGG_ORBA,
                                                                                              1354,
                                                                                              LocalDate.of(2020, 9, 1),
                                                                                              null))
        every { satsRepository.finnAlleSatserFor(SatsType.FINN_SVAL) } returns listOf(Sats(3,
                                                                                           SatsType.FINN_SVAL,
                                                                                           330,
                                                                                           null,
                                                                                           LocalDate.of(2014, 3, 31)))
        every { satsRepository.finnAlleSatserFor(SatsType.ORBA) } returns listOf(
                Sats(4,
                     SatsType.ORBA,
                     1054,
                     LocalDate.of(2019, 3, 1),
                     null),
                Sats(5,
                     SatsType.ORBA,
                     970,
                     null,
                     LocalDate.of(2019, 2, 28))
        )
        return SatsService(satsRepository)
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
