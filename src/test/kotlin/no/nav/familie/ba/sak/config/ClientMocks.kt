package no.nav.familie.ba.sak.config

import io.mockk.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.*
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.time.Month
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
            mockPersonopplysningerService.hentPersoninfo(eq(barnFnr[0]), PersonInfoQuery.ENKEL)
        } returns personInfo.getValue(barnFnr[0])

        every {
            mockPersonopplysningerService.hentPersoninfo(eq(barnFnr[1]), PersonInfoQuery.ENKEL)
        } returns personInfo.getValue(barnFnr[1])

        every {
            mockPersonopplysningerService.hentPersoninfo(eq(søkerFnr[0]), PersonInfoQuery.ENKEL)
        } returns personInfo.getValue(søkerFnr[0])

        every {
            mockPersonopplysningerService.hentPersoninfo(eq(søkerFnr[1]), PersonInfoQuery.ENKEL)
        } returns personInfo.getValue(søkerFnr[1])

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(barnFnr[0]))
        } returns personInfo.getValue(barnFnr[0])

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(barnFnr[1]))
        } returns personInfo.getValue(barnFnr[1])

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(søkerFnr[0]))
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
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(søkerFnr[1]))
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

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(søkerFnr[2]))
        } returns personInfo.getValue(søkerFnr[2]).copy(
                familierelasjoner = setOf(
                        Familierelasjon(personIdent = Personident(id = barnFnr[0]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                        navn = personInfo.getValue(barnFnr[0]).navn,
                                        fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato),
                        Familierelasjon(personIdent = Personident(id = barnFnr[1]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                        navn = personInfo.getValue(barnFnr[1]).navn,
                                        fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato,
                                        adressebeskyttelseGradering = personInfo.getValue(barnFnr[1]).adressebeskyttelseGradering),
                        Familierelasjon(personIdent = Personident(id = søkerFnr[0]),
                                        relasjonsrolle = FAMILIERELASJONSROLLE.FAR)),
                familierelasjonerMaskert = setOf(
                        FamilierelasjonMaskert(relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
                                               adressebeskyttelseGradering = personInfo.getValue(barnDetIkkeGisTilgangTilFnr).adressebeskyttelseGradering!!)
                ))

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(integrasjonerFnr))
        } returns personInfo.getValue(integrasjonerFnr).copy(
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
            mockPersonopplysningerService.hentAdressebeskyttelseSomSystembruker(any())
        } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT

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
                FinnOppgaveResponseDto(2,
                                       listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999")))

        every { mockIntegrasjonClient.opprettOppgave(any()) } returns
                "12345678"

        every { mockIntegrasjonClient.patchOppgave(any()) } returns
                OppgaveResponse(12345678L)

        every { mockIntegrasjonClient.fordelOppgave(any(), any()) } returns
                "12345678"

        every { mockIntegrasjonClient.oppdaterJournalpost(any(), any()) } returns
                OppdaterJournalpostResponse("1234567")

        every { mockIntegrasjonClient.journalførVedtaksbrev(any(), any(), any(), any()) } returns "journalpostId"

        every {
            mockIntegrasjonClient.journalførManueltBrev(any(), any(), any(), any(), any())
        } returns "journalpostId"

        every { mockIntegrasjonClient.distribuerBrev(any()) } returns success("bestillingsId")

        every { mockIntegrasjonClient.ferdigstillJournalpost(any(), any()) } just runs

        every { mockIntegrasjonClient.ferdigstillOppgave(any()) } just runs

        every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns
                listOf(Arbeidsfordelingsenhet("4833", "NAV Familie- og pensjonsytelser Oslo 1"))

        every { mockIntegrasjonClient.hentDokument(any(), any()) } returns
                "mock data".toByteArray()

        val idSlot = slot<List<String>>()
        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(capture(idSlot))
        } answers {
            if (idSlot.captured.contains(barnDetIkkeGisTilgangTilFnr))
                listOf(Tilgang(false, null))
            else
                listOf(Tilgang(true, null))
        }

        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(any<Set<Person>>())
        } returns listOf(Tilgang(false, null))


        every { mockIntegrasjonClient.hentPersonIdent(any()) } returns PersonIdent(søkerFnr[0])

        every { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) } returns emptyList()

        initEuKodeverk(mockIntegrasjonClient)

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
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(farId)
        } returns PersonInfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(morId)
        } returns PersonInfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(barnId)
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
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(ukjentId)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "ikke funnet")

        val feilId = "41235678910"
        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjoner(feilId)
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

        val FOM_1900 = LocalDate.of(1900, Month.JANUARY, 1)
        val FOM_1990 = LocalDate.of(1990, Month.JANUARY, 1)
        val FOM_2000 = LocalDate.of(2000, Month.JANUARY, 1)
        val FOM_2004 = LocalDate.of(2004, Month.JANUARY, 1)
        val FOM_2008 = LocalDate.of(2008, Month.JANUARY, 1)
        val FOM_2010 = LocalDate.of(2010, Month.JANUARY, 1)
        val TOM_2000 = LocalDate.of(1999, Month.DECEMBER, 31)
        val TOM_2004 = LocalDate.of(2003, Month.DECEMBER, 31)
        val TOM_2010 = LocalDate.of(2009, Month.DECEMBER, 31)
        val TOM_9999 = LocalDate.of(9999, Month.DECEMBER, 31)

        fun initEuKodeverk(integrasjonClient: IntegrasjonClient) {
            val beskrivelsePolen = BeskrivelseDto("POL", "")
            val betydningPolen = BetydningDto(FOM_2004, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
            val beskrivelseTyskland = BeskrivelseDto("DEU", "")
            val betydningTyskland = BetydningDto(FOM_1900, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
            val beskrivelseDanmark = BeskrivelseDto("DEN", "")
            val betydningDanmark = BetydningDto(FOM_1990, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
            val beskrivelseUK = BeskrivelseDto("GBR", "")
            val betydningUK = BetydningDto(FOM_1900, TOM_2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

            val kodeverkLand = KodeverkDto(mapOf(
                    "POL" to listOf(betydningPolen),
                    "DEU" to listOf(betydningTyskland),
                    "DEN" to listOf(betydningDanmark),
                    "GBR" to listOf(betydningUK)))

            every { integrasjonClient.hentAlleEØSLand() }
                    .returns(kodeverkLand)
        }

        val søkerFnr = arrayOf("12345678910", "11223344556", "12345678911")
        val barnFødselsdatoer = arrayOf(LocalDate.now().minusYears(2), LocalDate.now().førsteDagIInneværendeMåned())
        val barnFnr = arrayOf(barnFødselsdatoer[0].tilddMMYY() + "00033", barnFødselsdatoer[1].tilddMMYY() + "00033")
        val barnDetIkkeGisTilgangTilFnr = "12345678912"
        val integrasjonerFnr = "10000111111"
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
                søkerFnr[2] to PersonInfo(fødselsdato = LocalDate.of(1985, 7, 10),
                                          bostedsadresse = null,
                                          sivilstand = SIVILSTAND.GIFT,
                                          kjønn = Kjønn.KVINNE,
                                          navn = "Moder Jord",
                                          adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT),
                barnFnr[0] to PersonInfo(fødselsdato = barnFødselsdatoer[0],
                                         bostedsadresse = bostedsadresse,
                                         sivilstand = SIVILSTAND.UOPPGITT,
                                         kjønn = Kjønn.MANN,
                                         navn = "Gutten Barnesen"),
                barnFnr[1] to PersonInfo(fødselsdato = barnFødselsdatoer[1],
                                         bostedsadresse = bostedsadresse,
                                         sivilstand = SIVILSTAND.UGIFT,
                                         kjønn = Kjønn.KVINNE,
                                         navn = "Jenta Barnesen",
                                         adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG),
                integrasjonerFnr to PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19),
                                               bostedsadresse = bostedsadresse,
                                               sivilstand = SIVILSTAND.GIFT,
                                               kjønn = Kjønn.KVINNE,
                                               navn = "Mor Moresen"),
                barnDetIkkeGisTilgangTilFnr to PersonInfo(fødselsdato = LocalDate.of(2019, 6, 22),
                                                          bostedsadresse = bostedsadresse,
                                                          sivilstand = SIVILSTAND.UGIFT,
                                                          kjønn = Kjønn.KVINNE,
                                                          navn = "Maskert Banditt",
                                                          adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
        )
    }

}

fun mockHentPersoninfoForMedIdenter(mockPersonopplysningerService: PersonopplysningerService, søkerFnr: String, barnFnr: String) {
    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(barnFnr))
    } returns PersonInfo(fødselsdato = LocalDate.of(2018, 5, 1), kjønn = Kjønn.KVINNE, navn = "Barn Barnesen")

    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjoner(eq(søkerFnr))
    } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")

    every {
        mockPersonopplysningerService.hentAktivAktørId(any())
    } returns AktørId("1")
}

val TEST_PDF = ClientMocks::class.java.getResource("/dokument/mockvedtak.pdf").readBytes()
