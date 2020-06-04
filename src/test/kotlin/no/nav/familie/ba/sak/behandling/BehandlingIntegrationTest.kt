package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.beregning.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.task.OpphørVedtakTask
import no.nav.familie.ba.sak.task.OpphørVedtakTask.Companion.opprettOpphørVedtakTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import javax.transaction.Transactional


@SpringBootTest(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class BehandlingIntegrationTest {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var behandlingResultatRepository: BehandlingResultatRepository

    @Autowired
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    lateinit var fagsakService: FagsakService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                fagsakPersonRepository,
                persongrunnlagService,
                beregningService,
                fagsakService)

        stubFor(get(urlEqualTo("/api/aktoer/v1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.success(mapOf("aktørId" to "1"))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(LocalDate.of(2019,
                                1,
                                1)))))))
        stubFor(get(urlEqualTo("/api/personopplysning/v1/info/BAR"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.success(Personinfo(LocalDate.of(2019,
                                1,
                                1)))))))
    }

    @Test
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
        Assertions.assertEquals(1,
                behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Transactional
    fun `Opprett behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(fagsak.id, behandling.fagsak.id)
    }

    @Test
    fun `Kast feil om man lager ny behandling på fagsak som har behandling som skal godkjennes`() {
        val morId = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morId))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(morId))
        behandling.steg = StegType.BESLUTTE_VEDTAK
        behandlingRepository.saveAndFlush(behandling)

        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(NyBehandling(
                    BehandlingKategori.NASJONAL,
                    BehandlingUnderkategori.ORDINÆR,
                    morId,
                    BehandlingType.REVURDERING,
                    null))
        }
    }

    @Test
    fun `Bruk samme behandling hvis nytt barn kommer på fagsak med aktiv behandling`() {
        val morId = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        behandlingService.opprettBehandling(nyOrdinærBehandling(morId))

        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsakId = fagsak.id).size)

        behandlingService.opprettBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                morId,
                BehandlingType.REVURDERING,
                null))

        val behandlinger = behandlingService.hentBehandlinger(fagsakId = fagsak.id)
        Assertions.assertEquals(1, behandlinger.size)
    }

    @Test
    fun `Opphør migrert vedtak via task`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val stønadFom = LocalDate.of(2020, 1, 1)
        val stønadTom = stønadFom.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat =
                BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater =
                lagPersonResultaterForSøkerOgToBarn(behandlingResultat, søkerFnr, barn1Fnr, barn2Fnr, stønadFom, stønadTom)
        behandlingResultatRepository.save(behandlingResultat)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val task = opprettOpphørVedtakTask(
                behandling,
                vedtak!!,
                "saksbehandler",
                BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                LocalDate.now()
        )

        val taskRepository: TaskRepository = mockk()
        val slot = slot<Task>()

        every { taskRepository.save(capture(slot)) } answers { slot.captured }

        OpphørVedtakTask(
                vedtakService,
                taskRepository
        ).doTask(task)

        verify(exactly = 1) {
            taskRepository.save(any())
            Assertions.assertEquals("iverksettMotOppdrag", slot.captured.taskStepType)
        }

        val aktivBehandling = behandlingService.hentAktivForFagsak(behandling.fagsak.id)

        Assertions.assertEquals(BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT, aktivBehandling!!.type)
        Assertions.assertNotEquals(behandling.id, aktivBehandling.id)
    }

    @Test
    fun `Opprett barnas beregning på vedtak`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val dato_2020_10_01 = LocalDate.of(2020, 10, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val behandlingResultat =
                BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater = setOf(
                lagPersonResultat(behandlingResultat = behandlingResultat,
                        fnr = søkerFnr,
                        resultat = Resultat.JA,
                        periodeFom = dato_2020_01_01.minusMonths(1),
                        periodeTom = stønadTom,
                        lagFullstendigVilkårResultat = true,
                        personType = PersonType.SØKER
                ),
                lagPersonResultat(behandlingResultat = behandlingResultat,
                        fnr = barn1Fnr,
                        resultat = Resultat.JA,
                        periodeFom = dato_2020_01_01.minusMonths(1),
                        periodeTom = stønadTom,
                        lagFullstendigVilkårResultat = true,
                        personType = PersonType.BARN
                ),
                lagPersonResultat(behandlingResultat = behandlingResultat,
                        fnr = barn2Fnr,
                        resultat = Resultat.JA,
                        periodeFom = dato_2020_10_01.minusMonths(1),
                        periodeTom = stønadTom,
                        lagFullstendigVilkårResultat = true,
                        personType = PersonType.BARN
                )
        )
        behandlingResultatRepository.save(behandlingResultat)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it!!.personBeregninger }
                .associateBy({ it.personIdent }, { it.ytelsePerioder[0] })

        Assertions.assertEquals(2, restVedtakBarnMap.size)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!!.beløp)
        Assertions.assertEquals(dato_2020_01_01, restVedtakBarnMap[barn1Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn1Fnr]!!.stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn1Fnr]!!.ytelseType)

        Assertions.assertEquals(1054, restVedtakBarnMap[barn2Fnr]!!.beløp)
        Assertions.assertEquals(dato_2020_10_01, restVedtakBarnMap[barn2Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2020_10_01 < restVedtakBarnMap[barn2Fnr]!!.stønadTom)
        Assertions.assertEquals(YtelseType.ORDINÆR_BARNETRYGD, restVedtakBarnMap[barn2Fnr]!!.ytelseType)
    }

    @Test
    fun `Endre barnas beregning på vedtak`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val dato_2021_01_01 = LocalDate.of(2021, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr, barn3Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        Assertions.assertNotNull(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val behandlingResultat1 =
                BehandlingResultat(behandling = behandling)
        behandlingResultat1.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat1,
                søkerFnr,
                barn1Fnr,
                barn2Fnr,
                dato_2020_01_01.minusMonths(1),
                stønadTom)
        behandlingResultatRepository.save(behandlingResultat1)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)


        val behandlingResultat2 =
                BehandlingResultat(behandling = behandling)
        behandlingResultat2.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat2,
                søkerFnr,
                barn1Fnr,
                barn3Fnr,
                dato_2021_01_01.minusMonths(1),
                stønadTom)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat2, true)

        val restVedtakBarnMap = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it!!.personBeregninger }
                .associateBy({ it.personIdent }, { it.ytelsePerioder[0] })

        Assertions.assertEquals(2, restVedtakBarnMap.size)
        Assertions.assertEquals(1054, restVedtakBarnMap[barn1Fnr]!!.beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn1Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn1Fnr]!!.stønadTom)

        Assertions.assertEquals(1054, restVedtakBarnMap[barn3Fnr]!!.beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn3Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn3Fnr]!!.stønadTom)

        Assertions.assertNull(restVedtakBarnMap[barn2Fnr])
    }
}
