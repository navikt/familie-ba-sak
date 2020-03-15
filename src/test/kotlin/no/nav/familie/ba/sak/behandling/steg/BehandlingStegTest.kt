package no.nav.familie.ba.sak.behandling.steg

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingStegTest {
    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge = listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.VILKÅRSVURDERING,
                StegType.SEND_TIL_BESLUTTER,
                StegType.GODKJENNE_VEDTAK,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET)

        var steg = initSteg
        riktigRekkefølge.forEach {
            Assertions.assertEquals(steg, it)
            steg = it.hentNesteSteg()
        }
    }
}