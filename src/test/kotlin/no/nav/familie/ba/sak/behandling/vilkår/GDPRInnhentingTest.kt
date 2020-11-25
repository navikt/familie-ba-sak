package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.*
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
        private val stegService: StegService,

        @Autowired
        private val behandleFødselshendelseTask: BehandleFødselshendelseTask,

        @Autowired
        private val fødelshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository
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
     * Manuell saksbehandling, skal ikke hente noe ekstra informasjon
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

        verify(exactly = 0) {
            personopplysningerService.hentBostedsadresseperioder(GDPRMockConfiguration.morsfnr[4])
            personopplysningerService.hentOpphold(GDPRMockConfiguration.morsfnr[4])
            personopplysningerService.hentStatsborgerskap(Ident(GDPRMockConfiguration.morsfnr[4]))
            integrasjonClient.hentArbeidsforhold(GDPRMockConfiguration.morsfnr[4], any())
        }
    }

    /**
     * Automatisk fødselshendelse, lagre ned innhenting og resultat
     */
    @Test
    fun `Lagring av fødselshendelse til midlertidig tabell`() {
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(
                        morsIdent = GDPRMockConfiguration.morsfnr[5],
                        barnasIdenter = listOf(GDPRMockConfiguration.barnefnr[4])
                ))))

        val fødselshendelsePreLansering =
                fødelshendelsePreLanseringRepository.finnFødselshendelsePreLansering(GDPRMockConfiguration.morsfnr[5])
                        .firstOrNull()
        Assertions.assertNotNull(fødselshendelsePreLansering)

        val faktaForFiltreringsregler = mapTilFaktaTilFiltrering(fødselshendelsePreLansering!!.filtreringsreglerInput)
        Assertions.assertEquals(GDPRMockConfiguration.morsfnr[5], faktaForFiltreringsregler.mor.personIdent.ident)

        val vurderinger = fødselshendelsePreLansering.hentVilkårsvurderingerForFødselshendelse().vurderinger
        Assertions.assertNotNull(vurderinger.find {
            mapTilFaktaTilVilkårsvurdering(it.faktaTilVilkårsvurdering).personForVurdering.personIdent.ident == GDPRMockConfiguration.morsfnr[5]
        })

        Assertions.assertNotNull(vurderinger.find {
            mapTilFaktaTilVilkårsvurdering(it.faktaTilVilkårsvurdering).personForVurdering.personIdent.ident == GDPRMockConfiguration.barnefnr[4]
        })

        Assertions.assertNull(fagsakService.hent(PersonIdent(GDPRMockConfiguration.morsfnr[5])))
    }

    fun mapTilFaktaTilFiltrering(faktaTilFiltrering: String): SubsetFaktaTilFiltreringForTest = objectMapper.readValue(
            faktaTilFiltrering)

    fun mapTilFaktaTilVilkårsvurdering(faktaTilVilkårsvurdering: String): SubsetFaktaTilVilkårsvurderingForTest = objectMapper.readValue(
            faktaTilVilkårsvurdering)
}

data class SubsetFaktaTilFiltreringForTest(val mor: SubsetAvPersonForTest)

data class SubsetFaktaTilVilkårsvurderingForTest(val personForVurdering: SubsetAvPersonForTest)

data class SubsetAvPersonForTest(
        val personIdent: PersonIdent,
)

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
            personopplysningerServiceMock.hentPersoninfo(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
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

        // Utenlandsk mor, norsk far
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
            personopplysningerServiceMock.hentPersoninfo(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
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

        // Utenlandsk mor og far
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Tre",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(farsfnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Far Søker To",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[2])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[2])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(
                        Familierelasjon(
                                personIdent = Personident(id = farsfnr[1]),
                                relasjonsrolle = FAMILIERELASJONSROLLE.FAR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        // Tredjelandsborger
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[3])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Fire",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(farsfnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Far Søker Tre",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[3])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[3])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(
                        Familierelasjon(
                                personIdent = Personident(id = farsfnr[2]),
                                relasjonsrolle = FAMILIERELASJONSROLLE.FAR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        // Manuell saksbehandling
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[4])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Fem",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        // Automatisk, lagrecase
        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[5])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Fem",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[4])
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(),
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

        val ikkeOppfyltBarnBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 3333, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "4444")
        )

    }

}