package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingstemaUtilsTest {

    @Test
    fun `skal bestemme kategori til EØS når saksbehandler bytter behandlingstema på en NASJONAL sak`() {
        val bestemtKategori = bestemKategori(
            overstyrtKategori = BehandlingKategori.EØS,
            kategoriFraInneværendeBehandling = BehandlingKategori.NASJONAL
        )
        assertEquals(BehandlingKategori.EØS, bestemtKategori)
    }

    @Test
    fun `skal bestemme kategori til NASJONAL når saksbehandler bytter behandlingstema på en EØS sak`() {
        val bestemtKategori = bestemKategori(
            overstyrtKategori = BehandlingKategori.NASJONAL,
            // default verdi, antar vilkårresultat har ikke endret på aktiv behandling
            kategoriFraInneværendeBehandling = BehandlingKategori.NASJONAL
        )
        assertEquals(BehandlingKategori.NASJONAL, bestemtKategori)
    }

    @Test
    fun `skal bestemme kategori til EØS ved opprettelse av revurdering på en EØS sak`() {
        val bestemtKategori = bestemKategori(
            overstyrtKategori = null,
            kategoriFraLøpendeBehandling = BehandlingKategori.EØS,
            // default verdi, siden det finnes ingen endring på vilkår resulat ved opprettelse
            kategoriFraInneværendeBehandling = BehandlingKategori.NASJONAL
        )
        assertEquals(BehandlingKategori.EØS, bestemtKategori)
    }
}
