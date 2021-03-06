package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-pdl-gdpr", "mock-infotrygd-feed", "mock-infotrygd-barnetrygd")
@Tag("integration")
@TestInstance(Lifecycle.PER_CLASS)
class GDPRInnhentingTest(
        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val integrasjonClient: IntegrasjonClient,

        @Autowired
        private val stegService: StegService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    /**
     * Norsk søker
     */
    @Test
    fun `Autovedtak for nordisk søker`() {
        stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(NyBehandlingHendelse(
                morsIdent = GDPRMockConfiguration.morsfnr[0],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[0])
        ))

        verify(exactly = 0) {
            integrasjonClient.hentArbeidsforhold(GDPRMockConfiguration.morsfnr[0], any())
            personopplysningerService.hentOpphold(GDPRMockConfiguration.morsfnr[0])
        }

        verify(exactly = 1) {
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[0]))
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[0])
        }
    }

    /**
     * Eøs søker, norsk medforelder
     * Trenger arbeidsforhold på søker, men ikke på far som er nordisk
     */
    @Test
    fun `Autovedtak for eøs søker og nordisk medforelder`() {
        stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(NyBehandlingHendelse(
                morsIdent = GDPRMockConfiguration.morsfnr[1],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[1])
        ))

        verify(exactly = 1) {
            integrasjonClient.hentArbeidsforhold(GDPRMockConfiguration.morsfnr[1], any())
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[1])

            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[1]))
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.farsfnr[0]))
        }
    }

    /**
     * Eøs søker og medforelder
     * Trenger arbeidsforhold på søker, men ikke på far som er nordisk
     */
    @Test
    fun `Autovedtak for eøs søker og medforelder`() {
        stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(NyBehandlingHendelse(
                morsIdent = GDPRMockConfiguration.morsfnr[2],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[2])
        ))

        verify(exactly = 1) {
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[2])
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[2]))
            integrasjonClient.hentArbeidsforhold(GDPRMockConfiguration.morsfnr[2], any())

            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.farsfnr[1]))
            integrasjonClient.hentArbeidsforhold(GDPRMockConfiguration.farsfnr[1], any())
        }
    }

    /**
     * Tredjelandsborger
     * Trenger kun opphold på søker
     */
    @Test
    fun `Autovedtak for tredjelandsborger`() {
        stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(NyBehandlingHendelse(
                morsIdent = GDPRMockConfiguration.morsfnr[3],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[3])
        ))

        verify(exactly = 1) {
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[3])
            personopplysningerService.hentOpphold(GDPRMockConfiguration.morsfnr[3])
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[3]))
        }
    }

    /**
     * Manuell saksbehandling, henter bosted, opphold og statsborgerskap for søker
     * Data for barn hentes først ved valgt barn i registrer søknad
     */
    @Test
    fun `Manuell saksbehandling`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(GDPRMockConfiguration.morsfnr[4]))

        stegService.håndterNyBehandling(NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = GDPRMockConfiguration.morsfnr[4],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[4]),
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        ))


        verify(exactly = 1) {
            personopplysningerService.hentHistoriskPersoninfoManuell(GDPRMockConfiguration.morsfnr[4])
        }

        verify(exactly = 0) {
            personopplysningerService.hentHistoriskPersoninfoManuell(GDPRMockConfiguration.barnefnr[4])
        }
    }
}

@Configuration
class GDPRMockConfiguration {

    val now: LocalDate = LocalDate.now()

    @Bean
    @Profile("mock-pdl-gdpr")
    @Primary
    fun mockPersonopplysningsService(): PersonopplysningerService {
        val personopplysningerServiceMock = mockk<PersonopplysningerService>()

        val identSlot = slot<Ident>()

        every {
            personopplysningerServiceMock.hentIdenter(capture(identSlot))
        } answers {
            listOf(IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"))
        }

        // Dummy registerhistorikk ved manuell behandling
        every {
            personopplysningerServiceMock.hentHistoriskPersoninfoManuell(morsfnr[4])
        } returns PersonInfo(fødselsdato = now, navn = "")

        every {
            personopplysningerServiceMock.hentHistoriskPersoninfoManuell(barnefnr[4])
        } returns PersonInfo(fødselsdato = now, navn = "")

        // Norsk mor
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(
                        ForelderBarnRelasjon(
                                personIdent = Personident(id = morsfnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        // Utenlandsk mor, norsk far
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker To",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(farsfnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Far Søker En",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(
                        ForelderBarnRelasjon(
                                personIdent = Personident(id = farsfnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        // Utenlandsk mor og far
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Tre",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(farsfnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Far Søker To",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[2])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[2])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(
                        ForelderBarnRelasjon(
                                personIdent = Personident(id = farsfnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        // Tredjelandsborger
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[3])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Fire",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(farsfnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Far Søker Tre",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[3])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[3])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(
                        ForelderBarnRelasjon(
                                personIdent = Personident(id = farsfnr[2]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        // Manuell saksbehandling
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[4])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Fem",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        // Automatisk, lagrecase
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[5])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Fem",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type=SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        val hentAktørIdIdentSlot = slot<Ident>()
        every {
            personopplysningerServiceMock.hentAktivAktørId(capture(hentAktørIdIdentSlot))
        } answers {
            AktørId(id = "0${hentAktørIdIdentSlot.captured.ident}")
        }

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[0]))
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[1]))
        } returns listOf(Statsborgerskap(land = "POL", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[2]))
        } returns listOf(Statsborgerskap(land = "POL", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[3]))
        } returns listOf(Statsborgerskap(land = "USA", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[4]))
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[5]))
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(farsfnr[0]))
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(farsfnr[1]))
        } returns listOf(Statsborgerskap(land = "POL", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(farsfnr[2]))
        } returns listOf(Statsborgerskap(land = "USA", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentOpphold(any())
        } returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))

        every {
            personopplysningerServiceMock.hentBostedsadresseperioder(any())
        } returns listOf()

        every {
            personopplysningerServiceMock.hentDødsfall(any())
        } returns DødsfallData(false, null)

        every {
            personopplysningerServiceMock.hentVergeData(any())
        } returns VergeData(false)

        return personopplysningerServiceMock
    }

    companion object {

        val morsfnr = listOf("12445678910", "12445678911", "12445678917", "12445678918", "12345678921", "12345678923")
        val farsfnr = listOf("12345678912", "12345678913", "12345678919")
        val barnefnr = listOf("12345678914", "12345678915", "12345678916", "12345678920", "12345678922")

        val søkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 1111, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "2222")
        )
    }

}