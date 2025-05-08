package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.beregning.AvregningService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningBrevService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SendTilBeslutterTest {
    private val mockBehandlingService = mockk<BehandlingService>()
    private val mockTaskRepository = mockk<TaskRepositoryWrapper>()
    private val mockLoggService = mockk<LoggService>()
    private val mockTotrinnskontrollRepository = mockk<TotrinnskontrollRepository>()
    private val mockSaksbehandlerContext = mockk<SaksbehandlerContext>()
    private val mockVilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val mockVedtakService = mockk<VedtakService>()
    private val mockVedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val mockAutomatiskBeslutningService = mockk<AutomatiskBeslutningService>()
    private val mockValiderBrevmottakerService = mockk<ValiderBrevmottakerService>()
    private val mockAvregningService = mockk<AvregningService>()
    private val mockTilbakekrevingsvedtakMotregningService = mockk<TilbakekrevingsvedtakMotregningService>()
    private val mockTilbakekrevingsvedtakMotregningBrevService = mockk<TilbakekrevingsvedtakMotregningBrevService>()

    private val totrinnskontrollService = TotrinnskontrollService(mockBehandlingService, mockTotrinnskontrollRepository, mockSaksbehandlerContext)

    private val sendTilBeslutter =
        SendTilBeslutter(
            behandlingService = mockBehandlingService,
            taskRepository = mockTaskRepository,
            loggService = mockLoggService,
            totrinnskontrollService = totrinnskontrollService,
            vilkårsvurderingService = mockVilkårsvurderingService,
            vedtakService = mockVedtakService,
            vedtaksperiodeService = mockVedtaksperiodeService,
            automatiskBeslutningService = mockAutomatiskBeslutningService,
            validerBrevmottakerService = mockValiderBrevmottakerService,
            avregningService = mockAvregningService,
            tilbakekrevingsvedtakMotregningService = mockTilbakekrevingsvedtakMotregningService,
            tilbakekrevingsvedtakMotregningBrevService = mockTilbakekrevingsvedtakMotregningBrevService,
        )

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @Test
        fun `Skal opprette totrinnskontroll, logg, task og oppdatere vedtaksbrev før sending til neste steg`() {
            // Arrange
            val behandling = lagBehandling()
            val vedtak = lagVedtak(behandling = behandling)

            every { mockTotrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) } returns null
            every { mockTotrinnskontrollRepository.save(any()) } returns mockk()
            every { mockAutomatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling) } returns true
            every { mockSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "signatur"
            every { mockLoggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = true) } just runs
            every { mockTaskRepository.save(any()) } returnsArgument 0
            every { mockVedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id) } returns vedtak
            every { mockVedtakService.oppdaterVedtakMedStønadsbrev(vedtak) } returns vedtak
            every { mockBehandlingService.sendBehandlingTilBeslutter(behandling) } just runs
            every { mockTilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(any()) } returns null

            // Act
            sendTilBeslutter.utførStegOgAngiNeste(behandling, "")

            // Assert
            verify(exactly = 1) { mockTotrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) }
            verify(exactly = 1) { mockTotrinnskontrollRepository.save(any()) }
            verify(exactly = 1) { mockAutomatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling) }
            verify(exactly = 1) { mockSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() }
            verify(exactly = 1) { mockLoggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = true) }
            verify(exactly = 1) { mockTaskRepository.save(any()) }
            verify(exactly = 1) { mockVedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id) }
            verify(exactly = 1) { mockVedtakService.oppdaterVedtakMedStønadsbrev(vedtak) }
            verify(exactly = 1) { mockBehandlingService.sendBehandlingTilBeslutter(behandling) }
        }

        @Test
        fun `Skal ikke oppdatere vedtaksbrev dersom det er automatisk behandling`() {
            // Arrange
            val behandling = lagBehandling(skalBehandlesAutomatisk = true)
            val vedtak = lagVedtak(behandling = behandling)

            every { mockTotrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) } returns null
            every { mockTotrinnskontrollRepository.save(any()) } returns mockk()
            every { mockAutomatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling) } returns true
            every { mockSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "signatur"
            every { mockLoggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = true) } just runs
            every { mockTaskRepository.save(any()) } returnsArgument 0
            every { mockBehandlingService.sendBehandlingTilBeslutter(behandling) } just runs
            every { mockTilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(any()) } returns null

            // Act
            sendTilBeslutter.utførStegOgAngiNeste(behandling, "")

            // Assert
            verify(exactly = 1) { mockTotrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) }
            verify(exactly = 1) { mockTotrinnskontrollRepository.save(any()) }
            verify(exactly = 1) { mockAutomatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling) }
            verify(exactly = 1) { mockSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() }
            verify(exactly = 1) { mockLoggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = true) }
            verify(exactly = 1) { mockTaskRepository.save(any()) }
            verify(exactly = 0) { mockVedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id) }
            verify(exactly = 0) { mockVedtakService.oppdaterVedtakMedStønadsbrev(vedtak) }
            verify(exactly = 1) { mockBehandlingService.sendBehandlingTilBeslutter(behandling) }
        }

        @Test
        fun `Skal oppdatere vedtaksbrev for tilbakekrevingsvedtak motregning`() {
            // Arrange
            val behandling = lagBehandling(skalBehandlesAutomatisk = true)

            every { mockSaksbehandlerContext.hentSaksbehandlerSignaturTilBrev() } returns "signatur"
            every { mockTotrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) } returns null
            every { mockTotrinnskontrollRepository.save(any()) } returns mockk()
            every { mockAutomatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling) } returns false
            justRun { mockLoggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = false) }
            every { mockTaskRepository.save(any()) } returnsArgument 0
            every { mockTilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(any()) } returns mockk()
            every { mockTilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(any()) } returns mockk()
            justRun { mockBehandlingService.sendBehandlingTilBeslutter(behandling) }

            // Act
            sendTilBeslutter.utførStegOgAngiNeste(behandling, "")

            // Assert
            verify(exactly = 1) { mockTilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(behandling.id) }
        }
    }

    @Nested
    inner class PreValiderSteg {
        @Test
        fun `skal kaste feil hvis tilbakekrevingsvedtak ikke er komplett hvis det er perioder med avregning`() {
            val behandling = lagBehandling()
            val stegService = mockk<StegService>()
            val behandlingsresultatSteg = mockk<BehandlingsresultatSteg>()

            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    heleBeløpetSkalKrevesTilbake = false,
                )

            justRun {
                mockValiderBrevmottakerService
                    .validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                        any(),
                        any(),
                        any(),
                    )
            }
            every { mockVilkårsvurderingService.hentAktivForBehandling(any()) } returns null
            every { stegService.hentBehandlingSteg(any()) } returns behandlingsresultatSteg
            justRun { behandlingsresultatSteg.preValiderSteg(any()) }
            every { mockVedtakService.hentAktivForBehandlingThrows(any()) } returns mockk()
            every { mockVedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(any()) } returns emptyList()
            every { mockAvregningService.behandlingHarPerioderSomAvregnes(any()) } returns true
            every {
                mockTilbakekrevingsvedtakMotregningService.hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(
                    any(),
                )
            } returns tilbakekrevingsvedtakMotregning

            // Act
            val feil =
                assertThrows<Feil> {
                    sendTilBeslutter.preValiderSteg(behandling, stegService)
                }

            assertThat(feil.message).isEqualTo("Kan ikke sende tilbakekrevingsvedtak ved motregning til beslutter hvis samtykke ikke er bekreftet.")
        }
    }

    @Test
    fun `Sjekk at validering er bakoverkompatibel med endring i stegrekkefølge`() {
        val behandling = lagBehandling(førsteSteg = StegType.REGISTRERE_SØKNAD)
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)

        assertTrue(behandling.validerRekkefølgeOgUnikhetPåSteg())
    }

    @Test
    fun `Sjekk validering med gyldig stegrekkefølge`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_SØKNAD)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)

        assertTrue(behandling.validerRekkefølgeOgUnikhetPåSteg())
    }

    @Test
    fun `Sjekk validering med ugyldig flere steg av samme type`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_SØKNAD)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = StegType.VILKÅRSVURDERING,
            ),
        )

        assertThrows<Feil> {
            behandling.validerRekkefølgeOgUnikhetPåSteg()
        }
    }

    @Test
    fun `Sjekk validering med ugyldig stegrekkefølge`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_SØKNAD)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = StegType.VILKÅRSVURDERING,
            ),
        )

        assertThrows<Feil> {
            behandling.validerRekkefølgeOgUnikhetPåSteg()
        }
    }

    @Test
    fun `Sjekk validering som inneholder annen vurdering som ikke er vurdert`() {
        val vilkårsvurdering =
            lagVilkårsvurdering(randomAktør(), lagBehandling(), Resultat.IKKE_VURDERT)

        assertThrows<FunksjonellFeil> {
            vilkårsvurdering.validerAtAndreVurderingerErVurdert()
        }
    }

    @Test
    fun `Sjekk validering som inneholder annen vurdering hvor alle er vurdert`() {
        val vilkårsvurdering =
            lagVilkårsvurdering(randomAktør(), lagBehandling(), Resultat.IKKE_OPPFYLT)

        vilkårsvurdering.validerAtAndreVurderingerErVurdert()
    }
}
