package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal

class VurderTilbakekrevingStegTest {
    private val tilbakekrevingService: TilbakekrevingService = mockk()
    private val simuleringService: SimuleringService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val vurderTilbakekrevingSteg: VurderTilbakekrevingSteg =
        VurderTilbakekrevingSteg(tilbakekrevingService = tilbakekrevingService, simuleringService = simuleringService)

    private val behandling: Behandling =
        lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )
    private val tilbakekrevingDto: TilbakekrevingDto =
        TilbakekrevingDto(
            valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            varsel = "testverdi",
            begrunnelse = "testverdi",
        )

    @BeforeEach
    fun setup() {
        every { tilbakekrevingService.søkerHarÅpenTilbakekreving(any()) } returns false
        every { tilbakekrevingService.validerRestTilbakekreving(any(), any()) } returns Unit
        every { tilbakekrevingService.lagreTilbakekreving(any(), any()) } returns null
        every { simuleringService.hentEtterbetaling(any(classifier = Long::class)) } returns BigDecimal.ZERO
        every { featureToggleService.isEnabled(toggle = any()) } returns true
    }

    @Test
    fun `skal utføre steg for vanlig behandling uten åpen tilbakekreving`() {
        val stegType =
            assertDoesNotThrow {
                vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                    behandling,
                    tilbakekrevingDto,
                )
            }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        verify(exactly = 1) { tilbakekrevingService.validerRestTilbakekreving(tilbakekrevingDto, behandling.id) }
        verify(exactly = 1) { tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto, behandling.id) }
    }

    @Test
    fun `skal utføre steg for vanlig behandling med åpen tilbakekreving`() {
        every { tilbakekrevingService.søkerHarÅpenTilbakekreving(any()) } returns true
        val stegType =
            assertDoesNotThrow {
                vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                    behandling,
                    tilbakekrevingDto,
                )
            }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        verify(exactly = 0) { tilbakekrevingService.validerRestTilbakekreving(tilbakekrevingDto, behandling.id) }
        verify(exactly = 0) { tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto, behandling.id) }
    }
}
