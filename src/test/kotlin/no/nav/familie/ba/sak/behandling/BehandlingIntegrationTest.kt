package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
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
    @Tag("integration")
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr,
                listOf(barnFnr)))
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr,
                listOf(randomFnr(),
                       randomFnr()
                )))
        Assertions.assertEquals(1,
                                behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    @Transactional
    fun `Opprett behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        Assertions.assertEquals(fagsak.id, behandling.fagsak.id)
    }

    @Test
    @Tag("integration")
    fun `Kast feil om man lager ny behandling på fagsak som har behandling som skal godkjennes`() {
        val morId = randomFnr()
        val barnId = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morId))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(morId, listOf(barnId)))
        behandling.steg = StegType.GODKJENNE_VEDTAK
        behandlingRepository.saveAndFlush(behandling)

        Assertions.assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(NyBehandling(
                    BehandlingKategori.NASJONAL,
                    BehandlingUnderkategori.ORDINÆR,
                    morId,
                    listOf(barnId),
                    BehandlingType.REVURDERING,
                    null))
        }
    }

    @Test
    @Tag("integration")
    fun `Bruk samme behandling hvis nytt barn kommer på fagsak med aktiv behandling`() {
        val morId = randomFnr()
        val barnId = randomFnr()
        val barn2Id = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        behandlingService.opprettBehandling(nyOrdinærBehandling(morId, listOf(barnId)))

        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsakId = fagsak.id).size)

        behandlingService.opprettBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                morId,
                listOf(barn2Id),
                BehandlingType.REVURDERING,
                null))

        val behandlinger = behandlingService.hentBehandlinger(fagsakId = fagsak.id)
        Assertions.assertEquals(1, behandlinger.size)
    }

    @Test
    @Tag("integration")
    fun `Sett riktig gjeldende behandling ved revurdering`() {
        val morId = randomFnr()
        val barnId = randomFnr()
        val vedtakDato = LocalDate.now()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(morId, listOf(barnId)))
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForFGB(
                morId,
                fagsak.id.toString(),
                behandling.id,
                vedtakDato.minusMonths(2).withDayOfMonth(1),
                vedtakDato.plusMonths(11),
                vedtakDato
        )
        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)

        val revurdering = behandlingService.opprettBehandling(nyRevurdering(morId, listOf(barnId)))
        val utbetalingsoppdragRevurdering = lagTestUtbetalingsoppdragForRevurdering(
                morId,
                fagsak.id.toString(),
                revurdering.id,
                vedtakDato,
                vedtakDato.minusMonths(2).withDayOfMonth(1),
                vedtakDato.plusMonths(11),
                vedtakDato.withDayOfMonth(1)
        )
        beregningService.lagreBeregningsresultat(revurdering, utbetalingsoppdragRevurdering)
        behandlingService.oppdaterStatusPåBehandling(revurdering.id, BehandlingStatus.IVERKSATT)

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertNotNull(gjeldendeBehandling)
        Assertions.assertEquals(revurdering.id, gjeldendeBehandling!!.id)
    }

    @Test
    @Tag("integration")
    fun `Opphør migrert vedtak via task`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr, listOf(barn1Fnr, barn2Fnr)))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        Assertions.assertNotNull(personopplysningGrunnlag)

        val personBeregninger = listOf(
                PersonBeregning(barn1Fnr,
                              1054,
                              LocalDate.of(2020, 1, 1),
                              Ytelsetype.ORDINÆR_BARNETRYGD),
                PersonBeregning(barn2Fnr,
                              1054,
                              LocalDate.of(2020, 1, 1),
                              Ytelsetype.ORDINÆR_BARNETRYGD)
        )
        val nyBeregning = NyBeregning(personBeregninger)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)

        vedtakService.oppdaterAktivVedtakMedBeregning(vedtak!!, personopplysningGrunnlag, nyBeregning)

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
}
