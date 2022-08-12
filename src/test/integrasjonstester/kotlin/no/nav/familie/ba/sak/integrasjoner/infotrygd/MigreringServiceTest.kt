package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractMockkSpringRunner
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
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
import no.nav.familie.ba.sak.task.PubliserVedtakV2Task
import no.nav.familie.ba.sak.task.SendVedtakTilInfotrygdTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.ba.infotrygd.Barn
import no.nav.familie.kontrakter.ba.infotrygd.Delytelse
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "mock-økonomi",
    "mock-pdl",
    "mock-pdl-client",
    "mock-ident-client",
    "mock-infotrygd-barnetrygd",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-infotrygd-feed",
    "mock-oauth",
    "mock-rest-template-config"
)
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MigreringServiceTest(
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val taskRepository: TaskRepositoryWrapper,

    @Autowired
    private val fagsakRepository: FagsakRepository,

    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,

    @Autowired
    private val iverksettMotOppdragTask: IverksettMotOppdragTask,

    @Autowired
    private val statusFraOppdragTask: StatusFraOppdragTask,

    @Autowired
    private val sendVedtakTilInfotrygdTask: SendVedtakTilInfotrygdTask,

    @Autowired
    private val publiserVedtakTask: PubliserVedtakTask,

    @Autowired
    private val publiserVedtakV2Task: PubliserVedtakV2Task,

    @Autowired
    private val ferdigstillBehandlingTask: FerdigstillBehandlingTask,

    @Autowired
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,

    @Autowired
    private val migreringService: MigreringService,

    @Autowired
    private val vilkårService: VilkårService,

    @Autowired
    private val pdlRestClient: PdlRestClient,

    @Autowired
    private val envService: EnvService,

) : AbstractMockkSpringRunner() {

    lateinit var migreringServiceMock: MigreringService

    @BeforeEach
    fun init() {
        MockKafkaProducer.sendteMeldinger.clear()
        databaseCleanupService.truncate()
        every { infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(any(), any()) } returns false

        val slotAktør = slot<Aktør>()

        every { pdlRestClient.hentForelderBarnRelasjoner(capture(slotAktør)) } answers {
            infotrygdBarnetrygdClient.hentSaker(listOf(slotAktør.captured.aktivFødselsnummer())).bruker.first().stønad!!.barn.map {
                ForelderBarnRelasjon(
                    relatertPersonsIdent = it.barnFnr!!,
                    relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN
                )
            }
        }
        every { envService.erPreprod() } returns false
        every { envService.erProd() } returns false

        val envServiceMock = mockk<EnvService>()
        every { envServiceMock.erPreprod() } returns false
        every { envServiceMock.erDev() } returns false
        migreringServiceMock = MigreringService(
            mockk(),
            mockk(),
            env = envServiceMock,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true),
        ) // => env.erDev() = env.erE2E() = false
    }

    @Test
    fun `migrering av ordinær sak - happy case`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())

        val migreringResponseDto = migreringService.migrer(ClientMocks.søkerFnr[0])

        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }!!
            iverksettMotOppdragTask.doTask(task)
            iverksettMotOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == SendVedtakTilInfotrygdTask.TASK_STEP_TYPE }!!
            sendVedtakTilInfotrygdTask.doTask(task)
            sendVedtakTilInfotrygdTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == StatusFraOppdragTask.TASK_STEP_TYPE }!!
            statusFraOppdragTask.doTask(task)
            statusFraOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
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
            assertThat(migreringResponseDto.virkningFom).isEqualTo(forventetUtbetalingFom.toYearMonth())

            task = tasks.find { it.type == PubliserVedtakV2Task.TASK_STEP_TYPE }!!
            publiserVedtakV2Task.doTask(task)
            publiserVedtakV2Task.onCompletion(task)

            val vedtakDVHV2 = MockKafkaProducer.sendteMeldinger.values.last() as VedtakDVHV2
            assertThat(vedtakDVHV2.utbetalingsperioderV2.first().stønadFom).isEqualTo(forventetUtbetalingFom)
        }
    }

    @Test
    fun `migrering av ordinær sak - happy case - ikke eget barn`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())
        val slotAktør = slot<Aktør>()
        every { pdlRestClient.hentForelderBarnRelasjoner(capture(slotAktør)) } returns emptyList()

        val migreringResponseDto = migreringService.migrer(ClientMocks.søkerFnr[0])

        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }!!
            iverksettMotOppdragTask.doTask(task)
            iverksettMotOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == SendVedtakTilInfotrygdTask.TASK_STEP_TYPE }!!
            sendVedtakTilInfotrygdTask.doTask(task)
            sendVedtakTilInfotrygdTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == StatusFraOppdragTask.TASK_STEP_TYPE }!!
            statusFraOppdragTask.doTask(task)
            statusFraOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
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
            assertThat(migreringResponseDto.virkningFom).isEqualTo(forventetUtbetalingFom.toYearMonth())

            task = tasks.find { it.type == PubliserVedtakV2Task.TASK_STEP_TYPE }!!
            publiserVedtakV2Task.doTask(task)
            publiserVedtakV2Task.onCompletion(task)

            val vedtakDVHV2 = MockKafkaProducer.sendteMeldinger.values.last() as VedtakDVHV2
            assertThat(vedtakDVHV2.utbetalingsperioderV2.first().stønadFom).isEqualTo(forventetUtbetalingFom)
        }
    }

    @Test
    fun `migrering av ordinær sak med delt bosted - enkel case`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6 / 2).copy(undervalg = "MD")),
            emptyList()
        )

        val migreringResponseDto = migreringService.migrer(ClientMocks.søkerFnr[0])

        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }!!
            iverksettMotOppdragTask.doTask(task)
            iverksettMotOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == SendVedtakTilInfotrygdTask.TASK_STEP_TYPE }!!
            sendVedtakTilInfotrygdTask.doTask(task)
            sendVedtakTilInfotrygdTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == StatusFraOppdragTask.TASK_STEP_TYPE }!!
            statusFraOppdragTask.doTask(task)
            statusFraOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
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
            assertThat(vedtakDVH.utbetalingsperioder.first().utbetaltPerMnd.toDouble()).isEqualTo(
                SAK_BELØP_2_BARN_1_UNDER_6 / 2
            )
            assertThat(migreringResponseDto.virkningFom).isEqualTo(forventetUtbetalingFom.toYearMonth())

            task = tasks.find { it.type == PubliserVedtakV2Task.TASK_STEP_TYPE }!!
            publiserVedtakV2Task.doTask(task)
            publiserVedtakV2Task.onCompletion(task)

            val vedtakDVHV2 = MockKafkaProducer.sendteMeldinger.values.last() as VedtakDVHV2
            assertThat(vedtakDVHV2.utbetalingsperioderV2.first().stønadFom).isEqualTo(forventetUtbetalingFom)
            assertThat(vedtakDVHV2.utbetalingsperioderV2.first().utbetaltPerMnd.toDouble()).isEqualTo(
                SAK_BELØP_2_BARN_1_UNDER_6 / 2
            )
        }
    }

    @Test
    fun `migrering av utvidet sak - happy case`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(opprettUtvidetSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6_UTVIDET)),
            emptyList()
        )

        val migreringResponseDto = migreringService.migrer(ClientMocks.søkerFnr[0])

        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }!!
            iverksettMotOppdragTask.doTask(task)
            iverksettMotOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == SendVedtakTilInfotrygdTask.TASK_STEP_TYPE }!!
            sendVedtakTilInfotrygdTask.doTask(task)
            sendVedtakTilInfotrygdTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
            val task = tasks.find { it.type == StatusFraOppdragTask.TASK_STEP_TYPE }!!
            statusFraOppdragTask.doTask(task)
            statusFraOppdragTask.onCompletion(task)
        }
        taskRepository.findAll().also { tasks ->
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
            assertThat(migreringResponseDto.virkningFom).isEqualTo(forventetUtbetalingFom.toYearMonth())

            task = tasks.find { it.type == PubliserVedtakV2Task.TASK_STEP_TYPE }!!
            publiserVedtakV2Task.doTask(task)
            publiserVedtakV2Task.onCompletion(task)

            val vedtakDVHV2 = MockKafkaProducer.sendteMeldinger.values.last() as VedtakDVHV2
            assertThat(vedtakDVHV2.utbetalingsperioderV2.first().stønadFom).isEqualTo(forventetUtbetalingFom)
        }
    }

    @Test
    fun `skal sette periodeFom til barnas fødselsdatoer på vilkårene som skal gjelde fra fødselsdato`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())

        val responseDto = migreringService.migrer(ClientMocks.søkerFnr[0])

        val barnasFødselsdatoer =
            ClientMocks.barnFnr.map { LocalDate.parse(it.subSequence(0, 6), DateTimeFormatter.ofPattern("ddMMyy")) }
        val vilkårResultater =
            vilkårService.hentVilkårsvurdering(behandlingId = responseDto.behandlingId)!!.personResultater.flatMap { it.vilkårResultater }
        assertThat(vilkårResultater.filter { it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() })
            .extracting("periodeFom")
            .hasSameElementsAs(barnasFødselsdatoer)
    }

    @Test
    fun `migrering skal feile dersom person registrert på stønad er også registrert som barn, Det er da mest sannsynlig institusjon`() {
        val fødselsnrBarn = FoedselsnummerGenerator().foedselsnummer(LocalDate.now()).asString

        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(
                Sak(
                    stønad = Stønad(
                        barn = listOf(
                            Barn(
                                fødselsnrBarn,
                                barnetrygdTom = "000000"
                            )
                        ),
                        antallBarn = 1,
                        delytelse = listOf(
                            Delytelse(
                                fom = LocalDate.now(),
                                tom = null,
                                beløp = 2048.0,
                                typeDelytelse = "MS",
                                typeUtbetaling = "J",
                            )
                        ),
                        opphørsgrunn = "0"
                    ),
                    status = "FB",
                    valg = "OR",
                    undervalg = "OS"
                )

            ),
            emptyList()
        )

        assertThatThrownBy {
            migreringService.migrer(fødselsnrBarn)
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.INSTITUSJON)
    }

    @Test
    fun `migrering skal feile dersom løpende sak i infotrygd er av type I, Det er da mest sannsynlig institusjon`() {
        val fødselsnrBarn = FoedselsnummerGenerator().foedselsnummer(LocalDate.now()).asString

        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(
                Sak(
                    stønad = Stønad(
                        barn = listOf(
                            Barn(
                                FoedselsnummerGenerator().foedselsnummer(LocalDate.now()).asString,
                                barnetrygdTom = "000000"
                            )
                        ),
                        antallBarn = 1,
                        delytelse = listOf(
                            Delytelse(
                                fom = LocalDate.now(),
                                tom = null,
                                beløp = 2048.0,
                                typeDelytelse = "MS",
                                typeUtbetaling = "J",
                            )
                        ),
                        opphørsgrunn = "0",
                    ),
                    status = "FB",
                    valg = "OR",
                    undervalg = "OS",
                    type = "I"
                )

            ),
            emptyList()
        )

        assertThatThrownBy {
            migreringService.migrer(fødselsnrBarn)
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.INSTITUSJON)
    }

    @Test
    fun `happy case - personer over 18 skal ignoreres hvis antallBarn på stønaden stemmer overens med antall barn etter at de over at er filtrert vekk`() {
        val fødselsnrBarn =
            FoedselsnummerGenerator().foedselsnummer(LocalDate.now().minusYears(18)).asString

        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(
                Sak(
                    // stønad med 2 barn, men kun 1 under 18
                    stønad = Stønad(
                        barn = listOf(
                            Barn(
                                fødselsnrBarn,
                                barnetrygdTom = "000000"
                            ),
                            Barn(ClientMocks.barnFnr[0], barnetrygdTom = "000000"),
                        ),
                        antallBarn = 1,
                        delytelse = listOf(
                            Delytelse(
                                fom = LocalDate.now(),
                                tom = null,
                                beløp = 1054.0,
                                typeDelytelse = "MS",
                                typeUtbetaling = "J",
                            )
                        ),
                        opphørsgrunn = "0"
                    ),
                    status = "FB",
                    valg = "OR",
                    undervalg = "OS"
                )

            ),
            emptyList()
        )

        migreringService.migrer(ClientMocks.søkerFnr[0])
    }

    @Test
    fun `Migrerin skal stoppes hvis antall barn på stønad ikke stemmer overens med antall barn under 18, når person har 1 barn over 18`() {
        val fødselsnrBarn =
            FoedselsnummerGenerator().foedselsnummer(LocalDate.now().minusYears(18)).asString

        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(
                Sak(
                    // stønad med 2 barn, men kun 1 under 18
                    stønad = Stønad(
                        barn = listOf(
                            Barn(
                                fødselsnrBarn,
                                barnetrygdTom = "000000"
                            ),
                            Barn(ClientMocks.barnFnr[0], barnetrygdTom = "000000"),
                        ),
                        antallBarn = 2,
                        delytelse = listOf(
                            Delytelse(
                                fom = LocalDate.now(),
                                tom = null,
                                beløp = 1054.0,
                                typeDelytelse = "MS",
                                typeUtbetaling = "J",
                            )
                        ),
                        opphørsgrunn = "0"
                    ),
                    status = "FB",
                    valg = "OR",
                    undervalg = "OS"
                )

            ),
            emptyList()
        )
        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.OPPGITT_ANTALL_BARN_ULIKT_ANTALL_BARNIDENTER)
    }

    @Test
    fun `Migrerin skal stoppes hvis antall delytelser er null og antall barn er 0`() {
        val fødselsnrBarn =
            FoedselsnummerGenerator().foedselsnummer(LocalDate.now().minusYears(18)).asString

        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(
            listOf(
                Sak(
                    // stønad med 2 barn, men kun 1 under 18
                    stønad = Stønad(
                        barn = emptyList(),
                        antallBarn = 0,
                        delytelse = emptyList(),
                        opphørsgrunn = "0"
                    ),
                    status = "FB",
                    valg = "OR",
                    undervalg = "OS"
                )

            ),
            emptyList()
        )
        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.DELYTELSE_OG_ANTALLBARN_NULL)
    }

    @Test
    fun `migrering skal feile dersom migrering av person allerede er påbegynt`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())

        migreringService.migrer(ClientMocks.søkerFnr[0])

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.MIGRERING_ALLEREDE_PÅBEGYNT)
    }

    @Test
    fun `migrering skal feile dersom personen allerede er migrert`() {
        run { `migrering av ordinær sak - happy case`() }

        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }.isInstanceOf(KanIkkeMigrereException::class.java)

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.ALLEREDE_MIGRERT)
    }

    @Test
    fun `migrering skal avbrytes med feilmelding dersom opprett behandling feiler`() {
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())
        every {
            infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(any(), any())
        } returns true

        assertThatThrownBy {
            migreringService.migrer(ClientMocks.søkerFnr[0])
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage("Kan ikke lage behandling på person med aktiv sak i Infotrygd")
            .extracting("feiltype").isEqualTo(MigreringsfeilType.KAN_IKKE_OPPRETTE_BEHANDLING)
    }

    @Test
    fun `migrering skal feile hvis beregnet beløp ikke er lik beløp fra infotrygd`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
            InfotrygdSøkResponse(listOf(opprettSakMedBeløp(1354.0)), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }
            .isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessageContaining("1354")
            .extracting("feiltype")
            .isEqualTo(MigreringsfeilType.BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD)
    }

    @Test
    fun `migrering skal feile hvis saken fra Infotrygd inneholder mer enn ett beløp`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
            InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6, 660.0)), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage("Fant ugylding antall delytelser 2")
            .extracting("feiltype").isEqualTo(MigreringsfeilType.UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD)
    }

    @Test
    fun `migrering skal feile hvis saken fra Infotrygd ikke er ordinær`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
            InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6).copy(valg = "UT")), emptyList())
        assertThatThrownBy { migreringService.migrer(ClientMocks.søkerFnr[0]) }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.IKKE_STØTTET_SAKSTYPE)
    }

    @Test
    fun `migrering skal feile på, og dagen etter, kjøredato i Infotrygd`() {
        val virkningsdatoUtleder =
            MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoUtleder.trySetAccessible()

        listOf<Long>(0, 1).forEach { antallDagerEtterKjøredato ->
            val kjøredato = LocalDate.now().minusDays(antallDagerEtterKjøredato)

            assertThatThrownBy { virkningsdatoUtleder.invoke(migreringServiceMock, kjøredato) }
                .cause.isInstanceOf(KanIkkeMigrereException::class.java)
                .hasMessageContaining("midlertidig deaktivert")
                .extracting("feiltype").isEqualTo(MigreringsfeilType.IKKE_GYLDIG_KJØREDATO)
        }
    }

    @Test
    fun `virkningsdatoFra skal returnere første dag i inneværende måned - minus en måned - når den kalles før kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            val kjøredato = this.plusDays(1)
            assertThat(virkningsdatoFra.invoke(migreringServiceMock, kjøredato)).isEqualTo(
                this.førsteDagIInneværendeMåned().minusMonths(1)
            )
        }
    }

    @Test
    fun `virkningsdatoFra skal returnere første dag i inneværende måned, 2 dager eller mer etter kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            listOf<Long>(2, 3).forEach { antallDagerEtterKjøredato ->
                val kjøredato = this.minusDays(antallDagerEtterKjøredato)
                assertThat(
                    virkningsdatoFra.invoke(
                        migreringServiceMock,
                        kjøredato
                    )
                ).isEqualTo(this.førsteDagIInneværendeMåned())
            }
        }
    }

    @Test
    fun `fagsak og saksstatistikk mellomlagring skal rulles tilbake når migrering feiler`() {
        every { infotrygdBarnetrygdClient.hentSaker(any(), any()) } returns
            InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())

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
                val antallNyeStatistikkEvents =
                    saksstatistikkMellomlagringRepository.count() - antallStatistikkEventsEtterSisteVelykkedeMigrering
                assertThat(antallNyeStatistikkEvents).isZero
                assertThat(fagsakRepository.finnAntallFagsakerTotalt()).isEqualTo(
                    antallFagsakerEtterSisteVelykkedeMigrering
                )
            }
    }

    @Test
    fun `innhold i meldinger til saksstatistikk ved migrering`() {
        run { `migrering av ordinær sak - happy case`() }

        val (sakDvhMeldinger, behandlingDvhMeldinger) = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()
            .partition { it.type == SaksstatistikkMellomlagringType.SAK }.let {
                it.first.map { objectMapper.readValue(it.json, SakDVH::class.java) } to
                    it.second.map { sakstatistikkObjectMapper.readValue(it.json, BehandlingDVH::class.java) }
            }

        assertThat(sakDvhMeldinger).extracting("sakStatus")
            .contains(FagsakStatus.OPPRETTET.name, FagsakStatus.LØPENDE.name)

        assertThat(behandlingDvhMeldinger).extracting("behandlingStatus").contains(
            BehandlingStatus.UTREDES.name,
            BehandlingStatus.IVERKSETTER_VEDTAK.name,
            BehandlingStatus.AVSLUTTET.name
        )
        assertThat(behandlingDvhMeldinger).extracting("resultat")
            .containsSequence(Behandlingsresultat.IKKE_VURDERT.name, Behandlingsresultat.INNVILGET.name)
        assertThat(behandlingDvhMeldinger).extracting("totrinnsbehandling").containsOnly(false)
        assertThat(behandlingDvhMeldinger).extracting("behandlingAarsak").containsOnly(BehandlingÅrsak.MIGRERING.name)
        assertThat(behandlingDvhMeldinger).extracting("behandlingType")
            .containsOnly(BehandlingType.MIGRERING_FRA_INFOTRYGD.name)
        assertThat(behandlingDvhMeldinger).extracting("saksbehandler", "beslutter")
            .containsOnly(tuple(SYSTEM_FORKORTELSE, SYSTEM_FORKORTELSE), tuple(null, null))
    }

    @Test
    fun `migrering skal feile med IDENT_IKKE_LENGER_AKTIV når input har ident som er historisk i PDL`() {
        val mockkPersonidentService = mockk<PersonidentService>()
        val s = MigreringService(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockkPersonidentService,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true)
        )

        val aktivFnr = randomFnr()
        val historiskFnr = randomFnr()

        every { mockkPersonidentService.hentIdenter(historiskFnr, true) } returns listOf(
            IdentInformasjon(aktivFnr, false, "FOLKEREGISTERIDENT"),
            IdentInformasjon(historiskFnr, true, "FOLKEREGISTERIDENT"),
            IdentInformasjon("112244", false, "AKTOERID")
        )

        assertThatThrownBy {
            s.migrer(historiskFnr)
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.IDENT_IKKE_LENGER_AKTIV)
    }

    @Test
    fun `migrering skal feile med HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD hvis PersonidentService returnerer samme aktørId `() {
        val mockkPersonidentService = mockk<PersonidentService>()
        val s = MigreringService(
            behandlingRepository = mockk(),
            behandlingService = mockk(),
            env = mockk(),
            fagsakService = mockk(),
            infotrygdBarnetrygdClient = infotrygdBarnetrygdClient,
            personidentService = mockkPersonidentService,
            stegService = mockk(),
            taskRepository = mockk(),
            tilkjentYtelseRepository = mockk(),
            totrinnskontrollService = mockk(),
            vedtakService = mockk(),
            vilkårService = mockk(),
            vilkårsvurderingService = mockk(),
            migreringRestClient = mockk(relaxed = true),
        )

        val ident = randomFnr()
        every { mockkPersonidentService.hentIdenter(any(), true) } answers {
            listOf(IdentInformasjon(firstArg(), false, "FOLKEREGISTERIDENT"))
        }
        every {
            infotrygdBarnetrygdClient.hentSaker(any(), any())
        } returns InfotrygdSøkResponse(listOf(opprettSakMedBeløp(SAK_BELØP_2_BARN_1_UNDER_6)), emptyList())

        every { mockkPersonidentService.hentOgLagreAktørIder(any(), false) } answers { callOriginal() }

        every { mockkPersonidentService.hentOgLagreAktør(any(), false) } returns
            Aktør(randomAktør().aktørId, personidenter = mutableSetOf())

        assertThatThrownBy {
            s.migrer(ident)
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD)

        // Verifiserer at feiltypen ikke er den samme når vi endrer til unike aktørId'er for barna
        every { mockkPersonidentService.hentOgLagreAktør(any(), false) } answers {
            Aktør(randomAktør().aktørId, personidenter = mutableSetOf())
        }
        assertThatThrownBy { s.migrer(ident) }
            .extracting("feiltype").isNotEqualTo(MigreringsfeilType.HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD)
    }

    private fun opprettSakMedBeløp(vararg beløp: Double) = Sak(
        stønad = Stønad(
            barn = listOf(
                Barn(ClientMocks.barnFnr[0], barnetrygdTom = "000000"),
                Barn(ClientMocks.barnFnr[1], barnetrygdTom = "000000")
            ),
            antallBarn = 2,
            delytelse = beløp.map {
                Delytelse(
                    fom = LocalDate.now(),
                    tom = null,
                    beløp = it,
                    typeDelytelse = "MS",
                    typeUtbetaling = "J",
                )
            },
            opphørsgrunn = "0"
        ),
        status = "FB",
        valg = "OR",
        undervalg = "OS"
    )

    private fun opprettUtvidetSakMedBeløp(vararg beløp: Double) = Sak(
        stønad = Stønad(
            barn = listOf(
                Barn(ClientMocks.barnFnr[0], barnetrygdTom = "000000"),
                Barn(ClientMocks.barnFnr[1], barnetrygdTom = "000000")
            ),
            antallBarn = 2,
            delytelse = beløp.map {
                Delytelse(
                    fom = LocalDate.now(),
                    tom = null,
                    beløp = it,
                    typeDelytelse = "MS",
                    typeUtbetaling = "J",
                )
            },
            opphørsgrunn = "0"
        ),
        status = "FB",
        valg = "UT",
        undervalg = "EF"
    )

    private fun infotrygdKjøredato(): LocalDate {
        return MigreringService::class.java.getDeclaredMethod("infotrygdKjøredato", YearMonth::class.java).run {
            this.trySetAccessible()
            this.invoke(migreringService, YearMonth.now()) as LocalDate
        }
    }

    companion object {
        const val SAK_BELØP_2_BARN_1_UNDER_6 = 2730.0
        const val SAK_BELØP_2_BARN_1_UNDER_6_UTVIDET = 3784.0
    }
}
