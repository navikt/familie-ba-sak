package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.verdikjedetester.lagMockRestJournalføring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JournalføringUtilsTest {

    @Test
    fun `Skal utlede ordinær når søknad om ordinær journalføres`() {
        val søkerFnr = randomFnr()
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            lagMockRestJournalføring(
                bruker = NavnOgIdent("Mock", søkerFnr)
            ).copy(
                journalpostTittel = "Søknad om ordinær barnetrygd",
                opprettOgKnyttTilNyBehandling = true
            ).hentUnderkategori()
        )
    }

    @Test
    fun `Skal utlede utvidet når søknad om utvidet journalføres`() {
        val søkerFnr = randomFnr()
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            lagMockRestJournalføring(
                bruker = NavnOgIdent("Mock", søkerFnr)
            ).copy(
                journalpostTittel = "Søknad om utvidet barnetrygd",
                opprettOgKnyttTilNyBehandling = true
            ).hentUnderkategori()
        )
    }
}
