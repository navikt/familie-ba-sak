package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BehandlingStegTest {
    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge = listOf(
                StegType.REGISTRERE_SØKNAD,
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

        var steg = initSteg(BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingOpprinnelse.MANUELL)
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
        steg = initSteg(BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)
        riktigRekkefølgeForFødselshendelser.forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(utførendeStegType = steg,
                                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    behandlingOpprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)
        }
    }

    @Test
    fun testDisplayName() {
        assertEquals("Send til beslutter", StegType.SEND_TIL_BESLUTTER.displayName())
    }

    @Test
    fun testErKompatibelMed() {
        assertTrue(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.OPPRETTET))
        assertTrue(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.UNDERKJENT_AV_BESLUTTER))
        assertFalse(StegType.REGISTRERE_SØKNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSATT))
        assertFalse(StegType.BEHANDLING_AVSLUTTET.erGyldigIKombinasjonMedStatus(BehandlingStatus.OPPRETTET))
    }

    @Test
    fun testInitSteg() {
        assertEquals(StegType.REGISTRERE_PERSONGRUNNLAG,
                     initSteg(BehandlingType.MIGRERING_FRA_INFOTRYGD, BehandlingOpprinnelse.MANUELL))
        assertEquals(StegType.REGISTRERE_SØKNAD, initSteg(behandlingOpprinnelse = BehandlingOpprinnelse.MANUELL))
        assertEquals(StegType.REGISTRERE_PERSONGRUNNLAG,
                     initSteg(BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE))
    }
}