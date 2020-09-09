package no.nav.familie.ba.sak.config

import io.mockk.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.oppgave.OppgaverOgAntall
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.util.*

@Component
class ClientMocks {

    @Bean
    @Profile("mock-pdl")
    @Primary
    fun mockPersonopplysningerService(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = false)

        every {
            mockPersonopplysningerService.hentAktivAktørId(any())
        } answers {
            randomAktørId()
        }

        every {
            mockPersonopplysningerService.hentAktivPersonIdent(any())
        } answers {
            PersonIdent(randomFnr())
        }

        every {
            mockPersonopplysningerService.hentStatsborgerskap(any())
        } answers {
            listOf(Statsborgerskap("NOR",
                                   LocalDate.of(1990, 1, 25),
                                   null))
        }

        every {
            mockPersonopplysningerService.hentOpphold(any())
        } answers {
            listOf(Opphold(type = OPPHOLDSTILLATELSE.PERMANENT,
                           oppholdFra = LocalDate.of(1990, 1, 25),
                           oppholdTil = LocalDate.of(2999, 1, 1)))
        }

        every {
            mockPersonopplysningerService.hentBostedsadresseperioder(any())
        } answers {
            listOf(GrBostedsadresseperiode(
                    periode = DatoIntervallEntitet(
                            fom = LocalDate.of(2002, 1, 4),
                            tom = LocalDate.of(2002, 1, 5)
                    )))
        }

        val identSlot = slot<Ident>()
        every {
            mockPersonopplysningerService.hentIdenter(capture(identSlot))
        } answers {
            listOf(IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"))
        }

        every {
            mockPersonopplysningerService.hentDødsfall(any())
        } returns DødsfallData(false, null)

        every {
            mockPersonopplysningerService.hentVergeData(any())
        } returns VergeData(false)

        every {
            mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
        } returns "NO"

        every {
            mockPersonopplysningerService.hentPersoninfoFor(eq(barnFnr[0]))
        } returns personInfo.getValue(barnFnr[0])

        every {
            mockPersonopplysningerService.hentPersoninfoFor(eq(barnFnr[1]))
        } returns personInfo.getValue(barnFnr[1])

        every {
            mockPersonopplysningerService.hentPersoninfoFor(eq(søkerFnr[0]))
        } returns personInfo.getValue(søkerFnr[0]).copy(
                familierelasjoner = setOf(
                        Familierelasjon(personIdent = Personident(id = barnFnr[0]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                        navn = personInfo.getValue(barnFnr[0]).navn,
                                        fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato),
                        Familierelasjon(personIdent = Personident(id = barnFnr[1]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                        navn = personInfo.getValue(barnFnr[1]).navn,
                                        fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato),
                        Familierelasjon(personIdent = Personident(id = søkerFnr[1]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.MEDMOR)))
        every {
            mockPersonopplysningerService.hentPersoninfoFor(eq(søkerFnr[1]))
        } returns personInfo.getValue(søkerFnr[1]).copy(
                familierelasjoner = setOf(
                        Familierelasjon(personIdent = Personident(id = barnFnr[0]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                        navn = personInfo.getValue(barnFnr[0]).navn,
                                        fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato),
                        Familierelasjon(personIdent = Personident(id = barnFnr[1]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                        navn = personInfo.getValue(barnFnr[1]).navn,
                                        fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato),
                        Familierelasjon(personIdent = Personident(id = søkerFnr[0]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.FAR)))
        return mockPersonopplysningerService
    }

    @Bean
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {

        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = false)
        every { mockIntegrasjonClient.hentJournalpost(any()) } answers {
            success(lagTestJournalpost(søkerFnr[0],
                                       UUID.randomUUID().toString()))
        }

        every { mockIntegrasjonClient.hentJournalpost(any()) } answers {
            success(lagTestJournalpost(søkerFnr[0],
                                       UUID.randomUUID().toString()))
        }

        every { mockIntegrasjonClient.finnOppgaveMedId(any()) } returns
                lagTestOppgaveDTO(1L)

        every { mockIntegrasjonClient.hentOppgaver(any()) } returns
                OppgaverOgAntall(2, listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999")))

        every { mockIntegrasjonClient.opprettOppgave(any()) } returns
                "12345678"

        every { mockIntegrasjonClient.fordelOppgave(any(), any()) } returns
                "12345678"

        every { mockIntegrasjonClient.oppdaterJournalpost(any(), any()) } returns
                OppdaterJournalpostResponse("1234567")

        every { mockIntegrasjonClient.journalFørVedtaksbrev(any(), any(), any()) } returns "journalpostId"

        every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns listOf(Arbeidsfordelingsenhet("9999",
                                                                                                          "Ukjent"))

        every { mockIntegrasjonClient.distribuerBrev(any()) } returns success("bestillingsId")

        every { mockIntegrasjonClient.ferdigstillJournalpost(any(), any()) } just runs

        every { mockIntegrasjonClient.ferdigstillOppgave(any()) } just runs

        every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns
                listOf(Arbeidsfordelingsenhet("2970", "enhetsNavn"))

        every { mockIntegrasjonClient.hentDokument(any(), any()) } returns
                "mock data".toByteArray()

        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(any<Set<Person>>())
        } returns listOf(Tilgang(true, null))

        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(any<List<String>>())
        } returns listOf(Tilgang(true, null))

        every {
            mockIntegrasjonClient.journalFørVedtaksbrev(eq(søkerFnr[0]), any(), any())
        } returns "Testrespons"

        every {
            mockIntegrasjonClient.journalførManueltBrev(eq(søkerFnr[0]), any(), any(), any(), any())
        } returns "Testrespons"

        every { mockIntegrasjonClient.hentPersonIdent(any()) } returns PersonIdent(søkerFnr[0])

        every { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) } returns emptyList()

        return mockIntegrasjonClient
    }

    @Profile("mock-pdl-test-søk")
    @Primary
    @Bean
    fun mockPDL(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>()

        val farId = "12345678910"
        val morId = "21345678910"
        val barnId = "31245678910"

        every {
            mockPersonopplysningerService.hentPersoninfoFor(farId)
        } returns PersonInfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoFor(morId)
        } returns PersonInfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoFor(barnId)
        } returns PersonInfo(fødselsdato = LocalDate.of(2009, 5, 1), kjønn = Kjønn.MANN, navn = "Barn Mocksen",
                             familierelasjoner = setOf(
                                     Familierelasjon(Personident(farId),
                                                     FAMILIERELASJONSROLLE.FAR,
                                                     "Far Mocksen",
                                                     LocalDate.of(1969, 5, 1)),
                                     Familierelasjon(Personident(morId),
                                                     FAMILIERELASJONSROLLE.MOR,
                                                     "Mor Mocksen",
                                                     LocalDate.of(1979, 5, 1))
                             ))

        every {
            mockPersonopplysningerService.hentIdenter(any())
        } answers {
            listOf(IdentInformasjon("123", false, "FOLKEREGISTERIDENT"))
        }

        every {
            mockPersonopplysningerService.hentAktivAktørId(any())
        } answers {
            randomAktørId()
        }

        every {
            mockPersonopplysningerService.hentStatsborgerskap(any())
        } answers {
            listOf(Statsborgerskap("NOR",
                                   LocalDate.of(1990, 1, 25),
                                   null))
        }

        every {
            mockPersonopplysningerService.hentOpphold(any())
        } answers {
            listOf(Opphold(type = OPPHOLDSTILLATELSE.PERMANENT,
                           oppholdFra = LocalDate.of(1990, 1, 25),
                           oppholdTil = LocalDate.of(2999, 1, 1)))
        }

        every {
            mockPersonopplysningerService.hentBostedsadresseperioder(any())
        } answers {
            listOf(GrBostedsadresseperiode(
                    periode = DatoIntervallEntitet(
                            fom = LocalDate.of(2002, 1, 4),
                            tom = LocalDate.of(2002, 1, 5)
                    )))
        }

        every {
            mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
        } returns "NO"

        val ukjentId = "43125678910"
        every {
            mockPersonopplysningerService.hentPersoninfoFor(ukjentId)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "ikke funnet")

        val feilId = "41235678910"
        every {
            mockPersonopplysningerService.hentPersoninfoFor(feilId)
        } throws IntegrasjonException("feil id")

        return mockPersonopplysningerService
    }

    @Bean
    @Primary
    fun mockFeatureToggleService(): FeatureToggleService {
        val mockFeatureToggleService = mockk<FeatureToggleService>(relaxed = true)

        every {
            mockFeatureToggleService.isEnabled(any())
        } returns false

        return mockFeatureToggleService
    }

    companion object {
        val søkerFnr = arrayOf("12345678910", "11223344556")
        val barnFnr = arrayOf("01101800033", "01101900033")
        val bostedsadresse = Bostedsadresse(
                matrikkeladresse = Matrikkeladresse(matrikkelId = 123L, bruksenhetsnummer = "H301", tilleggsnavn = "navn",
                                                    postnummer = "0202", kommunenummer = "2231")
        )

        val personInfo = mapOf(
                søkerFnr[0] to PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19),
                                          bostedsadresse = bostedsadresse,
                                          sivilstand = SIVILSTAND.GIFT,
                                          kjønn = Kjønn.KVINNE,
                                          navn = "Mor Moresen"),
                søkerFnr[1] to PersonInfo(fødselsdato = LocalDate.of(1995, 2, 19),
                                          bostedsadresse = null,
                                          sivilstand = SIVILSTAND.GIFT,
                                          kjønn = Kjønn.MANN,
                                          navn = "Far Faresen"),
                barnFnr[0] to PersonInfo(fødselsdato = LocalDate.now().minusYears(1),
                                         bostedsadresse = bostedsadresse,
                                         sivilstand = SIVILSTAND.UOPPGITT,
                                         kjønn = Kjønn.MANN,
                                         navn = "Gutten Barnesen"),
                barnFnr[1] to PersonInfo(fødselsdato = LocalDate.now(),
                                         bostedsadresse = bostedsadresse,
                                         sivilstand = SIVILSTAND.UGIFT,
                                         kjønn = Kjønn.KVINNE,
                                         navn = "Jenta Barnesen")
        )
    }

}

fun mockHentPersoninfoForMedIdenter(mockPersonopplysningerService: PersonopplysningerService, søkerFnr: String, barnFnr: String) {
    every {
        mockPersonopplysningerService.hentPersoninfoFor(eq(barnFnr))
    } returns PersonInfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "Barn Barnesen")

    every {
        mockPersonopplysningerService.hentPersoninfoFor(eq(søkerFnr))
    } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")

    every {
        mockPersonopplysningerService.hentAktivAktørId(any())
    } returns AktørId("1")
}

val TEST_PDF = ClientMocks::class.java.getResource("/dokument/mockvedtak.pdf").readBytes()
