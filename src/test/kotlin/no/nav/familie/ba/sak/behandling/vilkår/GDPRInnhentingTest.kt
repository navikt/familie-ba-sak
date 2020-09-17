package no.nav.familie.ba.sak.behandling.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.kontrakter.felles.personopplysning.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
@ActiveProfiles("dev", "mock-pdl-gdpr")
@Tag("integration")
@TestInstance(Lifecycle.PER_CLASS)
class GDPRInnhentingTest(
        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val integrasjonClient: IntegrasjonClient,

        @Autowired
        private val stegService: StegService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Autovedtak for nordisk søker skal kun innhente informasjon på søker`() {
        stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(NyBehandlingHendelse(
                søkersIdent = GDPRMockConfiguration.morsfnr[0],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[0])
        ))

        verify(exactly = 0) {
            integrasjonClient.hentArbeidsforhold(any(), any())
            personopplysningerService.hentOpphold(any())
        }

        verify(exactly = 1) {
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[0]))
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[0])
        }
    }

    @Test
    fun `Autovedtak for eøs søker skal innhente informasjon på søker og medforelder, men ikke arbeidsforhold på medforelder`() {
        stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(NyBehandlingHendelse(
                søkersIdent = GDPRMockConfiguration.morsfnr[1],
                barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[1])
        ))

        // Hentes kun for søker. Arbeidsforhold innhentes ikke på medforelder fordi medforelder er nordisk
        verify(exactly = 1) {
            personopplysningerService.hentOpphold(GDPRMockConfiguration.morsfnr[1])
            integrasjonClient.hentArbeidsforhold(GDPRMockConfiguration.morsfnr[1], any())
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[1])
        }

        // Hentes for søker og medforelder
        verify(exactly = 1) {
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[1]))
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.farsfnr[0]))
        }
    }
    // TODO: Test for auto med eøs søker og medforelder
    // TODO: Teste noe på opphold?
    // TODO: Test for manuell SB at vi ikke henter inn noe info på noen parter
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

        // Norsk mor
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[0], PersonInfoQuery.ENKEL)
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(
                        Familierelasjon(
                                personIdent = Personident(id = morsfnr[0]),
                                relasjonsrolle = FAMILIERELASJONSROLLE.MOR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        // Utenlands mor, norsk far
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker To",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(farsfnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Far Søker En",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[1], PersonInfoQuery.ENKEL)
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(
                        Familierelasjon(
                                personIdent = Personident(id = farsfnr[0]),
                                relasjonsrolle = FAMILIERELASJONSROLLE.FAR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
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
            personopplysningerServiceMock.hentStatsborgerskap(Ident(farsfnr[0]))
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(barnefnr[0]))
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusMonths(1), gyldigTilOgMed = null))

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

        val morsfnr = listOf("12445678910", "12445678911")
        val farsfnr = listOf("12345678912", "12345678913")
        val barnefnr = listOf("12345678911", "12345678912")

        val søkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 1111, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "2222")
        )

        val ikkeOppfyltBarnBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 3333, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "4444")
        )

    }

}