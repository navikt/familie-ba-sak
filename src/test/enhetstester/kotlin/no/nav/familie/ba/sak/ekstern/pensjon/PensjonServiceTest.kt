package no.nav.familie.ba.sak.ekstern.pensjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.SendMeldingOmFerdigstiltBehandlingTilPensjonTask
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PensjonServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val infotrygdBarnetrygdKlient = mockk<InfotrygdBarnetrygdKlient>()
    private val envService = mockk<EnvService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val vilkårsvurderingTidslinjeService = mockk<VilkårsvurderingTidslinjeService>()
    private val pensjonService =
        PensjonService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            fagsakRepository = fagsakRepository,
            personidentService = personidentService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            taskRepository = taskRepository,
            infotrygdBarnetrygdKlient = infotrygdBarnetrygdKlient,
            envService = envService,
            featureToggleService = featureToggleService,
            vilkårsvurderingTidslinjeService = vilkårsvurderingTidslinjeService,
        )

    @Nested
    inner class OpprettTaskForSendingAvMeldingOmFerdigstiltBehandling {
        @Test
        fun `skal opprette task for sending av melding om ferdigstilt behandling til pensjon`() {
            // Arrange
            val behandling = lagBehandling()

            val forventetTask =
                SendMeldingOmFerdigstiltBehandlingTilPensjonTask.opprettTask(
                    behandling.fagsak.aktør.aktivFødselsnummer(),
                    behandling.id,
                )

            val taskSlot = slot<Task>()

            every { taskRepository.save(capture(taskSlot)) } answers { firstArg() }

            // Act
            pensjonService.opprettTaskForSendingAvMeldingOmFerdigstiltBehandling(behandling)

            // Assert
            verify(exactly = 1) { taskRepository.save(any()) }
            assertThat(taskSlot.captured.type).isEqualTo(SendMeldingOmFerdigstiltBehandlingTilPensjonTask.TASK_STEP_TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(forventetTask.payload)
        }
    }
}
