package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilbakestillBehandlingTilBehandlingsresultatServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()

    private val tilbakestillBehandlingTilBehandlingsresultatService =
        TilbakestillBehandlingTilBehandlingsresultatService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            vedtakRepository = vedtakRepository,
            tilbakekrevingService = tilbakekrevingService,
        )

    @Test
    fun `skal tilbakestille behandlingen til behandlingsresultat når behandlingen har passert behandlingsresultatet`() {
        // Arrange
        val behandling = lagBehandling(førsteSteg = StegType.VURDER_TILBAKEKREVING)
        val vedtak = lagVedtak(behandling = behandling)

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        every { vedtakRepository.findByBehandlingAndAktiv(behandling.id) } returns vedtak
        justRun { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak) }
        every { tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id) } returns Unit
        every { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.BEHANDLINGSRESULTAT) } returns behandling

        // Act
        tilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)

        // Assert
        verify(exactly = 1) { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak) }
        verify(exactly = 1) { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.BEHANDLINGSRESULTAT) }
    }

    @Test
    fun `skal ikke flytte behandlingen fram til behandlingsresultat når behandlingen står på vilkårsvurdering`() {
        // Arrange
        val behandling = lagBehandling(førsteSteg = StegType.VILKÅRSVURDERING)

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        // Act
        val resultat = tilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)

        // Assert
        assertThat(resultat.steg).isEqualTo(StegType.VILKÅRSVURDERING)
        verify(exactly = 0) { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(any(), any()) }
        verify(exactly = 0) { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(any()) }
        verify(exactly = 0) { tilbakekrevingService.slettTilbakekrevingPåBehandling(any()) }
    }
}
