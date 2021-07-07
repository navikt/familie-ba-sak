
package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseServiceGammel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles(
        "postgres",
        "dev",
        "mock-pdl",
)
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomatiskVurderingsLøpTest (
        @Autowired
        private val fødselshendelseServiceGammel: FødselshendelseServiceGammel,
        @Autowired
        private val featureToggleService: FeatureToggleService,
        @Autowired
        private val stegService: StegService,
        @Autowired
        private val infotrygdFeedService: InfotrygdFeedService,
        @Autowired
        private val fødelshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository,
        @Autowired
        private val infotrygdService: InfotrygdService,
        @Autowired
        private val behandlingMetrikker: BehandlingMetrikker,
        @Autowired
        private val loggService: LoggService,
        @Autowired
        private val arbeidsfordelingService: ArbeidsfordelingService,
        @Autowired
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
        @Autowired
        private val oppgaveService: OppgaveService,
        @Autowired
        private val vedtaksperiodeService: VedtaksperiodeService,

        ) {

    private val barnFnr = "21111777001"
    private val søkerFnr = "04086226621"

    val mockStegService = mockk<StegService>(relaxed = true)
    val mockFiltreringsreglerService = mockk<FiltreringsreglerService>(relaxed = true)
    val mockTaskRespository = mockk<TaskRepository>(relaxed = true)
    val mockPersongrunnlagService = mockk<PersongrunnlagService>(relaxed = true)
    val mockFagsakService = mockk<FagsakService>(relaxed = true)
    val mockVelgFagSystemService = mockk<VelgFagSystemService>(relaxed = true)
    val mockInfotrygdFeedService = mockk<InfotrygdFeedService>(relaxed = true)
    val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = true)
    val mockBehandlingRepository = mockk<BehandlingRepository>()
    val mockFagsakPersonRepository = mockk<FagsakPersonRepository>()
    val mockVedtakRepository = mockk<VedtakRepository>()
    val mockInfotrygdService = mockk<InfotrygdService>()


    val velgFagSystemService = VelgFagSystemService(mockFagsakService, mockPersonopplysningerService)
    val behandlingService = BehandlingService(
            mockBehandlingRepository,
            behandlingMetrikker,
            mockFagsakPersonRepository,
            mockVedtakRepository,
            loggService,
            arbeidsfordelingService,
            saksstatistikkEventPublisher,
            oppgaveService, mockInfotrygdService, vedtaksperiodeService, featureToggleService
    )
    val fødselshendelseServiceNy = FødselshendelseServiceNy(mockStegService,
                                                            mockFiltreringsreglerService,
                                                            mockTaskRespository,
                                                            mockPersongrunnlagService,
                                                            mockFagsakService,
                                                            behandlingService,
                                                            velgFagSystemService,
                                                            mockInfotrygdFeedService)
    val behandleFødselshendelseTask = BehandleFødselshendelseTask(fødselshendelseServiceGammel,
                                                                  fødselshendelseServiceNy,
                                                                  featureToggleService,
                                                                  stegService,
                                                                  infotrygdFeedService,
                                                                  fødelshendelsePreLanseringRepository)
    private lateinit var task: Task

    @BeforeEach
    fun setup() {
        task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(søkerFnr,
                                                                                                           listOf(barnFnr))))
    }

    @Test
    fun `sjekker at en sak skal til BA`() {
        every {
            mockVelgFagSystemService.velgFagsystem(any())
        } returns FagsystemRegelVurdering.SEND_TIL_BA
        Assert.assertEquals(FagsystemRegelVurdering.SEND_TIL_BA, fødselshendelseServiceNy.hentFagsystemForFødselshendelse(NyBehandlingHendelse(søkerFnr, listOf(barnFnr))))
    }

    @Test
    @Disabled
    fun `Starter en ny behandling i BA`() {
        every {
            mockFagsakService.hent(any())
        } returns defaultFagsak.also { it.status = FagsakStatus.LØPENDE }
        every {
            mockInfotrygdService.harÅpenSakIInfotrygd(any())
        } returns false
        behandleFødselshendelseTask.doTask(task) //todo mock en behandling i repositoriet?
        Assert.assertEquals(fødselshendelseServiceNy.hentFagsystemForFødselshendelse(NyBehandlingHendelse(søkerFnr,
                                                                                                          listOf(barnFnr))),
                            FagsystemRegelVurdering.SEND_TIL_BA)
    }

    @Test
    fun `Ikke løpende betaling i BA, skal sendes til infotrygd`() {
        every {
            mockFagsakService.hent(any())
        } returns defaultFagsak.also { it.status = FagsakStatus.AVSLUTTET }
        Assert.assertEquals(FagsystemRegelVurdering.SEND_TIL_INFOTRYGD, fødselshendelseServiceNy.hentFagsystemForFødselshendelse(
                NyBehandlingHendelse(søkerFnr, listOf(barnFnr))))
    }   
}

