package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.IVERKSETT_MOT_FAMILIE_TILBAKE,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD
                ).copy(
                    resultat = BehandlingResultat.INNVILGET
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av avslått søknad`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.REGISTRERE_SØKNAD,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD
                ).copy(
                    resultat = BehandlingResultat.AVSLÅTT
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av fødselshendelser ved innvilgelse`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.FILTRERING_FØDSELSHENDELSER,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                ).copy(resultat = BehandlingResultat.INNVILGET),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av fødselshendelser ved avslag`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.FILTRERING_FØDSELSHENDELSER,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.HENLEGG_BEHANDLING,
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                ).copy(resultat = BehandlingResultat.AVSLÅTT),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av type migrering fra infotrygd`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            assertNotEquals(StegType.JOURNALFØR_VEDTAKSBREV, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.TEKNISK_OPPHØR
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av type migrering fra infotrygd opphørt`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            assertNotEquals(StegType.JOURNALFØR_VEDTAKSBREV, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                    årsak = BehandlingÅrsak.TEKNISK_OPPHØR
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av type teknisk opphør`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            assertNotEquals(StegType.JOURNALFØR_VEDTAKSBREV, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    behandlingType = BehandlingType.TEKNISK_OPPHØR,
                    årsak = BehandlingÅrsak.TEKNISK_OPPHØR
                ),
                utførendeStegType = it
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
