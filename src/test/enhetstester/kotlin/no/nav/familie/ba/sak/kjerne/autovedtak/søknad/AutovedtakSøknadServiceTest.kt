package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SøknadData
import no.nav.familie.ba.sak.kjerne.behandling.Søknad
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AutovedtakSøknadServiceTest {
    private val autovedtakService = mockk<AutovedtakService>()
    private val simuleringService = mockk<SimuleringService>()
    private val taskService = mockk<TaskService>()
    private val autovedtakSøknadBegrunnelseService = mockk<AutovedtakSøknadBegrunnelseService>()
    private val autovedtakSøknadValideringService = mockk<AutovedtakSøknadValideringService>()

    private val autovedtakSøknadService =
        AutovedtakSøknadService(
            autovedtakService = autovedtakService,
            simuleringService = simuleringService,
            taskService = taskService,
            autovedtakSøknadBegrunnelseService = autovedtakSøknadBegrunnelseService,
            autovedtakSøknadValideringService = autovedtakSøknadValideringService,
        )

    private val fagsak = lagFagsak()
    private val behandling = lagBehandling(fagsak = fagsak, førsteSteg = StegType.IVERKSETT_MOT_OPPDRAG, resultat = Behandlingsresultat.INNVILGET)
    private val simulering = listOf(lagØkonomiSimuleringMottaker(behandling = behandling))
    private val søknad =
        Søknad(
            fagsakId = fagsak.id,
            søkersIdent = "12345678910",
            barnasIdenter = listOf("12345678911"),
        )
    private val søknadData = SøknadData(søknad = søknad)

    @Nested
    inner class SkalAutovedtakBehandles {
        @Test
        fun `skal alltid returnere true`() {
            // Act
            val skalAutovedtakBehandles = autovedtakSøknadService.skalAutovedtakBehandles(søknadData)

            // Assert
            assertThat(skalAutovedtakBehandles).isTrue()
        }
    }

    @Nested
    inner class KjørBehandling {
        @BeforeEach
        fun setup() {
            every {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    fagsakId = søknad.fagsakId,
                    behandlingType = any(),
                    behandlingÅrsak = any(),
                    filtrerAutomatiskBehandlingData = any(),
                )
            } returns behandling

            justRun { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(any()) }
            every { simuleringService.oppdaterSimuleringPåBehandling(behandling) } returns simulering
            justRun { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(any()) }
            justRun { autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(any()) }
            every { autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling) } returns lagVedtak(behandling = behandling)
            every { taskService.save(any()) } returns mockk()
        }

        @Test
        fun `skal validere behandlingen etter behandlingsresultat og simuleringen før vedtak`() {
            // Act
            autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            verifyOrder {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(any(), any(), any(), any())
                autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling)
                simuleringService.oppdaterSimuleringPåBehandling(behandling)
                autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering)
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling)
            }
        }

        @Test
        fun `skal ikke simulere eller opprette vedtak når behandlingen må behandles manuelt`() {
            // Arrange
            every { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling) } throws
                AutovedtakMåBehandlesManueltFeil("Behandling av søknad må håndteres manuelt.")

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadService.kjørBehandling(søknadData) }
            verify(exactly = 0) {
                simuleringService.oppdaterSimuleringPåBehandling(any())
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any())
                taskService.save(any())
            }
        }

        @Test
        fun `skal ikke opprette vedtak når simuleringen må behandles manuelt`() {
            // Arrange
            every { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering) } throws
                AutovedtakMåBehandlesManueltFeil("Behandling av søknad må håndteres manuelt.")

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadService.kjørBehandling(søknadData) }
            verify(exactly = 0) {
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any())
                taskService.save(any())
            }
        }

        @Test
        fun `skal begrunne vedtak og opprette IverksettMotOppdragTask når behandlingsteg er IVERKSETT_MOT_OPPDRAG`() {
            // Arrange
            val taskSlot = slot<Task>()
            every { taskService.save(capture(taskSlot)) } returns mockk()

            // Act
            val resultat = autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            assertThat(resultat).isEqualTo(AutovedtakStegService.BEHANDLING_FERDIG)
            assertThat(taskSlot.captured.type).isEqualTo(IverksettMotOppdragTask.TASK_STEP_TYPE)
            verify(exactly = 1) {
                autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(behandling)
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling)
            }
        }

        @Test
        fun `skal kaste Feil når behandlingsteg etter behandlingsresultat ikke er IVERKSETT_MOT_OPPDRAG`() {
            // Arrange
            val behandlingUtenIverksettelse = lagBehandling(fagsak = fagsak, førsteSteg = StegType.FERDIGSTILLE_BEHANDLING, resultat = Behandlingsresultat.INNVILGET)
            every {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    fagsakId = søknad.fagsakId,
                    behandlingType = any(),
                    behandlingÅrsak = any(),
                    filtrerAutomatiskBehandlingData = any(),
                )
            } returns behandlingUtenIverksettelse
            every { simuleringService.oppdaterSimuleringPåBehandling(behandlingUtenIverksettelse) } returns simulering
            every {
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingUtenIverksettelse)
            } returns lagVedtak(behandling = behandlingUtenIverksettelse)

            // Act & Assert
            assertThrows<Feil> { autovedtakSøknadService.kjørBehandling(søknadData) }
            verify(exactly = 0) {
                autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(any())
            }
        }
    }
}
