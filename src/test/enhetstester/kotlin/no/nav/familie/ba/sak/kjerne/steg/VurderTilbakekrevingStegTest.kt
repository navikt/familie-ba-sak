package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
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
    private val unleashService: UnleashNextMedContextService = mockk()

    private val vurderTilbakekrevingSteg: VurderTilbakekrevingSteg =
        VurderTilbakekrevingSteg(tilbakekrevingService = tilbakekrevingService, simuleringService = simuleringService, unleashService = unleashService)

    private val behandling: Behandling =
        lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            førsteSteg = StegType.VURDER_TILBAKEKREVING,
        )
    private val restTilbakekreving: RestTilbakekreving =
        RestTilbakekreving(
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
        every { unleashService.isEnabled(toggle = any()) } returns true
    }

    @Test
    fun `skal utføre steg for vanlig behandling uten åpen tilbakekreving`() {
        val stegType =
            assertDoesNotThrow {
                vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                    behandling,
                    restTilbakekreving,
                )
            }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        verify(exactly = 1) { tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id) }
        verify(exactly = 1) { tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id) }
    }

    @Test
    fun `skal utføre steg for vanlig behandling med åpen tilbakekreving`() {
        every { tilbakekrevingService.søkerHarÅpenTilbakekreving(any()) } returns true
        val stegType =
            assertDoesNotThrow {
                vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                    behandling,
                    restTilbakekreving,
                )
            }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        verify(exactly = 0) { tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id) }
        verify(exactly = 0) { tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id) }
    }
}
