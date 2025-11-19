package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilbakekrevingTest {
    @Test
    fun `Skal ikke være mulig å opprette en tilbakekreving med varsel uten varsel`() {
        assertThrows<FunksjonellFeil> {
            Tilbakekreving(
                behandling = lagBehandling(),
                valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                varsel = null,
                begrunnelse = "",
                tilbakekrevingsbehandlingId = null,
            )
        }
    }
}
