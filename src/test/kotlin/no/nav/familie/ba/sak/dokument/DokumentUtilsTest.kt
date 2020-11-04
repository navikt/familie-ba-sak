package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

internal class DokumentUtilsTest {

    /**
     * Siden navnet til saksbehandler blir hentet fra sikkerhetscontext er det riktig at denne er system her.
     */
    @Test
    fun `Saksbehandler blir hentet fra sikkerhetscontext og beslutter er bindestrek under behandling`() {
        val behandling = lagBehandling()

        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = null
        )

        assertEquals("System", saksbehandler)
        assertEquals("-", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter er hentet fra sikkerhetscontext under beslutning`() {
        val behandling = lagBehandling()
        behandling.steg = StegType.BESLUTTE_VEDTAK

        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "Mock Saksbehandler"
                )
        )

        assertEquals("Mock Saksbehandler", saksbehandler)
        assertEquals("System", beslutter)
    }

    @Test
    fun `Saksbehandler og beslutter blir hentet etter at totrinnskontroll er besluttet`() {
        val behandling = lagBehandling()
        behandling.steg = StegType.BESLUTTE_VEDTAK

        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = Totrinnskontroll(
                        behandling = behandling,
                        saksbehandler = "Mock Saksbehandler",
                        beslutter = "Mock Beslutter"
                )
        )

        assertEquals("Mock Saksbehandler", saksbehandler)
        assertEquals("Mock Beslutter", beslutter)
    }
}