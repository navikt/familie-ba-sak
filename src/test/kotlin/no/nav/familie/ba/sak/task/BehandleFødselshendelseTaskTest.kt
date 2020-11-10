package no.nav.familie.ba.sak.task

import io.mockk.every
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.vedtak.producer.MockKafkaProducer
import org.assertj.core.api.Assertions.anyOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
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
import kotlin.collections.contains
import kotlin.collections.listOf
import kotlin.ranges.contains
import kotlin.sequences.contains
import kotlin.text.contains

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-pdl", "mock-dokgen", "mock-infotrygd-feed", "mock-infotrygd-barnetrygd")
@Tag("integration")
class BehandleFødselshendelseTaskTest(@Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
                                      @Autowired private val featureToggleService: FeatureToggleService,
                                      @Autowired private val fagsakRepository: FagsakRepository,
                                      @Autowired private val behandlingRepository: BehandlingRepository,
                                      @Autowired private val databaseCleanupService: DatabaseCleanupService) {

    val barnIdent = ClientMocks.barnFnr[0]
    val morsIdent = ClientMocks.søkerFnr[0]

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
        MockKafkaProducer.sendteMeldinger.clear()
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun `ved behandling av fødselshendelse persisteres ikke behandlingsdata til databasen`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.skal-iverksette-fodselshendelse")
        } returns false

        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        assertNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent)))
        assertThat(MockKafkaProducer.sendteMeldinger).hasSize(0)
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun `ved behandling av fødselshendelse persisteres behandlingsdata til databasen`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.skal-iverksette-fodselshendelse")
        } returns true
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        assertNotNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent)))
        assertThat(MockKafkaProducer.sendteMeldinger).hasSize(2)
        assertThat(MockKafkaProducer.sendteMeldinger.keys.filter { it.startsWith("sak") }).hasSize(1)
        assertThat(MockKafkaProducer.sendteMeldinger.keys.filter { it.startsWith("behandling") }).hasSize(1)
    }

    @Test
    fun `fagsak eksisterer for søker, ny behandling blir ikke persistert`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.skal-iverksette-fodselshendelse")
        } returns true
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))

        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsIdent))!!
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!
        behandling.status = BehandlingStatus.AVSLUTTET
        behandlingRepository.save(behandling)

        every {
            featureToggleService.isEnabled("familie-ba-sak.skal-iverksette-fodselshendelse")
        } returns false
        behandleFødselshendelseTask.doTask(BehandleFødselshendelseTask.opprettTask(
                BehandleFødselshendelseTaskDTO(NyBehandlingHendelse(morsIdent = morsIdent, barnasIdenter = listOf(barnIdent)))))
        Assertions.assertEquals(behandling.id, behandlingRepository.findByFagsakAndAktiv(fagsakId = fagsak.id)!!.id)
    }

    @Test
    fun `fagsak eksisterer for søker, ny behandling opprettes og persisteres`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.skal-iverksette-fodselshendelse")
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

