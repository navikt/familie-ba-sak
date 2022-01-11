package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
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
import org.junit.jupiter.api.assertThrows

class BehandlingStegTest {

    @Test
    fun `Tester rekkefølgen på behandling MIGRERING`() {
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
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.MIGRERING
                ).copy(
                    resultat = BehandlingResultat.INNVILGET
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling ENDRE_MIGRERINGSDATO`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
            StegType.SEND_TIL_BESLUTTER,
            StegType.BESLUTTE_VEDTAK,
            StegType.IVERKSETT_MOT_OPPDRAG,
            StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(steg, it)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO
                ).copy(
                    resultat = BehandlingResultat.INNVILGET
                ),
                utførendeStegType = it
            )
        }
    }

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
                    årsak = BehandlingÅrsak.MIGRERING
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
                    årsak = BehandlingÅrsak.MIGRERING
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av type teknisk endring`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.VURDER_TILBAKEKREVING,
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
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    årsak = BehandlingÅrsak.TEKNISK_ENDRING
                ),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av omregn 18 år`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
            StegType.JOURNALFØR_VEDTAKSBREV,
            StegType.DISTRIBUER_VEDTAKSBREV,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_AVSLUTTET
        ).forEach {
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.OMREGNING_18ÅR,
                ).copy(resultat = BehandlingResultat.INNVILGET),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen til manuell behandling med årsak småbarnstillegg`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
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
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                ).copy(resultat = BehandlingResultat.INNVILGET),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av ÅRLIG_KONTROLL, som er test av else gren`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
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
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ).copy(resultat = BehandlingResultat.INNVILGET),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av satsendring`() {
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
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.SATSENDRING,
                ).copy(resultat = BehandlingResultat.ENDRET),
                utførendeStegType = it
            )
        }
    }

    @Test
    fun `Tester at man ikke får lov til å komme videre etter behandlingsresultat om resultatet ikke er ENDRET på satsendring`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
        ).forEach {
            assertEquals(it, steg)
            if (it == StegType.BEHANDLINGSRESULTAT) {
                assertThrows<Feil> {
                    hentNesteSteg(
                        behandling = lagBehandling(
                            årsak = BehandlingÅrsak.SATSENDRING,
                        ).copy(resultat = BehandlingResultat.AVSLÅTT_OG_ENDRET),
                        utførendeStegType = it
                    )
                }
            } else {
                steg = hentNesteSteg(
                    behandling = lagBehandling(
                        årsak = BehandlingÅrsak.SATSENDRING,
                    ).copy(resultat = BehandlingResultat.AVSLÅTT_OG_ENDRET),
                    utførendeStegType = it
                )
            }
        }
    }

    @Test
    fun `Tester at man ikke får lov til å komme videre etter behandlingsresultat om resultatet er FORTSATT_INNVILGET på migrering tilbake i tid`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
            StegType.VILKÅRSVURDERING,
            StegType.BEHANDLINGSRESULTAT,
        ).forEach {
            assertEquals(it, steg)
            if (it == StegType.BEHANDLINGSRESULTAT) {
                assertThrows<FunksjonellFeil> {
                    hentNesteSteg(
                        behandling = lagBehandling(
                            årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                        ).copy(resultat = BehandlingResultat.FORTSATT_INNVILGET),
                        utførendeStegType = it
                    )
                }
            } else {
                steg = hentNesteSteg(
                    behandling = lagBehandling(
                        årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                    ).copy(resultat = BehandlingResultat.FORTSATT_INNVILGET),
                    utførendeStegType = it
                )
            }
        }
    }

    @Test
    fun `Tester rekkefølgen på behandling av ÅRLIG_KONTROLL, som er test av else gren, med FORTSATT_INNVILGET`() {
        var steg = FØRSTE_STEG

        listOf(
            StegType.REGISTRERE_PERSONGRUNNLAG,
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
            assertEquals(it, steg)
            steg = hentNesteSteg(
                behandling = lagBehandling(
                    årsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                ).copy(resultat = BehandlingResultat.FORTSATT_INNVILGET),
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
