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
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        Assert.assertTrue(behandling.behandlingStegTilstand.first { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.first { it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.first { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT)
    }

    @Test
    fun `Verifiser maks et steg av hver type`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT)
    }

    @Test
    fun `Verifiser at alle steg med høyere rekkefølge enn siste fjernes`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)

        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.none { it.behandlingSteg == StegType.SEND_TIL_BESLUTTER })
    }

    @Test
    fun `Verifiser henlegg søknad ikke endrer stegstatus`() {
        val behandling = opprettBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.REGISTRERE_PERSONGRUNNLAG)
        behandling.leggTilBehandlingStegTilstand(StegType.VILKÅRSVURDERING)
        behandling.leggTilBehandlingStegTilstand(StegType.SEND_TIL_BESLUTTER)
        behandling.leggTilBehandlingStegTilstand(StegType.HENLEGG_SØKNAD)

        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_SØKNAD }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.VILKÅRSVURDERING }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.SEND_TIL_BESLUTTER }.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT)
        Assert.assertTrue(behandling.behandlingStegTilstand.single { it.behandlingSteg == StegType.HENLEGG_SØKNAD }.behandlingStegStatus == BehandlingStegStatus.UTFØRT)
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
