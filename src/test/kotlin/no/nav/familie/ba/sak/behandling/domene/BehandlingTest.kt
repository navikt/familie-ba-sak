package no.nav.familie.ba.sak.behandling.domene

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
import org.junit.Assert
import org.junit.Test

class BehandlingTest() {

    @Test
    fun `Verifiser at siste steg får status IKKE_UTFØRT`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        Assert.assertTrue(behandling.behandlingStegTilstand.filter { it.behandlingSteg == StegType.VILKÅRSVURDERING }
                                  .first().behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.filter { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }
                                  .first().behandlingStegStatus == BehandlingStegStatus.UTFØRT)

    }

    @Test
    fun `Verifiser at to steg av samme type ikke kommer etter hverandre`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        Assert.assertTrue(behandling.behandlingStegTilstand.filter { it.behandlingSteg == StegType.VILKÅRSVURDERING }
                                  .single().behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT)
    }

    @Test
    fun `Verifiser at to steg av samme type kan opprettes om de ikke kommer etter hverandre`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        Assert.assertTrue(behandling.behandlingStegTilstand.filter { it.behandlingSteg == StegType.VILKÅRSVURDERING }
                                  .size == 2)
    }

    fun opprettBehandling(): Behandling {
        return Behandling(id = 1,
                          fagsak = mockk(),
                          kategori = BehandlingKategori.NASJONAL,
                          type = BehandlingType.FØRSTEGANGSBEHANDLING,
                          underkategori = BehandlingUnderkategori.ORDINÆR,
                          opprettetÅrsak = BehandlingÅrsak.SØKNAD).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, StegType.REGISTRERE_SØKNAD))
        }
    }
}
