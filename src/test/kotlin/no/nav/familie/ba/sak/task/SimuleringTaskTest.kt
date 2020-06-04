package no.nav.familie.ba.sak.task

import io.mockk.every
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
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
class SimuleringTaskTest(@Autowired private val simuleringTask: SimuleringTask,
                         @Autowired private val featureToggleService: FeatureToggleService,
                         @Autowired private val fagsakRepository: FagsakRepository,
                         @Autowired private val databaseCleanupService: DatabaseCleanupService) {

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `simulering persisterer ikke behandlingsdata til databasen`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
        } returns true
        val task = SimuleringTask.opprettTask(NyBehandlingHendelse("12345678910", listOf("01101800033")))
        simuleringTask.doTask(task)
        Assertions.assertNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent("12345678910")))
    }

    @Test
    fun `simulering persisterer behandlingsdata til databasen`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
        } returns false

        val task = SimuleringTask.opprettTask(NyBehandlingHendelse("12345678910", listOf("01101800033")))
        simuleringTask.doTask(task)
        Assertions.assertNotNull(fagsakRepository.finnFagsakForPersonIdent(PersonIdent("12345678910")))
    }
}

