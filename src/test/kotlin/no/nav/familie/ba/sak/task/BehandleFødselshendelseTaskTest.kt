package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class BehandleFødselshendelseTaskTest(@Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
                                      @Autowired private val fagsakRepository: FagsakRepository,
                                      @Autowired private val behandlingRepository: BehandlingRepository,
                                      @Autowired private val databaseCleanupService: DatabaseCleanupService) {

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `ved behandling av fødselshendelse persisteres ikke behandlingsdata til databasen`() {
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse("12345678910", listOf("01101800033")), true)))
        Assertions.assertNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent("12345678910")))
    }

    @Test
    fun `ved behandling av fødselshendelse persisteres behandlingsdata til databasen`() {
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse("12345678910", listOf("01101800033")), false)))
        Assertions.assertNotNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent("12345678910")))
    }

    @Test
    fun `fagsak eksisterer for søker, ny behandling blir ikke persistert`() {
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse("12345678910", listOf("01101800033")), false)))

        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent("12345678910"))!!
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!
        behandling.status = BehandlingStatus.FERDIGSTILT
        behandlingRepository.save(behandling)

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse("12345678910", listOf("01101900033")), true)))
        Assertions.assertEquals(behandling.id, behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!.id)
    }

    @Test
    fun `fagsak eksisterer for søker, ny behandling opprettes og persisteres`() {
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse("12345678910", listOf("01101800033")), false)))

        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent("12345678910"))!!
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!
        behandling.status = BehandlingStatus.FERDIGSTILT
        behandlingRepository.save(behandling)
        
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse("12345678910", listOf("01101900033")), false)))
        Assertions.assertNotEquals(behandling.id, behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!.id)
    }
}

