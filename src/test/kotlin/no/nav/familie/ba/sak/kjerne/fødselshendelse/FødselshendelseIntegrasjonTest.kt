package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fødselshendelse.MockConfiguration.Companion.barnefnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.MockConfiguration.Companion.morsfnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.GDPRService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.YearMonth

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres",
                "mock-brev-klient",
                "mock-oauth",
                "mock-pdl-flere-barn",
                "mock-task-repository",
                "mock-infotrygd-barnetrygd")
@Tag("integration")
class FødselshendelseIntegrasjonTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,

        @Autowired
        private val taskRepository: TaskRepository,

        @Autowired
        private val evaluerFiltreringsreglerForFødselshendelse: EvaluerFiltreringsreglerForFødselshendelse,

        @Autowired
        private val filtreringsreglerService: FiltreringsreglerService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val fagsakRepository: FagsakRepository,

        @Autowired
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,

        @Autowired
        private val gdprService: GDPRService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    val now = LocalDate.now()

    private final val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    private final val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    private final val envServiceMock = mockk<EnvService>()

    val fødselshendelseService = FødselshendelseServiceGammel(
            infotrygdFeedServiceMock,
            infotrygdBarnetrygdClientMock,
            stegService,
            vedtakService,
            evaluerFiltreringsreglerForFødselshendelse,
            taskRepository,
            personopplysningerService,
            vilkårsvurderingRepository,
            persongrunnlagService,
            behandlingRepository,
            gdprService,
            envServiceMock
    )

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
    }

    @Test
    @Disabled
    fun `Fødselshendelse med flere barn med oppfylt vilkårsvurdering skal håndteres riktig`() {
        val oppfyltBarnFnr = listOf(barnefnr[0], barnefnr[1])

        // Tester for automatisk behandling. Kan skrus på når automatisk behandling støttes av bevrsystemet.
        assertThrows<Feil> {
            fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(
                    NyBehandlingHendelse(
                            morsfnr[0], oppfyltBarnFnr
                    )
            )
            val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsfnr[0]))
            val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak!!.id)
            val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling!!.id)!!

            assertEquals(BehandlingResultat.INNVILGET, behandling.resultat)
            assertEquals(true, vilkårsvurdering.aktiv)
            assertEquals(3, vilkårsvurdering.personResultater.size)

            assertTrue(vilkårsvurdering.personResultater.all { personResultat ->
                personResultat.vilkårResultater.all {
                    it.resultat == Resultat.OPPFYLT
                }
            })

            assertTrue(vilkårsvurdering.personResultater.map { it.personIdent }.containsAll(
                    oppfyltBarnFnr.plus(morsfnr[0])
            ))

            val andelTilkjentYtelser =
                    andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))
            val satsOrdinær = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.now(), YearMonth.now()).first()
            val satsTillegg = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.now(), YearMonth.now()).first()

            assertEquals(4, andelTilkjentYtelser.size)
            assertEquals(2, andelTilkjentYtelser.filter { it.beløp == satsOrdinær.beløp }.size)
            assertEquals(2, andelTilkjentYtelser.filter { it.beløp == satsTillegg.beløp }.size)

            val reffom = now
            val reftom = now.plusYears(18).minusMonths(2)
            val fom = reffom.toYearMonth()
            val tom = reftom.toYearMonth()

            val (barn1, barn2) = andelTilkjentYtelser.partition { it.personIdent == barnefnr[0] }
            assertEquals(fom, barn1.minByOrNull { it.stønadFom }!!.stønadFom)
            assertEquals(tom, barn1.maxByOrNull { it.stønadTom }!!.stønadTom)
            assertEquals(fom, barn2.minByOrNull { it.stønadFom }!!.stønadFom)
            assertEquals(tom, barn2.maxByOrNull { it.stønadTom }!!.stønadTom)
        }
    }

    @Test
    fun `Fødselshendelse med avslag på barn skal gi avslag på behandling`() {
        val ikkeOppfyltBarnFnr = listOf(barnefnr[2])

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(NyBehandlingHendelse(
                morsfnr[1], ikkeOppfyltBarnFnr
        ))
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsfnr[1]))
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak!!.id)
        val behandlingResultater = vilkårsvurderingRepository.finnBehandlingResultater(behandling!!.id)

        assertEquals(1, behandlingResultater.size)

        val vilkårsvurdering = behandlingResultater[0]

        assertEquals(BehandlingResultat.AVSLÅTT, behandling.resultat)
        assertEquals(true, vilkårsvurdering.aktiv)
        assertEquals(2, vilkårsvurdering.personResultater.size)
        assertTrue(vilkårsvurdering.personResultater.map { it.personIdent }.containsAll(
                ikkeOppfyltBarnFnr.plus(morsfnr[1])
        ))

        val ikkeOppfyltBarnVilkårResultater = vilkårsvurdering.personResultater.find {
            it.personIdent == ikkeOppfyltBarnFnr[0]
        }!!.vilkårResultater

        assertEquals(1, ikkeOppfyltBarnVilkårResultater.filter { it.resultat == Resultat.IKKE_OPPFYLT }.size)
        assertEquals(Vilkår.BOR_MED_SØKER,
                     ikkeOppfyltBarnVilkårResultater.find { it.resultat == Resultat.IKKE_OPPFYLT }!!.vilkårType)

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))

        assertEquals(0, andelTilkjentYtelser.size)
    }

    @BeforeEach
    fun initMocks() {
        every { infotrygdFeedServiceMock.sendTilInfotrygdFeed(any()) } just runs
        every { envServiceMock.skalIverksetteBehandling() } returns true
    }
}

@Configuration
class MockConfiguration {

    val now = LocalDate.now().withDayOfMonth(15)

    @Bean
    @Profile("mock-pdl-flere-barn")
    @Primary
    fun mockLocalDateService(): LocalDateService {
        val localDateServiceMock = mockk<LocalDateService>()
        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(15)
        return localDateServiceMock
    }

    @Bean
    @Profile("mock-pdl-flere-barn")
    @Primary
    fun mockPersonopplysningsService(): PersonopplysningerService {
        val personopplysningerServiceMock = mockk<PersonopplysningerService>()

        val identSlot = slot<Ident>()

        every {
            personopplysningerServiceMock.hentIdenter(capture(identSlot))
        } answers {
            listOf(IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"))
        }

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker To",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(morsfnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker Tre",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(ikkeOppfyltSøkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Gutt Barn To",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(ikkeOppfyltBarnBostedsadresse)
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
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentStatsborgerskap(Ident(morsfnr[2]))
        } returns listOf(Statsborgerskap(land = "USA", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentOpphold(morsfnr[0])
        } returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))

        every {
            personopplysningerServiceMock.hentOpphold(morsfnr[1])
        } returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))

        every {
            personopplysningerServiceMock.hentOpphold(morsfnr[2])
        } returns listOf()

        every {
            personopplysningerServiceMock.hentBostedsadresseperioder(any())
        } returns listOf()

        every {
            personopplysningerServiceMock.hentDødsfall(any())
        } returns DødsfallData(false, null)

        every {
            personopplysningerServiceMock.hentVergeData(any())
        } returns VergeData(false)

        every {
            personopplysningerServiceMock.hentStatsborgerskap(match { barnefnr.contains(it.ident) })
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = null, gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentOpphold(match { barnefnr.contains(it) })
        } returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))

        return personopplysningerServiceMock
    }

    companion object {

        val morsfnr = listOf("12445678910", "12445678911", "12345678914")
        val barnefnr = listOf("12345678911", "12345678912", "12345678913")

        val søkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 1111, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "2222")
        )

        val ikkeOppfyltSøkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 2211, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "0123")
        )

        val ikkeOppfyltBarnBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 3333, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "4444")
        )

    }

}
