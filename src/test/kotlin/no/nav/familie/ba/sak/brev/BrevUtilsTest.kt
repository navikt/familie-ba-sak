package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BrevUtilsTest {

    /**
     * Siden navnet til saksbehandler blir hentet fra sikkerhetscontext er det riktig at denne er system her.
     */
    @Test
    fun `Saksbehandler blir hentet fra sikkerhetscontext og beslutter viser placeholder tekst under behandling`() {
        val behandling = lagBehandling()

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = null
        )

        assertEquals("System", saksbehandler)
        assertEquals("Beslutter", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter er hentet fra sikkerhetscontext under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "Mock Saksbehandler",
                        saksbehandlerId = "mock.saksbehandler@nav.no"
                )
        )

        assertEquals("Mock Saksbehandler", saksbehandler)
        assertEquals("System", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter viser placeholder tekst under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "System",
                        saksbehandlerId = "systembruker"
                )
        )

        assertEquals("System", saksbehandler)
        assertEquals("Beslutter", beslutter)
    }

    @Test
    fun `Saksbehandler og beslutter blir hentet etter at totrinnskontroll er besluttet`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "Mock Saksbehandler",
                        saksbehandlerId = "mock.saksbehandler@nav.no",
                        beslutter = "Mock Beslutter",
                        beslutterId = "mock.beslutter@nav.no"
                )
        )

        assertEquals("Mock Saksbehandler", saksbehandler)
        assertEquals("Mock Beslutter", beslutter)
    }

    private val støttedeBehandlingsersultaterFørstegangsbehandling = listOf(
            BehandlingResultat.INNVILGET,
            BehandlingResultat.INNVILGET_OG_OPPHØRT,
            BehandlingResultat.DELVIS_INNVILGET,
            BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT,
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for førstegangsbehandling`() {

        støttedeBehandlingsersultaterFørstegangsbehandling.forEach {
            Assertions.assertEquals(
                    Vedtaksbrevtype.FØRSTEGANGSVEDTAK,
                    hentManuellVedtaksbrevtype(
                            BehandlingType.FØRSTEGANGSBEHANDLING,
                            it),
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype kaster exception for ikke-støttede behandlingsresultater ved førstegangsbehandling`() {
        val ikkeStøttedeBehandlingsersultater =
                BehandlingResultat.values().subtract(støttedeBehandlingsersultaterFørstegangsbehandling)

        ikkeStøttedeBehandlingsersultater.forEach {
            assertThrows<Exception> {
                hentManuellVedtaksbrevtype(BehandlingType.FØRSTEGANGSBEHANDLING,
                                           it)
            }
        }
    }

    val behandlingsersultaterForVedtakEndring = listOf(
            BehandlingResultat.INNVILGET,
            BehandlingResultat.INNVILGET_OG_ENDRET,
            BehandlingResultat.DELVIS_INNVILGET,
            BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET,
            BehandlingResultat.AVSLÅTT_OG_ENDRET,
            BehandlingResultat.ENDRET
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Vedtak endring'`() {
        behandlingsersultaterForVedtakEndring.forEach {
            Assertions.assertEquals(
                    Vedtaksbrevtype.VEDTAK_ENDRING,
                    hentManuellVedtaksbrevtype(
                            BehandlingType.REVURDERING,
                            it),
            )
        }
    }

    val behandlingsersultaterForOpphørt = listOf(BehandlingResultat.OPPHØRT)

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphørt'`() {
        behandlingsersultaterForOpphørt.forEach {
            Assertions.assertEquals(
                    Vedtaksbrevtype.OPPHØRT,
                    hentManuellVedtaksbrevtype(
                            BehandlingType.REVURDERING,
                            it),
            )
        }
    }

    val behandlingsersultaterForOpphørMedEndring = listOf(
            BehandlingResultat.INNVILGET_OG_OPPHØRT,
            BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT,
            BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT,
            BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
            BehandlingResultat.AVSLÅTT_OG_OPPHØRT,
            BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
            BehandlingResultat.ENDRET_OG_OPPHØRT,
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphør med endring'`() {
        behandlingsersultaterForOpphørMedEndring.forEach {
            Assertions.assertEquals(
                    Vedtaksbrevtype.OPPHØR_MED_ENDRING,
                    hentManuellVedtaksbrevtype(
                            BehandlingType.REVURDERING,
                            it),
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype kaster exception for ikke-støttede behandlingsresultater ved revurdering`() {
        val ikkeStøttedeBehandlingsersultater =
                BehandlingResultat.values()
                        .subtract(behandlingsersultaterForVedtakEndring)
                        .subtract(behandlingsersultaterForOpphørt)
                        .subtract(behandlingsersultaterForOpphørMedEndring)

        ikkeStøttedeBehandlingsersultater.forEach {
            assertThrows<Exception> {
                hentManuellVedtaksbrevtype(BehandlingType.REVURDERING,
                                           it)
            }
        }
    }
}