package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BehandlingStegTest {
    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge = listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.REGISTRERE_SØKNAD,
                StegType.VILKÅRSVURDERING,
                StegType.SEND_TIL_BESLUTTER,
                StegType.BESLUTTE_VEDTAK,
                StegType.IVERKSETT_MOT_OPPDRAG,
                StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                StegType.JOURNALFØR_VEDTAKSBREV,
                StegType.DISTRIBUER_VEDTAKSBREV,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET)

        var steg = FØRSTE_STEG
        riktigRekkefølge.forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(utførendeStegType = steg)
        }
        steg = StegType.REGISTRERE_PERSONGRUNNLAG
        val riktigRekkefølgeForInfotrygd = listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.VILKÅRSVURDERING,
                StegType.SEND_TIL_BESLUTTER,
                StegType.BESLUTTE_VEDTAK,
                StegType.IVERKSETT_MOT_OPPDRAG,
                StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                StegType.JOURNALFØR_VEDTAKSBREV,
                StegType.DISTRIBUER_VEDTAKSBREV,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET)
        riktigRekkefølgeForInfotrygd.forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(utførendeStegType = steg)
        }
        val riktigRekkefølgeForFødselshendelser = listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.VILKÅRSVURDERING,
                StegType.IVERKSETT_MOT_OPPDRAG,
                StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                StegType.JOURNALFØR_VEDTAKSBREV,
                StegType.DISTRIBUER_VEDTAKSBREV,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET)
        steg = FØRSTE_STEG
        riktigRekkefølgeForFødselshendelser.forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(utførendeStegType = steg,
                                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE)
        }
    }

    @Test
    fun testDisplayName() {
        assertEquals("Send til beslutter", StegType.SEND_TIL_BESLUTTER.displayName())
    }

    @Test
    fun testErKompatibelMed() {
        assertTrue(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertFalse(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertFalse(StegType.BEHANDLING_AVSLUTTET.erGyldigIKombinasjonMedStatus(BehandlingStatus.OPPRETTET))
    }
}