package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.*
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.fødselshendelse.MockConfiguration.Companion.barnefnr
import no.nav.familie.ba.sak.behandling.fødselshendelse.MockConfiguration.Companion.morsfnr
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.*
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Resultat
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
import java.time.LocalDate.of
import java.time.YearMonth

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("dev", "mock-dokgen", "mock-oauth", "mock-pdl-flere-barn")
@Tag("integration")
class FødselshendelseIntegrasjonTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val behandlingResultatRepository: BehandlingResultatRepository,

        @Autowired
        private val taskRepository: TaskRepository,

        @Autowired
        private val evaluerFiltreringsreglerForFødselshendelse: EvaluerFiltreringsreglerForFødselshendelse,

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
) {

    val now = LocalDate.now()

    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    val featureToggleServiceMock = mockk<FeatureToggleService>()
    val vilkårsvurderingMetricsMock = mockk<VilkårsvurderingMetrics>()


    val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                        infotrygdBarnetrygdClientMock,
                                                        featureToggleServiceMock,
                                                        stegService,
                                                        vedtakService,
                                                        evaluerFiltreringsreglerForFødselshendelse,
                                                        taskRepository,
                                                        personopplysningerService,
                                                        behandlingResultatRepository,
                                                        persongrunnlagService,
                                                        behandlingRepository,
                                                        vilkårsvurderingMetricsMock)

    @Test
    fun `Fødselshendelse med flere barn med oppfylt vilkårsvurdering skal håndteres riktig`() {
        val oppfyltBarnFnr = listOf(barnefnr[0], barnefnr[1])

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(NyBehandlingHendelse(
                morsfnr[0], morsfnr[0], oppfyltBarnFnr
        ))
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsfnr[0]))
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak!!.id)
        val behandlingResultater = behandlingResultatRepository.finnBehandlingResultater(behandling!!.id)

        Assert.assertEquals(1, behandlingResultater.size)

        val behandlingResultat = behandlingResultater.get(0)

        Assert.assertEquals(BehandlingResultatType.INNVILGET, behandlingResultat.hentSamletResultat())
        Assert.assertEquals(true, behandlingResultat.aktiv)
        Assert.assertEquals(3, behandlingResultat.personResultater.size)

        Assert.assertTrue(behandlingResultat.personResultater.all {
            it.vilkårResultater.all {
                it.resultat == Resultat.JA
            }
        })

        Assert.assertTrue(behandlingResultat.personResultater.map { it.personIdent }.containsAll(
                oppfyltBarnFnr.plus(morsfnr[0])
        ))

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))
        val satsOrdinær = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.now(), YearMonth.now()).first()
        val satsTillegg = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.now(), YearMonth.now()).first()

        Assert.assertEquals(4, andelTilkjentYtelser.size)
        Assert.assertEquals(2, andelTilkjentYtelser.filter { it.beløp == satsOrdinær.beløp }.size)
        Assert.assertEquals(2, andelTilkjentYtelser.filter { it.beløp == satsTillegg.beløp }.size)

        val reffom = now
        val reftom = now.plusYears(18).minusMonths(2)
        val fom = of(reffom.year, reffom.month, 1)
        val tom = of(reftom.year, reftom.month, reftom.lengthOfMonth())

        val (barn1, barn2) = andelTilkjentYtelser.partition { it.personIdent == barnefnr[0] }
        Assert.assertEquals(fom, barn1.minByOrNull { it.stønadFom }!!.stønadFom)
        Assert.assertEquals(tom, barn1.maxByOrNull { it.stønadTom }!!.stønadTom)
        Assert.assertEquals(fom, barn2.minByOrNull { it.stønadFom }!!.stønadFom)
        Assert.assertEquals(tom, barn2.maxByOrNull { it.stønadTom }!!.stønadTom)
    }

    @Test
    fun `Fødselshendelse med flere barn som ikke oppfyl vilkår skal håndteres riktig`() {
        val ikkeOppfyltBarnFnr = listOf(barnefnr[0], barnefnr[2])

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(NyBehandlingHendelse(
                morsfnr[1], morsfnr[1], ikkeOppfyltBarnFnr
        ))
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsfnr[1]))
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak!!.id)
        val behandlingResultater = behandlingResultatRepository.finnBehandlingResultater(behandling!!.id)

        Assert.assertEquals(1, behandlingResultater.size)

        val behandlingResultat = behandlingResultater.get(0)

        Assert.assertEquals(BehandlingResultatType.AVSLÅTT, behandlingResultat.hentSamletResultat())
        Assert.assertEquals(true, behandlingResultat.aktiv)
        Assert.assertEquals(3, behandlingResultat.personResultater.size)
        Assert.assertTrue(behandlingResultat.personResultater.map { it.personIdent }.containsAll(
                ikkeOppfyltBarnFnr.plus(morsfnr[1])
        ))

        val ikkeOppfyltBarnVilkårResultater = behandlingResultat.personResultater.find {
            it.personIdent == ikkeOppfyltBarnFnr[1]
        }!!.vilkårResultater

        Assert.assertEquals(1, ikkeOppfyltBarnVilkårResultater.filter { it.resultat == Resultat.NEI }.size)
        Assert.assertEquals(Vilkår.BOR_MED_SØKER,
                            ikkeOppfyltBarnVilkårResultater.find { it.resultat == Resultat.NEI }!!.vilkårType)

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))

        Assert.assertEquals(2, andelTilkjentYtelser.size)
        val satsOrdinær = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.now(), YearMonth.now()).first()
        val satsTillegg = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.now(), YearMonth.now()).first()

        Assert.assertEquals(1, andelTilkjentYtelser.filter { it.beløp == satsOrdinær.beløp }.size)
        Assert.assertEquals(1, andelTilkjentYtelser.filter { it.beløp == satsTillegg.beløp }.size)

        val reffom = now
        val reftom = now.plusYears(18).minusMonths(2)
        val fom = of(reffom.year, reffom.month, 1)
        val tom = of(reftom.year, reftom.month, reftom.lengthOfMonth())

        Assert.assertEquals(fom, andelTilkjentYtelser.minByOrNull { it.stønadFom }!!.stønadFom)
        Assert.assertEquals(tom, andelTilkjentYtelser.maxByOrNull { it.stønadTom }!!.stønadTom)
        Assert.assertEquals(ikkeOppfyltBarnFnr[0], andelTilkjentYtelser[0].personIdent)
    }

    @BeforeEach
    fun initMocks() {
        every { infotrygdFeedServiceMock.sendTilInfotrygdFeed(any()) } returns Unit
        every { featureToggleServiceMock.isEnabled(any()) } returns false
        every {vilkårsvurderingMetricsMock.
        økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(any(), any())} just(runs)
    }
}

@Configuration
class MockConfiguration {

    val now = LocalDate.now()

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
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

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
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
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
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnefnr[2])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(1),
                navn = "Gutt Barn To",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = ikkeOppfyltBarnBostedsadresse
        )

        val hentAktørIdIdentSlot = slot<Ident>()
        every {
            personopplysningerServiceMock.hentAktivAktørId(capture(hentAktørIdIdentSlot))
        } answers {
            AktørId(id = "0${hentAktørIdIdentSlot.captured.ident}")
        }

        every {
            personopplysningerServiceMock.hentStatsborgerskap(any())
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

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
        val barnefnr = listOf("12345678911", "12345678912", "12345678913")

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
