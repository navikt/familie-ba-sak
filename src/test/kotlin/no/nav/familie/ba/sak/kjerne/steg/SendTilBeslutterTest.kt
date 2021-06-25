package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SendTilBeslutterTest {

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
                behandlingSteg = StegType.VILKÅRSVURDERING
            )
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
                behandlingSteg = StegType.VILKÅRSVURDERING
            )
        )

        assertThrows<Feil> {
            behandling.validerRekkefølgeOgUnikhetPåSteg()
        }
    }

    @Test
    fun `Sjekk validering som inneholder annen vurdering som ikke er vurdert`() {
        val vilkårsvurdering = lagVilkårsvurdering(randomFnr(), lagBehandling(), Resultat.IKKE_VURDERT)

        assertThrows<FunksjonellFeil> {
            vilkårsvurdering.validerAtAlleAnndreVurderingerErVurdert()
        }
    }

    @Test
    fun `Sjekk validering som inneholder annen vurdering hvor alle er vurdert`() {
        val vilkårsvurdering = lagVilkårsvurdering(randomFnr(), lagBehandling(), Resultat.IKKE_OPPFYLT)

        vilkårsvurdering.validerAtAlleAnndreVurderingerErVurdert()
    }
}
