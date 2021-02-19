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

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for førstegansgsbrev`() {
        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        BehandlingResultat.INNVILGET),
                Vedtaksbrevtype.FØRSTEGANGSVEDTAK)

        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        BehandlingResultat.INNVILGET_OG_OPPHØRT),
                Vedtaksbrevtype.FØRSTEGANGSVEDTAK)

        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        BehandlingResultat.DELVIS_INNVILGET),
                Vedtaksbrevtype.FØRSTEGANGSVEDTAK)
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Vedtak endring'`() {
        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.REVURDERING,
                        BehandlingResultat.INNVILGET),
                Vedtaksbrevtype.VEDTAK_ENDRING)

        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.REVURDERING,
                        BehandlingResultat.DELVIS_INNVILGET),
                Vedtaksbrevtype.VEDTAK_ENDRING)
    }


    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphørt'`() {
        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.REVURDERING,
                        BehandlingResultat.OPPHØRT),
                Vedtaksbrevtype.OPPHØRT)
    }


    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphørt med endring'`() {
        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.REVURDERING,
                        BehandlingResultat.INNVILGET_OG_OPPHØRT),
                Vedtaksbrevtype.OPPHØRT_ENDRING)

        Assertions.assertEquals(
                hentManuellVedtaksbrevtype(
                        BehandlingType.REVURDERING,
                        BehandlingResultat.ENDRET_OG_OPPHØRT),
                Vedtaksbrevtype.OPPHØRT_ENDRING)
    }
}