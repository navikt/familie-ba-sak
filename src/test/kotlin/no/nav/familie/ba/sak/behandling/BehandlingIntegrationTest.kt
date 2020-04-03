package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
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
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var fagsakService: FagsakService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
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
        val barnFnr = randomFnr()

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
        val barnId = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morId))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(morId))
        behandling.steg = StegType.GODKJENNE_VEDTAK
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
        val barnId = randomFnr()
        val barn2Id = randomFnr()

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

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))
        behandlingResultatService.lagreNyOgDeaktiverGammel(lagBehandlingResultat(søkerFnr, behandling, Resultat.JA))
        behandlingResultatService.settBegrunnelseForVilkårsvurderingerPåAktiv(behandlingId = behandling.id, begrunnelse = "")

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyBeregning = NyBeregning(listOf(
                PersonBeregning(barn1Fnr,
                              1054,
                              LocalDate.of(2020, 1, 1),
                              Ytelsetype.ORDINÆR_BARNETRYGD),
                PersonBeregning(barn2Fnr,
                              1054,
                              LocalDate.of(2020, 1, 1),
                              Ytelsetype.ORDINÆR_BARNETRYGD)
        ))

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        val andelerTilkjentYtelse = mapNyBeregningTilAndelerTilkjentYtelse(behandling.id, nyBeregning, personopplysningGrunnlag)

        vedtakService.oppdaterAktivtVedtakMedBeregning(vedtak!!, andelerTilkjentYtelse)

        val task = opprettOpphørVedtakTask(
                behandling,
                vedtak, "saksbehandler",
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

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))
        behandlingResultatService.lagreNyOgDeaktiverGammel(lagBehandlingResultat(søkerFnr, behandling, Resultat.JA))
        behandlingResultatService.settBegrunnelseForVilkårsvurderingerPåAktiv(behandlingId = behandling.id, begrunnelse = "")


        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val personBeregninger = listOf(
                PersonBeregning(barn1Fnr, 1054, dato_2020_01_01, Ytelsetype.ORDINÆR_BARNETRYGD),
                PersonBeregning(barn2Fnr, 1354, dato_2020_10_01, Ytelsetype.ORDINÆR_BARNETRYGD)
        )


        val nyBeregning = NyBeregning(personBeregninger)

        val andelerTilkjentYtelse = mapNyBeregningTilAndelerTilkjentYtelse(behandling.id, nyBeregning, personopplysningGrunnlag)

        val restVedtakBarnMap = vedtakService.oppdaterAktivtVedtakMedBeregning(vedtak, andelerTilkjentYtelse)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it!!.personBeregninger }
                .associateBy({it.barn}, {it.ytelsePerioder[0]} )

        Assertions.assertEquals(2, restVedtakBarnMap.size)
        Assertions.assertEquals(1054,restVedtakBarnMap[barn1Fnr]!!.beløp)
        Assertions.assertEquals(dato_2020_01_01, restVedtakBarnMap[barn1Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < restVedtakBarnMap[barn1Fnr]!!.stønadTom)
        Assertions.assertEquals(Ytelsetype.ORDINÆR_BARNETRYGD,restVedtakBarnMap[barn1Fnr]!!.type)

        Assertions.assertEquals(1354,restVedtakBarnMap[barn2Fnr]!!.beløp)
        Assertions.assertEquals(dato_2020_10_01, restVedtakBarnMap[barn2Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2020_10_01 < restVedtakBarnMap[barn2Fnr]!!.stønadTom)
        Assertions.assertEquals(Ytelsetype.ORDINÆR_BARNETRYGD,restVedtakBarnMap[barn2Fnr]!!.type)
    }

    @Test
    fun `Endre barnas beregning på vedtak`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val dato_2020_10_01 = LocalDate.of(2020, 10, 1)
        val dato_2021_01_01 = LocalDate.of(2021, 1, 1)
        val dato_2021_10_01 = LocalDate.of(2021, 10, 1)

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))
        behandlingResultatService.lagreNyOgDeaktiverGammel(lagBehandlingResultat(søkerFnr, behandling, Resultat.JA))
        behandlingResultatService.settBegrunnelseForVilkårsvurderingerPåAktiv(behandlingId = behandling.id, begrunnelse = "")

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr,barn3Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        Assertions.assertNotNull(personopplysningGrunnlag)

        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val førsteBeregning = NyBeregning(listOf(
                PersonBeregning(barn1Fnr, 1054, dato_2020_01_01, Ytelsetype.ORDINÆR_BARNETRYGD),
                PersonBeregning(barn2Fnr, 1354, dato_2020_10_01, Ytelsetype.ORDINÆR_BARNETRYGD)
        ))

        val andelerTilkjentYtelse = mapNyBeregningTilAndelerTilkjentYtelse(behandling.id, førsteBeregning, personopplysningGrunnlag)

        vedtakService.oppdaterAktivtVedtakMedBeregning(vedtak, andelerTilkjentYtelse)

        val andreBeregning = NyBeregning(listOf(
                PersonBeregning(barn1Fnr, 970, dato_2021_01_01, Ytelsetype.MANUELL_VURDERING),
                PersonBeregning(barn3Fnr, 314, dato_2021_10_01, Ytelsetype.EØS)
        ))

        val andreAndelerTilkjentYtelse = mapNyBeregningTilAndelerTilkjentYtelse(behandling.id, andreBeregning, personopplysningGrunnlag)

        val oppdatertVedtak = vedtakRepository.findById(vedtak.id).get()

        val restVedtakBarnMap = vedtakService.oppdaterAktivtVedtakMedBeregning(oppdatertVedtak, andreAndelerTilkjentYtelse)
                .data!!.behandlinger
                .flatMap { it.vedtakForBehandling }
                .flatMap { it!!.personBeregninger }
                .associateBy({it.barn}, {it.ytelsePerioder[0]} )

        Assertions.assertEquals(2, restVedtakBarnMap.size)
        Assertions.assertEquals(970,restVedtakBarnMap[barn1Fnr]!!.beløp)
        Assertions.assertEquals(dato_2021_01_01, restVedtakBarnMap[barn1Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2021_01_01 < restVedtakBarnMap[barn1Fnr]!!.stønadTom)
        Assertions.assertEquals(Ytelsetype.MANUELL_VURDERING,restVedtakBarnMap[barn1Fnr]!!.type)

        Assertions.assertEquals(314,restVedtakBarnMap[barn3Fnr]!!.beløp)
        Assertions.assertEquals(dato_2021_10_01, restVedtakBarnMap[barn3Fnr]!!.stønadFom)
        Assertions.assertTrue(dato_2021_10_01 < restVedtakBarnMap[barn3Fnr]!!.stønadTom)
        Assertions.assertEquals(Ytelsetype.EØS,restVedtakBarnMap[barn3Fnr]!!.type)
    }
}
