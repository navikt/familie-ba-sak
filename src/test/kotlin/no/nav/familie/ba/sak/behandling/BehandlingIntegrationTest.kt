package no.nav.familie.ba.sak.behandling

import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.PersonBeregning
import no.nav.familie.ba.sak.beregning.NyBeregning
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
    lateinit var vedtakPersonRepository: VedtakPersonRepository

    @Autowired
    lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var fagsakService: FagsakService

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                persongrunnlagService,
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
                fnr))
        Assertions.assertEquals(1, behandlingService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    @Tag("integration")
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(
                fnr))
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
    @Tag("integration")
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
    @Tag("integration")
    fun `Opphør migrert vedtak via task`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

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

    @Test
    @Tag("integration")
    fun `Oppdater vedtak med beregning`() {

        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val dato_2020_10_01 = LocalDate.of(2020, 10, 1)
        
        fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(søkerFnr))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        Assertions.assertNotNull(personopplysningGrunnlag)

        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "saksbehandler1")

        val personBeregninger = listOf(
                PersonBeregning(barn1Fnr,
                                1054,
                                dato_2020_01_01,
                                Ytelsetype.ORDINÆR_BARNETRYGD),
                PersonBeregning(barn2Fnr,
                                1354,
                                dato_2020_10_01,
                                Ytelsetype.ORDINÆR_BARNETRYGD)
        )
        val nyBeregning = NyBeregning(personBeregninger)

        vedtakService.oppdaterAktivVedtakMedBeregning(vedtak, personopplysningGrunnlag, nyBeregning)

        val oppdatertVedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

        val vedtakPersoner = vedtakPersonRepository.finnPersonBeregningForVedtak(oppdatertVedtak!!.id)
        Assertions.assertEquals(2,vedtakPersoner.size)

        val vedtakPersonMap = vedtakPersoner.associateBy { it.person.personIdent.ident }

        Assertions.assertEquals(1,vedtakPersonMap[barn1Fnr]!!.ytelsePerioder.size)
        Assertions.assertEquals(1054,vedtakPersonMap[barn1Fnr]!!.ytelsePerioder[0].beløp)
        Assertions.assertEquals(dato_2020_01_01, vedtakPersonMap[barn1Fnr]!!.ytelsePerioder[0].stønadFom)
        Assertions.assertTrue(dato_2020_01_01 < vedtakPersonMap[barn1Fnr]!!.ytelsePerioder[0].stønadTom)

        Assertions.assertEquals(1,vedtakPersonMap[barn2Fnr]!!.ytelsePerioder.size)
        Assertions.assertEquals(1354,vedtakPersonMap[barn2Fnr]!!.ytelsePerioder[0].beløp)
        Assertions.assertEquals(dato_2020_10_01, vedtakPersonMap[barn2Fnr]!!.ytelsePerioder[0].stønadFom)
        Assertions.assertTrue(dato_2020_10_01 < vedtakPersonMap[barn2Fnr]!!.ytelsePerioder[0].stønadTom)
    }
}
