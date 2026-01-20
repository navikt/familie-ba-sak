package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningBrevService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.task.FerdigstillOppgaver
import no.nav.familie.ba.sak.task.JournalførTilbakekrevingsvedtakMotregningBrevTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

class BeslutteVedtakTest {
    private val toTrinnKontrollService = mockk<TotrinnskontrollService>()
    private val vedtakService: VedtakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val beregningService: BeregningService = mockk()
    private val taskRepository: TaskRepositoryWrapper = mockk()
    private val vilkårsvurderingService: VilkårsvurderingService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService = mockk()
    private val automatiskBeslutningService: AutomatiskBeslutningService = mockk()
    private val saksbehandlerContext = mockk<SaksbehandlerContext>()
    private val loggService = mockk<LoggService>()
    private val automatiskOppdaterValutakursService = mockk<AutomatiskOppdaterValutakursService>()
    private val simuleringService = mockk<SimuleringService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val brevmottakerService = mockk<BrevmottakerService>()
    private val tilbakekrevingsvedtakMotregningService = mockk<TilbakekrevingsvedtakMotregningService>()
    private val tilbakekrevingsvedtakMotregningBrevService = mockk<TilbakekrevingsvedtakMotregningBrevService>()

    val beslutteVedtak =
        BeslutteVedtak(
            totrinnskontrollService = toTrinnKontrollService,
            vedtakService = vedtakService,
            behandlingService = behandlingService,
            beregningService = beregningService,
            taskRepository = taskRepository,
            loggService = loggService,
            vilkårsvurderingService = vilkårsvurderingService,
            featureToggleService = featureToggleService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            saksbehandlerContext = saksbehandlerContext,
            automatiskBeslutningService = automatiskBeslutningService,
            simuleringService = simuleringService,
            tilbakekrevingService = tilbakekrevingService,
            brevmottakerService = brevmottakerService,
            tilbakekrevingsvedtakMotregningService = tilbakekrevingsvedtakMotregningService,
            tilbakekrevingsvedtakMotregningBrevService = tilbakekrevingsvedtakMotregningBrevService,
        )

    private val randomVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

    @BeforeEach
    fun setUp() {
        every { taskRepository.save(any()) } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        every {
            toTrinnKontrollService.besluttTotrinnskontroll(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            Totrinnskontroll(
                behandling = lagBehandling(),
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "Mock.Saksbehandler",
            )
        every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any(), any()) } just Runs
        every { vedtakService.oppdaterVedtaksdatoOgBrev(any()) } just runs
        every { behandlingService.opprettOgInitierNyttVedtakForBehandling(any(), any()) } just runs
        every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns randomVilkårsvurdering
        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) } returns randomVilkårsvurdering
        every { saksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "saksbehandlerNavn"
        every { automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(any<BehandlingId>()) } just runs
        every { tilbakekrevingService.søkerHarÅpenTilbakekreving(any()) } returns false
        every { tilbakekrevingService.hentTilbakekrevingsvalg(any()) } returns null
        every { simuleringService.hentFeilutbetaling(any<Long>()) } returns BigDecimal.ZERO
        every { tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(any()) } returns null
    }

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @Test
        fun `Skal ferdigstille Godkjenne vedtak-oppgave ved Godkjent vedtak`() {
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.GODKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )

            val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)

            verify(exactly = 1) { FerdigstillOppgaver.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
            Assertions.assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, nesteSteg)
        }

        @Test
        fun `Ved godkjent vedtak og eksisterende tilbakekrevingsvedtak ved motregning brev så skal det opprettes task for å journalføre denne`() {
            // Arrange
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.GODKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            every { tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(any()) } returns mockk()
            every { tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(any()) } returns mockk()
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            mockkObject(JournalførTilbakekrevingsvedtakMotregningBrevTask.Companion)
            every { JournalførTilbakekrevingsvedtakMotregningBrevTask.opprettTask(behandling.id) } returns mockk()
            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )

            // Act
            beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)

            // Assert
            verify(exactly = 1) { JournalførTilbakekrevingsvedtakMotregningBrevTask.opprettTask(any()) }
        }

        @Test
        fun `Skal ferdigstille Godkjenne vedtak-oppgave og opprette Behandle Underkjent Vedtak-oppgave ved Underkjent vedtak`() {
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.UNDERKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            mockkObject(FerdigstillOppgaver.Companion)
            mockkObject(OpprettOppgaveTask.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every {
                OpprettOppgaveTask.opprettTask(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")

            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )

            val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)

            verify(exactly = 1) { FerdigstillOppgaver.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak) }
            verify(exactly = 1) {
                OpprettOppgaveTask.opprettTask(
                    behandling.id,
                    Oppgavetype.BehandleUnderkjentVedtak,
                    any(),
                    any(),
                    any(),
                )
            }
            Assertions.assertEquals(StegType.SEND_TIL_BESLUTTER, nesteSteg)
        }

        @Test
        fun `Skal ikke iverksette hvis det ikke er forskjell i utbetaling mellom nåværende og forrige andeler`() {
            val behandling = lagBehandling()
            val vedtak = lagVedtak(behandling)
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.GODKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING

            mockkObject(JournalførVedtaksbrevTask.Companion)
            every {
                JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                    any(),
                    any(),
                    any(),
                )
            } returns Task(OpprettOppgaveTask.TASK_STEP_TYPE, "")

            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )

            val nesteSteg = beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)

            verify(exactly = 1) { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) }

            verify(exactly = 1) {
                JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                    personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                    behandlingId = behandling.id,
                    vedtakId = vedtak.id,
                )
            }
            Assertions.assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, nesteSteg)
        }

        @Test
        fun `Skal initiere nytt vedtak når vedtak ikke er godkjent`() {
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.UNDERKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            mockkObject(FerdigstillOppgaver.Companion)
            mockkObject(OpprettOppgaveTask.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns
                Task(
                    OpprettOppgaveTask.TASK_STEP_TYPE,
                    "",
                )

            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false

            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )

            beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)
            verify(exactly = 1) { behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling, true) }
        }

        @Test
        fun `Skal kaste feil dersom toggle ikke er enabled og årsak er korreksjon vedtaksbrev`() {
            every {
                featureToggleService.isEnabled(
                    FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV,
                    any<Long>(),
                )
            } returns false

            val behandling = lagBehandling(årsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV)
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns
                Task(
                    type = FerdigstillOppgaver.TASK_STEP_TYPE,
                    payload = "",
                )

            assertThrows<FunksjonellFeil> { beslutteVedtak.preValiderSteg(behandling) }
        }

        @Test
        fun `Skal kaste feil dersom saksbehandler uten tilgang til teknisk endring prøve å godkjenne en behandling med årsak=teknisk endring`() {
            every { featureToggleService.isEnabled(FeatureToggle.TEKNISK_ENDRING, any<Long>()) } returns false

            val behandling = lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING)
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns
                Task(
                    type = FerdigstillOppgaver.TASK_STEP_TYPE,
                    payload = "",
                )

            assertThrows<FunksjonellFeil> { beslutteVedtak.preValiderSteg(behandling) }
        }

        @Test
        fun `Skal feile ferdigstilling av Godkjenne vedtak-oppgave ved Godkjent vedtak når brevmottakerne er ugyldige`() {
            // Arrange
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.GODKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false

            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id, landkode = "SE"),
                    lagBrevmottakerDb(behandlingId = behandling.id, landkode = "NO"),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)
                }

            assertThat(exception.message).isEqualTo("Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket")
        }

        @ParameterizedTest
        @EnumSource(Tilbakekrevingsvalg::class, names = ["OPPRETT_TILBAKEKREVING_MED_VARSEL", "OPPRETT_TILBAKEKREVING_UTEN_VARSEL", "OPPRETT_TILBAKEKREVING_AUTOMATISK"], mode = EnumSource.Mode.INCLUDE)
        fun `Skal kaste feil dersom feilutbetaling ikke lenger finnes og det er valgt å opprette en tilbakekrevingssak`(tilbakekrevingsvalg: Tilbakekrevingsvalg) {
            // Arrange
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))

            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.GODKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )
            every { tilbakekrevingService.hentTilbakekrevingsvalg(behandling.id) } returns tilbakekrevingsvalg

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)
                }.melding

            assertThat(feilmelding).isEqualTo("Det er valgt å opprette tilbakekrevingssak men det er ikke lenger feilutbetalt beløp. Behandlingen må underkjennes, og saksbehandler må gå tilbake til behandlingsresultatet og trykke neste og fullføre behandlingen på nytt.")
        }

        @Test
        fun `Skal kaste feil dersom det er feilutbetalt beløp men det er ikke valgt å opprette tilbakekrevingssak`() {
            // Arrange
            val behandling = lagBehandling()
            behandling.status = BehandlingStatus.FATTER_VEDTAK
            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))

            val beslutningPåVedtakDto = BeslutningPåVedtakDto(Beslutning.GODKJENT)

            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            mockkObject(FerdigstillOppgaver.Companion)
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { FerdigstillOppgaver.opprettTask(any(), any()) } returns Task(FerdigstillOppgaver.TASK_STEP_TYPE, "")
            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
                listOf(
                    lagBrevmottakerDb(behandlingId = behandling.id),
                )
            every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(10000)

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    beslutteVedtak.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)
                }.melding

            assertThat(feilmelding).isEqualTo("Det er en feilutbetaling som saksbehandler ikke har tatt stilling til. Saken må underkjennes og sendes tilbake til saksbehandler for ny vurdering.")
        }

        @Test
        fun `Skal oppdatere vedtaksbrev for tilbakekrevingsvedtak motregning`() {
            // Arrange
            val behandling = lagBehandling()

            every { brevmottakerService.hentBrevmottakere(any()) } returns emptyList()
            every { automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(any()) } returns false
            every { vedtakService.hentAktivForBehandling(any()) } returns lagVedtak(behandling)
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(any()) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            every { tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(any()) } returns mockk()
            every { tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(any()) } returns mockk()

            // Act
            beslutteVedtak.utførStegOgAngiNeste(behandling, BeslutningPåVedtakDto(Beslutning.GODKJENT))

            // Assert
            verify(exactly = 1) {
                tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(
                    behandling.id,
                )
            }
        }
    }
}
