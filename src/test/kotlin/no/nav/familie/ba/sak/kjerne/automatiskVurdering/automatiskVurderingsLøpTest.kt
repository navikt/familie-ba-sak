
package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FødselshendelseServiceNy
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FødselshendelseServiceGammel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
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

        ) {
    private val barnFnr = "21111777001"
    private val søkerFnr = "04086226621"

    val mockStegService = mockk<StegService>(relaxed = true)
    val mockFiltreringsreglerService = mockk<FiltreringsreglerService>(relaxed = true)
    val mockTaskRespository = mockk<TaskRepository>(relaxed = true)
    val mockPersongrunnlagService = mockk<PersongrunnlagService>(relaxed = true)
    val mockFagsakService = mockk<FagsakService>(relaxed = true)
    val mockBehandlingService = mockk<BehandlingService>(relaxed = true)
    val mockVelgFagSystemService = mockk<VelgFagSystemService>(relaxed = true)
    val mockInfotrygdFeedService = mockk<InfotrygdFeedService>(relaxed = true)
    val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = true)
    
    
    val velgFagSystemService = VelgFagSystemService( mockFagsakService, mockPersonopplysningerService)
    val fødselshendelseServiceNy = FødselshendelseServiceNy(mockStegService,mockFiltreringsreglerService,mockTaskRespository,mockPersongrunnlagService, mockFagsakService, mockBehandlingService, velgFagSystemService, mockInfotrygdFeedService)
    val behandleFødselshendelseTask = BehandleFødselshendelseTask(fødselshendelseServiceGammel,fødselshendelseServiceNy,featureToggleService,stegService,infotrygdFeedService,fødelshendelsePreLanseringRepository)
    private lateinit var task: Task

    @BeforeEach
    fun setup() {
        task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(søkerFnr, listOf(barnFnr))))
    }
    
    @Test
    fun `sjekker at en sak skal til BA`() {
        every {
            mockVelgFagSystemService.velgFagsystem(any())
        } returns FagsystemRegelVurdering.SEND_TIL_BA
        Assert.assertEquals(FagsystemRegelVurdering.SEND_TIL_BA, fødselshendelseServiceNy.hentFagsystemForFødselshendelse(NyBehandlingHendelse(søkerFnr, listOf(barnFnr))))
    }

    @Test
    fun `Starter en ny behandling i BA`() {
        every {
            mockFagsakService.hent(any())
        } returns defaultFagsak.also { it.status = FagsakStatus.LØPENDE }
        behandleFødselshendelseTask.doTask(task)
        //Assert.assertEquals(fødselshendelseServiceNy.hentFagsystemForFødselshendelse(NyBehandlingHendelse(søkerFnr, listOf(barnFnr))),FagsystemRegelVurdering.SEND_TIL_BA)*/
    }
    
    @Test
    fun `Ikke løpende betaling i BA, skal sendes til infotrygd`(){
        every { 
            mockFagsakService.hent(any())
        } returns defaultFagsak.also { it.status = FagsakStatus.AVSLUTTET }
        behandleFødselshendelseTask.doTask(task)
        Assert.assertEquals(FagsystemRegelVurdering.SEND_TIL_INFOTRYGD, fødselshendelseServiceNy.hentFagsystemForFødselshendelse(
                NyBehandlingHendelse(søkerFnr, listOf(barnFnr))))
    }   
}

