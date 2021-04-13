package no.nav.familie.ba.sak.behandling.resultat

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.steg.StegType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingsresultatTest {

    @Test
    fun `Skal trigge journalføringssteg på fortsatt innvilget og avslag`() {
        assertEquals(StegType.JOURNALFØR_VEDTAKSBREV,
                     BehandlingResultat.FORTSATT_INNVILGET.hentStegTypeBasertPåBehandlingsresultat())
        assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, BehandlingResultat.AVSLÅTT.hentStegTypeBasertPåBehandlingsresultat())
    }

    @Test
    fun `Skal trigge iverksetting på alt utenom fortsatt innvilget og avslag`() {
        BehandlingResultat.values().filter {
            it != BehandlingResultat.FORTSATT_INNVILGET && it != BehandlingResultat.AVSLÅTT
        }.forEach {
            assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, it.hentStegTypeBasertPåBehandlingsresultat())
        }
    }
}