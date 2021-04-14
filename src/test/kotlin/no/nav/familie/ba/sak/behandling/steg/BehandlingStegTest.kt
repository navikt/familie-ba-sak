package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.common.lagBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BehandlingStegTest {

    @Test
    fun `Tester rekkefølgen på behandling av søknad`() {
        var steg = FØRSTE_STEG

        listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.REGISTRERE_SØKNAD,
                StegType.VILKÅRSVURDERING,
                StegType.SIMULERING,
                StegType.SEND_TIL_BESLUTTER,
                StegType.BESLUTTE_VEDTAK,
                StegType.IVERKSETT_MOT_OPPDRAG,
                StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                StegType.JOURNALFØR_VEDTAKSBREV,
                StegType.DISTRIBUER_VEDTAKSBREV,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(
                    behandling = lagBehandling(
                            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                            årsak = BehandlingÅrsak.SØKNAD
                    ).copy(
                            resultat = BehandlingResultat.INNVILGET
                    ))
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av avslått søknad`() {
        var steg = FØRSTE_STEG

        listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.REGISTRERE_SØKNAD,
                StegType.VILKÅRSVURDERING,
                StegType.SIMULERING,
                StegType.SEND_TIL_BESLUTTER,
                StegType.BESLUTTE_VEDTAK,
                StegType.JOURNALFØR_VEDTAKSBREV,
                StegType.DISTRIBUER_VEDTAKSBREV,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(
                    behandling = lagBehandling(
                            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                            årsak = BehandlingÅrsak.SØKNAD
                    ).copy(
                            resultat = BehandlingResultat.AVSLÅTT
                    ))
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av fødselshendelser`() {
        var steg = FØRSTE_STEG

        listOf(
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.VILKÅRSVURDERING,
                StegType.IVERKSETT_MOT_OPPDRAG,
                StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                StegType.JOURNALFØR_VEDTAKSBREV,
                StegType.DISTRIBUER_VEDTAKSBREV,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(
                    behandling = lagBehandling(
                            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                            årsak = BehandlingÅrsak.FØDSELSHENDELSE
                    )
            )
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