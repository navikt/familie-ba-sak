package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilbakestillBehandlingTilSimuleringServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilbakestillBehandlingTilSimuleringService =
        TilbakestillBehandlingTilSimuleringService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
        )

    @Test
    fun `skal ikke flytte behandlingen fremover når den står før simuleringssteget`() {
        // Arrange
        val behandling = lagBehandling(førsteSteg = StegType.VILKÅRSVURDERING)
        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        // Act
        val resultat = tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(behandling.id)

        // Assert
        assertThat(resultat).isEqualTo(behandling)
        verify(exactly = 0) { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(any(), any()) }
    }

    @Test
    fun `skal tilbakestille behandlingen når den står etter simuleringssteget`() {
        // Arrange
        val behandling = lagBehandling(førsteSteg = StegType.SEND_TIL_BESLUTTER)
        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        every { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.VURDER_TILBAKEKREVING) } returns behandling

        // Act
        tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(behandling.id)

        // Assert
        verify(exactly = 1) { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.VURDER_TILBAKEKREVING) }
    }
}
