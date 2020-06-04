package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class SimuleringTaskTest(@Autowired private val simuleringTask: SimuleringTask,
                         @Autowired private val featureToggleService: FeatureToggleService) {

    @Test
    fun `foo`() {
        every {
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
        } returns true

        val task = SimuleringTask.opprettTask(NyBehandlingHendelse("12345678910", listOf("01101800033")))
        simuleringTask.doTask(task)
        simuleringTask.onCompletion(task)


    }
}

