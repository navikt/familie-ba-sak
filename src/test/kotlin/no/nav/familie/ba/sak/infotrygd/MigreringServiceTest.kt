package no.nav.familie.ba.sak.infotrygd


import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.saksstatistikk.sakstatistikkObjectMapper
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.PubliserVedtakTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.ba.infotrygd.*
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.lang.IllegalStateException
import java.time.LocalDate


@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles("dev", "integrasjonstest", "mock-oauth", "mock-pdl", "mock-infotrygd-barnetrygd", "mock-infotrygd-feed", "mock-økonomi")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class MigreringServiceTest {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository

    @Autowired
    lateinit var iverksettMotOppdragTask: IverksettMotOppdragTask

    @Autowired
    lateinit var statusFraOppdragTask: StatusFraOppdragTask

    @Autowired
    lateinit var publiserVedtakTask: PubliserVedtakTask

    @Autowired
    lateinit var ferdigstillBehandlingTask: FerdigstillBehandlingTask

    @Autowired
    lateinit var infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

    @Autowired
    lateinit var migreringService: MigreringService

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `migrering happy case`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2408.0)), emptyList())

        migreringService.migrer(ClientMocks.søkerFnr[0], BehandlingÅrsak.NYE_OPPLYSNINGER)

        taskRepository.findAll().also { tasks ->
            assertThat(tasks).hasSize(1)
            val task = tasks.find { it.taskStepType == IverksettMotOppdragTask.TASK_STEP_TYPE }!!
            iverksettMotOppdragTask.doTask(task)
            iverksettMotOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            assertThat(tasks).hasSize(2)
            val task = tasks.find { it.taskStepType == StatusFraOppdragTask.TASK_STEP_TYPE }!!
            statusFraOppdragTask.doTask(task)
            statusFraOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            assertThat(tasks).hasSize(4)
            var task = tasks.find { it.taskStepType == FerdigstillBehandlingTask.TASK_STEP_TYPE }!!
            ferdigstillBehandlingTask.doTask(task)
            ferdigstillBehandlingTask.onCompletion(task)

            task = tasks.find { it.taskStepType == PubliserVedtakTask.TASK_STEP_TYPE }!!
            publiserVedtakTask.doTask(task)
            publiserVedtakTask.onCompletion(task)
        }
    }

    @Test
    fun `migrering skal feile dersom migrering av person allerede er påbegynt`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2408.0)), emptyList())

        migreringService.migrer(ClientMocks.søkerFnr[0], BehandlingÅrsak.NYE_OPPLYSNINGER)

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0], BehandlingÅrsak.NYE_OPPLYSNINGER)
        }.hasMessageContaining("allerede påbegynt")
    }

    @Test
    fun `migrering skal feile dersom personen allerede er migrert`() {
        run { `migrering happy case`() }

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0], BehandlingÅrsak.NYE_OPPLYSNINGER)
        }.hasMessageContaining("allerede migrert")
    }

    @Test
    fun `migrering skal feile hvis beregnet beløp ikke er lik beløp fra infotrygd`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(1054.0)), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0],
                                                     BehandlingÅrsak.NYE_OPPLYSNINGER) }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("beløp")
    }

    @Test
    fun `migrering skal feile hvis saken fra Infotrygd inneholder mer enn ett beløp`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2408.0, 660.0)), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0],
                                                     BehandlingÅrsak.NYE_OPPLYSNINGER) }.isInstanceOf(FunksjonellFeil::class.java)
                .hasMessageContaining("Fant 2")
    }

    @Test
    fun `migrering skal feile hvis saken fra Infotrygd ikke er ordinær`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2408.0).copy(valg = "UT")), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0],
                                                     BehandlingÅrsak.NYE_OPPLYSNINGER) }.isInstanceOf(FunksjonellFeil::class.java)
                .hasMessageContaining("ordinær")
    }


    @Test
    fun `migrering skal feile på, og dagen etter, kjøredato i Infotrygd`() {
        val virkningsdatoUtleder = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoUtleder.trySetAccessible()

        val migreringServiceMock = MigreringService(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(),
                                                    env = mockk(relaxed = true))  // => env.erDev() = env.erE2E() = false

        listOf<Long>(0, 1).forEach { antallDagerEtterKjøredato ->
            val kjøredato = LocalDate.now().minusDays(antallDagerEtterKjøredato)
            assertThatThrownBy { virkningsdatoUtleder.invoke(migreringServiceMock, kjøredato) }.cause
                    .hasMessageContaining("midlertidig deaktivert")
        }
    }


    @Test
    fun `virkningsdatoFra skal returnere første dag i inneværende måned når den kalles før kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            val kjøredato = this.plusDays(1)
            assertThat(virkningsdatoFra.invoke(migreringService, kjøredato)).isEqualTo(this.førsteDagIInneværendeMåned())
        }
    }

    @Test
    fun `virkningsdatoFra skal returnere første dag i neste måned, 2 dager eller mer etter kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            listOf<Long>(2, 3).forEach { antallDagerEtterKjøredato ->
                val kjøredato = this.minusDays(antallDagerEtterKjøredato)
                assertThat(virkningsdatoFra.invoke(migreringService, kjøredato)).isEqualTo(this.førsteDagINesteMåned())
            }
        }
    }

    @Test
    fun `fagsak og saksstatistikk mellomlagring skal rulles tilbake når migrering feiler`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2408.0)), emptyList())

        val antallStatistikkEventsFørVelykketMigrering = saksstatistikkMellomlagringRepository.count()
        migreringService.migrer(ClientMocks.søkerFnr[0], BehandlingÅrsak.NYE_OPPLYSNINGER)

        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(1054.0)), emptyList())

        val antallFagsakerEtterSisteVelykkedeMigrering = fagsakRepository.finnAntallFagsakerTotalt()
        val antallStatistikkEventsEtterSisteVelykkedeMigrering = saksstatistikkMellomlagringRepository.count()
                .also { assertThat(it > antallStatistikkEventsFørVelykketMigrering) }

        runCatching { migreringService.migrer(ClientMocks.søkerFnr[1], BehandlingÅrsak.NYE_OPPLYSNINGER) }
                .onSuccess { throw IllegalStateException("Testen forutsetter at migrer(...) kaster exception") }
                .onFailure {
                    val antallNyeStatistikkEvents = saksstatistikkMellomlagringRepository.count() - antallStatistikkEventsEtterSisteVelykkedeMigrering
                    assertThat(antallNyeStatistikkEvents).isZero
                    assertThat(fagsakRepository.finnAntallFagsakerTotalt()).isEqualTo(antallFagsakerEtterSisteVelykkedeMigrering)
                }
    }

    @Test
    fun `innhold i meldinger til saksstatistikk ved migrering`() {
        run { `migrering happy case`() }

        val (sakDvhMeldinger, behandlingDvhMeldinger) = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()
                .partition { it.type == SaksstatistikkMellomlagringType.SAK }.let {
                    it.first.map { objectMapper.readValue(it.json, SakDVH::class.java) } to
                            it.second.map { sakstatistikkObjectMapper.readValue(it.json, BehandlingDVH::class.java) }
                }

        assertThat(sakDvhMeldinger).extracting("sakStatus").contains(FagsakStatus.OPPRETTET.name, FagsakStatus.LØPENDE.name)

        assertThat(behandlingDvhMeldinger).extracting("behandlingStatus").contains(BehandlingStatus.UTREDES.name, BehandlingStatus.IVERKSETTER_VEDTAK.name, BehandlingStatus.AVSLUTTET.name)
        assertThat(behandlingDvhMeldinger).extracting("resultat").containsSequence(BehandlingResultat.IKKE_VURDERT.name, BehandlingResultat.INNVILGET.name)
        assertThat(behandlingDvhMeldinger).extracting("totrinnsbehandling").containsOnly(false)
        assertThat(behandlingDvhMeldinger).extracting("behandlingAarsak").containsOnly(BehandlingÅrsak.NYE_OPPLYSNINGER.name)
        assertThat(behandlingDvhMeldinger).extracting("behandlingType").containsOnly(BehandlingType.MIGRERING_FRA_INFOTRYGD.name)
        assertThat(behandlingDvhMeldinger).extracting("saksbehandler", "beslutter").containsOnly(tuple(SYSTEM_FORKORTELSE, SYSTEM_FORKORTELSE), tuple(null, null))
    }


    private fun opprettSakMedBeløp(vararg beløp: Double) = Sak(stønad = Stønad(barn = listOf(Barn(ClientMocks.barnFnr[0], barnetrygdTom = "000000"),
                                                                                             Barn(ClientMocks.barnFnr[1], barnetrygdTom = "000000")),
                                                                               delytelse = beløp.map { Delytelse(fom = LocalDate.now(),
                                                                                                                 tom = null,
                                                                                                                 beløp = it,
                                                                                                                 typeDelytelse = "MS",
                                                                                                                 typeUtbetaling = "J") }),
                                                               status = "FB",
                                                               valg = "OR",
                                                               undervalg = "OS")
}
