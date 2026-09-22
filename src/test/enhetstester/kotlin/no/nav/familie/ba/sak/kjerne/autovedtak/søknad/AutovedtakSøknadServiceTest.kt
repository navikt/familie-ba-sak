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
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SøknadData
import no.nav.familie.ba.sak.kjerne.behandling.HenleggBehandlingInfoDto
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
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
    private val filtreringsreglerSøknadService = mockk<FiltreringsreglerSøknadService>()
    private val stegService = mockk<StegService>()
    private val oppgaveService = mockk<OppgaveService>()

    private val autovedtakSøknadService =
        AutovedtakSøknadService(
            autovedtakService = autovedtakService,
            simuleringService = simuleringService,
            taskService = taskService,
            autovedtakSøknadBegrunnelseService = autovedtakSøknadBegrunnelseService,
            autovedtakSøknadValideringService = autovedtakSøknadValideringService,
            filtreringsreglerSøknadService = filtreringsreglerSøknadService,
            stegService = stegService,
            oppgaveService = oppgaveService,
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
            every { stegService.håndterHenleggBehandling(any(), any()) } returns behandling
            every { stegService.håndterNyBehandlingOgSendInfotrygdFeed(any()) } returns lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) } returns "Oppgave opprettet"
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
        fun `skal henlegge og opprette manuell behandling i stedet for å simulere og vedta når behandlingen må behandles manuelt`() {
            // Arrange
            val henleggBehandlingInfoSlot = slot<HenleggBehandlingInfoDto>()
            every { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling) } throws
                AutovedtakMåBehandlesManueltFeil("Behandling av søknad må håndteres manuelt.")

            // Act
            val resultat = autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            assertThat(resultat).isEqualTo("Automatisk behandling av søknad er henlagt og sendt til manuell behandling: Behandling av søknad må håndteres manuelt.")
            verify(exactly = 1) { stegService.håndterHenleggBehandling(behandling, capture(henleggBehandlingInfoSlot)) }
            assertThat(henleggBehandlingInfoSlot.captured.årsak).isEqualTo(HenleggÅrsak.AUTOMATISK_HENLAGT)
            assertThat(henleggBehandlingInfoSlot.captured.begrunnelse).isEqualTo("Behandling av søknad må håndteres manuelt.")
            verify(exactly = 1) { stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling) }
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    any(),
                    "Behandling av søknad må håndteres manuelt.",
                    any(),
                    ManuellOppgaveType.SØKNAD,
                    Oppgavetype.BehandleSak,
                )
            }
            verify(exactly = 0) {
                simuleringService.oppdaterSimuleringPåBehandling(any())
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any())
                taskService.save(any())
            }
        }

        @Test
        fun `skal henlegge og opprette manuell behandling i stedet for å vedta når simuleringen må behandles manuelt`() {
            // Arrange
            val henleggBehandlingInfoSlot = slot<HenleggBehandlingInfoDto>()
            every { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering) } throws
                AutovedtakMåBehandlesManueltFeil("Behandling av søknad må håndteres manuelt.")

            // Act
            autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            verify(exactly = 1) { stegService.håndterHenleggBehandling(behandling, capture(henleggBehandlingInfoSlot)) }
            assertThat(henleggBehandlingInfoSlot.captured.begrunnelse).isEqualTo("Behandling av søknad må håndteres manuelt.")
            verify(exactly = 1) { stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling) }
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

    @Nested
    inner class KjørBehandlingStoppetAvFiltreringsregler {
        private val behandlingStoppetAvFiltrering =
            lagBehandling(
                fagsak = fagsak,
                årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
                skalBehandlesAutomatisk = true,
                førsteSteg = StegType.HENLEGG_BEHANDLING,
            )
        private val manuellBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

        @BeforeEach
        fun setup() {
            every {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    nyBehandling = any(),
                    filtrerAutomatiskBehandlingData = any(),
                )
            } returns behandlingStoppetAvFiltrering

            every { filtreringsreglerSøknadService.hentBegrunnelseForIkkeOppfyltFiltreringsregel(behandlingStoppetAvFiltrering.id) } returns "Barnet er dødt"

            every { stegService.håndterHenleggBehandling(any(), any()) } returns behandlingStoppetAvFiltrering
            every { stegService.håndterNyBehandlingOgSendInfotrygdFeed(any()) } returns manuellBehandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) } returns "Oppgave opprettet"
        }

        @Test
        fun `skal henlegge den automatiske behandlingen med begrunnelsen fra filtreringsregelen som ikke er oppfylt`() {
            // Arrange
            val henleggBehandlingInfoSlot = slot<HenleggBehandlingInfoDto>()

            // Act
            autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            verify(exactly = 1) { stegService.håndterHenleggBehandling(behandlingStoppetAvFiltrering, capture(henleggBehandlingInfoSlot)) }
            assertThat(henleggBehandlingInfoSlot.captured.årsak).isEqualTo(HenleggÅrsak.AUTOMATISK_HENLAGT)
            assertThat(henleggBehandlingInfoSlot.captured.begrunnelse).isEqualTo("Barnet er dødt")
        }

        @Test
        fun `skal opprette manuell behandling med BehandleSak-oppgave etter henleggelsen`() {
            // Act
            val resultat = autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            assertThat(resultat).isEqualTo("Automatisk behandling av søknad er henlagt og sendt til manuell behandling: Barnet er dødt")
            verifyOrder {
                stegService.håndterHenleggBehandling(behandlingStoppetAvFiltrering, any())
                stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling)
                oppgaveService.opprettOppgaveForManuellBehandling(
                    manuellBehandling.id,
                    "Barnet er dødt",
                    any(),
                    ManuellOppgaveType.SØKNAD,
                    Oppgavetype.BehandleSak,
                )
            }
        }

        @Test
        fun `skal opprette den manuelle behandlingen med årsak SØKNAD selv om den automatiske behandlingen ble bestilt med årsak AUTOMATISK_BEHANDLING_AV_SØKNAD`() {
            // Arrange
            val søknadDataFraMottak =
                søknadData.copy(
                    nyBehandling = nyBehandling.copy(behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD),
                )
            val nyBehandlingSlot = slot<NyBehandling>()
            every { stegService.håndterNyBehandlingOgSendInfotrygdFeed(capture(nyBehandlingSlot)) } returns manuellBehandling

            // Act
            autovedtakSøknadService.kjørBehandling(søknadDataFraMottak)

            // Assert
            assertThat(nyBehandlingSlot.captured.behandlingÅrsak).isEqualTo(BehandlingÅrsak.SØKNAD)
        }

        @Test
        fun `skal ikke validere, simulere eller opprette vedtak når filtreringsreglene stopper behandlingen`() {
            // Act
            autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            verify(exactly = 0) {
                autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(any())
                simuleringService.oppdaterSimuleringPåBehandling(any())
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any())
                taskService.save(any())
            }
        }
    }
}
