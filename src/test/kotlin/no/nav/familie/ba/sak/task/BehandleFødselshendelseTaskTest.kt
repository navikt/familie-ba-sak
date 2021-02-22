package no.nav.familie.ba.sak.task

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.vedtak.producer.MockKafkaProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-pdl", "mock-dokgen", "mock-infotrygd-feed", "mock-infotrygd-barnetrygd")
@Tag("integration")
class BehandleFødselshendelseTaskTest(@Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
                                      @Autowired private val envService: EnvService,
                                      @Autowired private val fagsakRepository: FagsakRepository,
                                      @Autowired private val behandlingRepository: BehandlingRepository,
                                      @Autowired private val databaseCleanupService: DatabaseCleanupService,
                                      @Autowired private val mockIntegrasjonClient: IntegrasjonClient,
                                      @Autowired private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository) {

    val barnIdent = ClientMocks.barnFnr[0]
    val morsIdent = ClientMocks.søkerFnr[0]

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
        MockKafkaProducer.sendteMeldinger.clear()
        clearAllMocks(answers = false) // Resetter alle mocks, unntatt answers-blocks. Nødvendig da verify tidvis feiler pga eksisterende mock-state.
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun `ved behandling av fødselshendelse persisteres ikke behandlingsdata til databasen når iverksetting er avskrudd`() {
        every {
            envService.skalIverksetteBehandling()
        } returns false

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        assertNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent)))
        assertThat(MockKafkaProducer.sendteMeldinger).hasSize(0)
        verify(exactly = 0) { mockIntegrasjonClient.opprettSkyggesak(any(), any()) }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun `ved behandling av fødselshendelse persisteres behandlingsdata til databasen når iverksetting er påskrudd`() {
        every {
            envService.skalIverksetteBehandling()
        } returns true

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent))
        assertNotNull(fagsak)
        assertThat(saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()).hasSize(3)
        assertThat(saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending().filter { it.type == SaksstatistikkMellomlagringType.SAK }).hasSize(2)
        assertThat(saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending().filter { it.type == SaksstatistikkMellomlagringType.BEHANDLING }).hasSize(1)
        verify(exactly = 1) { mockIntegrasjonClient.opprettSkyggesak(any(), fagsak?.id!!) }
    }

    @Test
    fun `fagsak eksisterer for søker, ny behandling blir ikke persistert`() {

        // dette er kun for å lage en "eksisterende fagsak"
        every {
            envService.skalIverksetteBehandling()
        } returns true

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))

        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent))!!
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!
        behandling.status = BehandlingStatus.AVSLUTTET
        behandlingRepository.save(behandling)

        every {
            envService.skalIverksetteBehandling()
        } returns false

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        Assertions.assertEquals(behandling.id, behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!.id)
    }

    @Test
    fun `fagsak eksisterer for søker, ny behandling opprettes og persisteres`() {
        every {
            envService.skalIverksetteBehandling()
        } returns true

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))

        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent))!!
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!
        behandling.status = BehandlingStatus.AVSLUTTET
        behandlingRepository.save(behandling)

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        Assertions.assertNotEquals(behandling.id, behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!.id)
    }
}
