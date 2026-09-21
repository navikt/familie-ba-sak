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
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
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
import java.time.LocalDate

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
    private val søkersIdent = "12345678910"
    private val nyBehandling =
        NyBehandling(
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            kategori = BehandlingKategori.EØS,
            underkategori = BehandlingUnderkategori.UTVIDET,
            fagsakId = fagsak.id,
            barnasIdenter = listOf("12345678911"),
            søknadMottattDato = LocalDate.of(2026, 1, 1),
            søknadsinfo =
                Søknadsinfo(
                    journalpostId = "123456789",
                    brevkode = "NAV 33-00.07",
                    erDigital = true,
                ),
        )
    private val søknadData = SøknadData(nyBehandling = nyBehandling, søkersIdent = søkersIdent)

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
                    nyBehandling = any(),
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
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(any(), any())
                autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling)
                simuleringService.oppdaterSimuleringPåBehandling(behandling)
                autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering)
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling)
            }
        }

        @Test
        fun `skal videreføre opplysningene om søknaden til den automatiske behandlingen`() {
            // Arrange
            val nyBehandlingSlot = slot<NyBehandling>()
            every {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    nyBehandling = capture(nyBehandlingSlot),
                    filtrerAutomatiskBehandlingData = any(),
                )
            } returns behandling

            // Act
            autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            assertThat(nyBehandlingSlot.captured.søknadsinfo).isEqualTo(nyBehandling.søknadsinfo)
            assertThat(nyBehandlingSlot.captured.søknadMottattDato).isEqualTo(nyBehandling.søknadMottattDato)
            assertThat(nyBehandlingSlot.captured.barnasIdenter).isEqualTo(nyBehandling.barnasIdenter)
            assertThat(nyBehandlingSlot.captured.fagsakId).isEqualTo(nyBehandling.fagsakId)
            assertThat(nyBehandlingSlot.captured.kategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(nyBehandlingSlot.captured.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
            assertThat(nyBehandlingSlot.captured.behandlingType).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
            assertThat(nyBehandlingSlot.captured.behandlingÅrsak).isEqualTo(BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD)
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
                    nyBehandling = any(),
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
