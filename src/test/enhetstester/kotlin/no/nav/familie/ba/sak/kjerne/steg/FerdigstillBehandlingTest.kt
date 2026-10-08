package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.task.SendMeldingOmFerdigstiltBehandlingTilPensjonTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FerdigstillBehandlingTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val beregningService = mockk<BeregningService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingMetrikker = mockk<BehandlingMetrikker>()
    private val loggService = mockk<LoggService>()
    private val snikeIKøenService = mockk<SnikeIKøenService>()
    private val taskService = mockk<TaskService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val ferdigstillBehandling =
        FerdigstillBehandling(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            beregningService = beregningService,
            behandlingService = behandlingService,
            behandlingMetrikker = behandlingMetrikker,
            loggService = loggService,
            snikeIKøenService = snikeIKøenService,
            taskService = taskService,
            featureToggleService = featureToggleService,
        )

    private val behandling =
        lagBehandling(
            status = BehandlingStatus.IVERKSETTER_VEDTAK,
            resultat = Behandlingsresultat.INNVILGET,
        )

    @BeforeEach
    fun setUp() {
        every { loggService.opprettFerdigstillBehandling(any()) } just runs
        every { behandlingMetrikker.oppdaterBehandlingMetrikker(any()) } just runs
        every { fagsakService.oppdaterStatus(any(), any()) } answers { firstArg() }
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } answers { behandling }
        every { snikeIKøenService.reaktiverBehandlingPåMaskinellVent(any()) } returns false
    }

    @Nested
    inner class UtførStegOgAngiNeste {
        @Test
        fun `skal opprette task for pensjon når feature toggle er aktivert`() {
            // Arrange
            val personIdent = behandling.fagsak.aktør.aktivFødselsnummer()
            val forventetTask = SendMeldingOmFerdigstiltBehandlingTilPensjonTask.opprettTask(personIdent, behandling.id)
            val pensjonTaskSlot = slot<Task>()

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns lagTilkjentYtelse(behandling)
            every { featureToggleService.isEnabled(FeatureToggle.FERDIGSTILL_BEHANDLING_MELDING_TIL_PENSJON) } returns true
            every { taskService.save(capture(pensjonTaskSlot)) } answers { firstArg() }

            // Act
            ferdigstillBehandling.utførStegOgAngiNeste(behandling, "")

            // Assert
            verify(exactly = 1) { taskService.save(any()) }
            assertThat(pensjonTaskSlot.captured.type).isEqualTo(SendMeldingOmFerdigstiltBehandlingTilPensjonTask.TASK_STEP_TYPE)
            assertThat(pensjonTaskSlot.captured.payload).isEqualTo(forventetTask.payload)
        }

        @Test
        fun `skal ikke opprette task for pensjon når feature toggle er deaktivert`() {
            // Arrange
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns lagTilkjentYtelse(behandling)
            every { featureToggleService.isEnabled(FeatureToggle.FERDIGSTILL_BEHANDLING_MELDING_TIL_PENSJON) } returns false

            // Act
            ferdigstillBehandling.utførStegOgAngiNeste(behandling, "")

            // Assert
            verify(exactly = 0) { taskService.save(any()) }
        }

        @Test
        fun `skal ikke opprette task for pensjon når behandlingen er henlagt selv om feature toggle er aktivert`() {
            // Arrange
            val henlagtBehandling =
                lagBehandling(
                    status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    resultat = Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                )

            every { behandlingHentOgPersisterService.hent(henlagtBehandling.id) } returns henlagtBehandling
            every { behandlingHentOgPersisterService.hentBehandlinger(henlagtBehandling.fagsak.id) } returns listOf(henlagtBehandling)
            every { behandlingHentOgPersisterService.finnAktivForFagsak(henlagtBehandling.fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(henlagtBehandling.fagsak.id) } returns null
            every { featureToggleService.isEnabled(FeatureToggle.FERDIGSTILL_BEHANDLING_MELDING_TIL_PENSJON) } returns true

            // Act
            ferdigstillBehandling.utførStegOgAngiNeste(henlagtBehandling, "")

            // Assert
            verify(exactly = 0) { taskService.save(any()) }
        }
    }
}
