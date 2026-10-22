package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatStegValideringService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BehandlingsresultatStegTest {
    private val behandlingService: BehandlingService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val vedtaksperiodeService: VedtaksperiodeService = mockk()
    private val behandlingsresultatService: BehandlingsresultatService = mockk()
    private val beregningService: BeregningService = mockk()
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk()
    private val behandlingsresultatstegValideringService: BehandlingsresultatStegValideringService = mockk()

    private val behandlingsresultatSteg: BehandlingsresultatSteg =
        BehandlingsresultatSteg(
            behandlingService = behandlingService,
            simuleringService = mockk(),
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingsresultatService = behandlingsresultatService,
            beregningService = beregningService,
            småbarnstilleggService = mockk(),
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            behandlingsresultatstegValideringService = behandlingsresultatstegValideringService,
        )

    @Nested
    inner class PreValiderSteg {
        @Test
        fun `skal validere behandling før steg utføres`() {
            val behandling = lagBehandling()

            justRun { behandlingsresultatstegValideringService.validerBehandlingFørBehandlingsresultat(behandling) }

            behandlingsresultatSteg.preValiderSteg(behandling)

            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerBehandlingFørBehandlingsresultat(behandling)
            }
        }
    }

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @BeforeEach
        fun init() {
            justRun { tilbakestillBehandlingService.slettTilbakekrevingsvedtakMotregningHvisBehandlingIkkeAvregner(any()) }
        }

        @Test
        fun `Skal gå rett fra behandlingsresultat til iverksetting for alle fødselshendelser`() {
            // Arrange
            val fødselshendelseBehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                    skalBehandlesAutomatisk = true,
                )

            every { behandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.INNVILGET_OG_ENDRET
            every { behandlingService.nullstillEndringstidspunkt(fødselshendelseBehandling.id) } just runs
            every { behandlingService.oppdaterBehandlingsresultat(any(), any()) } returns
                fødselshendelseBehandling.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)
            every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns
                fødselshendelseBehandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)
            every { vedtakService.hentAktivForBehandlingThrows(fødselshendelseBehandling.id) } returns lagVedtak(fødselshendelseBehandling)
            every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(any()) } just runs
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(fødselshendelseBehandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING

            // Act
            val nesteSteg = behandlingsresultatSteg.utførStegOgAngiNeste(fødselshendelseBehandling, "")

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_OPPDRAG)
        }
    }
}
