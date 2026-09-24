package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AutovedtakServiceTest {
    private val stegService = mockk<StegService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()
    private val loggService = mockk<LoggService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val tilbakestillBehandlingTilBehandlingsresultatService = mockk<TilbakestillBehandlingTilBehandlingsresultatService>()

    private val autovedtakService =
        AutovedtakService(
            stegService = stegService,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            loggService = loggService,
            totrinnskontrollService = totrinnskontrollService,
            tilbakestillBehandlingTilBehandlingsresultatService = tilbakestillBehandlingTilBehandlingsresultatService,
        )

    private val fagsak = lagFagsak()

    private val nyBehandling =
        NyBehandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
            fagsakId = fagsak.id,
        )

    private val filtrerAutomatiskBehandlingData =
        FiltrerAutomatiskBehandlingData(
            søkersIdent = fagsak.aktør.aktivFødselsnummer(),
            barnasIdenter = emptyList(),
        )

    private val opprettetBehandling = lagAutomatiskBehandling(steg = StegType.FILTRERING_AUTOMATISK_BEHANDLING)

    private fun lagAutomatiskBehandling(
        steg: StegType,
        årsak: BehandlingÅrsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
    ) = lagBehandling(
        fagsak = fagsak,
        årsak = årsak,
        skalBehandlesAutomatisk = true,
        førsteSteg = steg,
    )

    @Nested
    inner class OpprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat {
        @BeforeEach
        fun setup() {
            every { stegService.håndterNyBehandling(any()) } returns opprettetBehandling
        }

        @Test
        fun `skal ikke kjøre vilkårsvurdering når filtreringsreglene har satt behandlingen til henleggelse`() {
            // Arrange
            val behandlingSomSkalHenlegges = lagAutomatiskBehandling(steg = StegType.HENLEGG_BEHANDLING)

            every {
                stegService.håndterFiltreringsreglerForAutomatiskeBehandlinger(opprettetBehandling, filtrerAutomatiskBehandlingData)
            } returns behandlingSomSkalHenlegges

            // Act
            val behandling =
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    nyBehandling = nyBehandling,
                    filtrerAutomatiskBehandlingData = filtrerAutomatiskBehandlingData,
                )

            // Assert
            assertThat(behandling.steg).isEqualTo(StegType.HENLEGG_BEHANDLING)
            verify(exactly = 0) { stegService.håndterVilkårsvurdering(any(), any()) }
        }

        @Test
        fun `skal kjøre vilkårsvurdering på behandlingen fra filtreringssteget når filtreringsreglene er oppfylt`() {
            // Arrange
            val behandlingEtterFiltrering = lagAutomatiskBehandling(steg = StegType.VILKÅRSVURDERING)
            val behandlingEtterBehandlingsresultat = lagAutomatiskBehandling(steg = StegType.IVERKSETT_MOT_OPPDRAG)

            every {
                stegService.håndterFiltreringsreglerForAutomatiskeBehandlinger(opprettetBehandling, filtrerAutomatiskBehandlingData)
            } returns behandlingEtterFiltrering
            every { stegService.håndterVilkårsvurdering(behandlingEtterFiltrering, null) } returns behandlingEtterBehandlingsresultat

            // Act
            val behandling =
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    nyBehandling = nyBehandling,
                    filtrerAutomatiskBehandlingData = filtrerAutomatiskBehandlingData,
                )

            // Assert
            assertThat(behandling).isEqualTo(behandlingEtterBehandlingsresultat)
            verify(exactly = 1) { stegService.håndterVilkårsvurdering(behandlingEtterFiltrering, null) }
        }

        @Test
        fun `skal ikke kjøre filtreringsregler for andre behandlingsårsaker enn automatisk behandling av søknad`() {
            // Arrange
            val behandlingEtterBehandlingsresultat =
                lagAutomatiskBehandling(
                    steg = StegType.IVERKSETT_MOT_OPPDRAG,
                    årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                )

            every { stegService.håndterVilkårsvurdering(opprettetBehandling, null) } returns behandlingEtterBehandlingsresultat

            // Act
            val behandling =
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    nyBehandling = nyBehandling.copy(behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG),
                    filtrerAutomatiskBehandlingData = filtrerAutomatiskBehandlingData,
                )

            // Assert
            assertThat(behandling).isEqualTo(behandlingEtterBehandlingsresultat)
            verify(exactly = 0) { stegService.håndterFiltreringsreglerForAutomatiskeBehandlinger(any(), any()) }
        }
    }
}
