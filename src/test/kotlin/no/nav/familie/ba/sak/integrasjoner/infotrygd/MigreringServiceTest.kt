package no.nav.familie.ba.sak.integrasjoner.infotrygd


import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ba.sak.statistikk.producer.MockKafkaProducer
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.statistikk.saksstatistikk.sakstatistikkObjectMapper
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.PubliserVedtakTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.ba.infotrygd.Barn
import no.nav.familie.kontrakter.ba.infotrygd.Delytelse
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
class MigreringServiceTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var taskRepository: TaskRepositoryWrapper

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

    @Autowired
    lateinit var vilkårService: VilkårService

    @BeforeEach
    fun init() {
        MockKafkaProducer.sendteMeldinger.clear()
        databaseCleanupService.truncate()
        every { infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(any(), any()) } returns false
    }

    @Test
    fun `migrering happy case`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0)), emptyList())

        migreringService.migrer(ClientMocks.søkerFnr[0])

        taskRepository.findAll().also { tasks ->
            assertThat(tasks).hasSize(1)
            val task = tasks.find { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }!!
            iverksettMotOppdragTask.doTask(task)
            iverksettMotOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            assertThat(tasks).hasSize(2)
            val task = tasks.find { it.type == StatusFraOppdragTask.TASK_STEP_TYPE }!!
            statusFraOppdragTask.doTask(task)
            statusFraOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            assertThat(tasks).hasSize(4)
            var task = tasks.find { it.type == FerdigstillBehandlingTask.TASK_STEP_TYPE }!!
            ferdigstillBehandlingTask.doTask(task)
            ferdigstillBehandlingTask.onCompletion(task)

            task = tasks.find { it.type == PubliserVedtakTask.TASK_STEP_TYPE }!!
            publiserVedtakTask.doTask(task)
            publiserVedtakTask.onCompletion(task)

            val now = LocalDate.now()
            val forventetUtbetalingFom: LocalDate =
                    if (infotrygdKjøredato().isAfter(now)) now.førsteDagIInneværendeMåned() else now.førsteDagINesteMåned()

            val vedtakDVH = MockKafkaProducer.sendteMeldinger.values.first() as VedtakDVH
            assertThat(vedtakDVH.utbetalingsperioder.first().stønadFom).isEqualTo(forventetUtbetalingFom)
        }
    }

    @Test
    fun`skal sette periodeFom til barnas fødselsdatoer på vilkårene som skal gjelde fra fødselsdato`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0)), emptyList())

        val responseDto = migreringService.migrer(ClientMocks.søkerFnr[0])

        val barnasFødselsdatoer = ClientMocks.barnFnr.map { LocalDate.parse(it.subSequence(0, 6), DateTimeFormatter.ofPattern("ddMMyy")) }
        val vilkårResultater = vilkårService.hentVilkårsvurdering(behandlingId = responseDto.behandlingId)!!.personResultater.flatMap { it.vilkårResultater }
        assertThat(vilkårResultater.filter { it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() })
                .extracting("periodeFom")
                .hasSameElementsAs(barnasFødselsdatoer)
    }

    @Test
    fun `migrering skal feile dersom migrering av person allerede er påbegynt`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0)), emptyList())

        migreringService.migrer(ClientMocks.søkerFnr[0])

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.hasMessageContaining("allerede påbegynt")
    }

    @Test
    fun `migrering skal feile dersom personen allerede er migrert`() {
        run { `migrering happy case`() }

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.hasFieldOrProperty("frontendFeilmelding")
                .hasMessageContaining("allerede migrert")
    }

    @Test
    fun `migrering skal avbrytes med feilmelding dersom personen har en åpen sak i Infotrygd`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0)), emptyList())
        every {
            infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(any(), any())
        } returns true

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.hasFieldOrProperty("frontendFeilmelding")
                .hasMessageContaining("sak i Infotrygd")
    }

    @Test
    fun `migrering skal feile hvis beregnet beløp ikke er lik beløp fra infotrygd`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(1354.0)), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("beløp")
    }

    @Test
    fun `migrering skal feile hvis saken fra Infotrygd inneholder mer enn ett beløp`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0, 660.0)), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }.isInstanceOf(FunksjonellFeil::class.java)
                .hasMessageContaining("Fant 2")
    }

    @Test
    fun `migrering skal feile hvis saken fra Infotrygd ikke er ordinær`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0).copy(valg = "UT")), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }.isInstanceOf(FunksjonellFeil::class.java)
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
    fun `virkningsdatoFra skal returnere første dag i inneværende måned - minus en måned - når den kalles før kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            val kjøredato = this.plusDays(1)
            assertThat(virkningsdatoFra.invoke(migreringService, kjøredato)).isEqualTo(this.førsteDagIInneværendeMåned().minusMonths(1))
        }
    }

    @Test
    fun `virkningsdatoFra skal returnere første dag i inneværende måned, 2 dager eller mer etter kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            listOf<Long>(2, 3).forEach { antallDagerEtterKjøredato ->
                val kjøredato = this.minusDays(antallDagerEtterKjøredato)
                assertThat(virkningsdatoFra.invoke(migreringService, kjøredato)).isEqualTo(this.førsteDagIInneværendeMåned())
            }
        }
    }

    @Test
    fun `fagsak og saksstatistikk mellomlagring skal rulles tilbake når migrering feiler`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(2708.0)), emptyList())

        val antallStatistikkEventsFørVelykketMigrering = saksstatistikkMellomlagringRepository.count()
        migreringService.migrer(ClientMocks.søkerFnr[0])

        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
                InfotrygdSøkResponse(listOf(opprettSakMedBeløp(1354.0)), emptyList())

        val antallFagsakerEtterSisteVelykkedeMigrering = fagsakRepository.finnAntallFagsakerTotalt()
        val antallStatistikkEventsEtterSisteVelykkedeMigrering = saksstatistikkMellomlagringRepository.count()
                .also { assertThat(it > antallStatistikkEventsFørVelykketMigrering) }

        runCatching { migreringService.migrer(ClientMocks.søkerFnr[1]) }
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
        assertThat(behandlingDvhMeldinger).extracting("behandlingAarsak").containsOnly(BehandlingÅrsak.MIGRERING.name)
        assertThat(behandlingDvhMeldinger).extracting("behandlingType").containsOnly(BehandlingType.MIGRERING_FRA_INFOTRYGD.name)
        assertThat(behandlingDvhMeldinger).extracting("saksbehandler", "beslutter").containsOnly(tuple(SYSTEM_FORKORTELSE, SYSTEM_FORKORTELSE), tuple(null, null))
    }


    private fun opprettSakMedBeløp(vararg beløp: Double) = Sak(stønad = Stønad(barn = listOf(Barn(ClientMocks.barnFnr[0], barnetrygdTom = "000000"),
                                                                                             Barn(ClientMocks.barnFnr[1], barnetrygdTom = "000000")),
                                                                               delytelse = beløp.map { Delytelse(fom = LocalDate.now(),
                                                                                                                 tom = null,
                                                                                                                 beløp = it,
                                                                                                                 typeDelytelse = "MS",
                                                                                                                 typeUtbetaling = "J",) },
    opphørsgrunn = "0"),
                                                               status = "FB",
                                                               valg = "OR",
                                                               undervalg = "OS")

    private fun infotrygdKjøredato(): LocalDate {
        return MigreringService::class.java.getDeclaredMethod("infotrygdKjøredato", YearMonth::class.java).run {
            this.trySetAccessible()
            this.invoke(migreringService, YearMonth.now()) as LocalDate
        }
    }
}
