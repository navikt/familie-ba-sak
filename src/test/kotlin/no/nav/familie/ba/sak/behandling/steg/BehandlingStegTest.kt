package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingStegTest {
    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge = listOf(
                StegType.REGISTRERE_SØKNAD,
                StegType.VILKÅRSVURDERING,
                StegType.SEND_TIL_BESLUTTER,
                StegType.GODKJENNE_VEDTAK,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET)

        var steg = initSteg(BehandlingType.FØRSTEGANGSBEHANDLING)
        riktigRekkefølge.forEach {
            Assertions.assertEquals(steg, it)
            steg = it.hentNesteSteg(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        }
    }
}