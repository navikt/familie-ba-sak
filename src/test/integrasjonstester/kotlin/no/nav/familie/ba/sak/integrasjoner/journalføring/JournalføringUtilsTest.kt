package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.datagenerator.lagMockJournalføringDto
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JournalføringUtilsTest {
    val ordinærJournalpostTittel = "Søknad om ordinær barnetrygd"
    val utvidetJournalpostTittel = "Søknad om utvidet barnetrygd"

    @Test
    fun `Skal utlede ordinær når søknad om ordinær journalføres`() {
        val søkerFnr = randomFnr()
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            lagMockJournalføringDto(
                bruker = NavnOgIdent("Mock", søkerFnr),
            ).copy(
                journalpostTittel = "Søknad om ordinær barnetrygd",
                opprettOgKnyttTilNyBehandling = true,
            ).hentUnderkategori(),
        )
    }

    @Test
    fun `Skal utlede utvidet når søknad om utvidet journalføres`() {
        val søkerFnr = randomFnr()
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            lagMockJournalføringDto(
                bruker = NavnOgIdent("Mock", søkerFnr),
            ).copy(
                journalpostTittel = utvidetJournalpostTittel,
                underkategori = BehandlingUnderkategori.UTVIDET,
                opprettOgKnyttTilNyBehandling = true,
            ).hentUnderkategori(),
        )
    }

    @Test
    fun `Skal utlede ordinær når søknad om ordinær journalføres, men underkategori ikke er satt`() {
        val søkerFnr = randomFnr()
        val underkategori: BehandlingUnderkategori =
            lagMockJournalføringDto(bruker = NavnOgIdent(navn = "Mock", søkerFnr))
                .copy(
                    journalpostTittel = ordinærJournalpostTittel,
                    kategori = null,
                    underkategori = null,
                    opprettOgKnyttTilNyBehandling = true,
                ).hentUnderkategori()
        assertEquals(BehandlingUnderkategori.ORDINÆR, underkategori)
    }

    @Test
    fun `Skal utlede ordinær når søknad om ordinær journalføres, og underkategori er satt til ordinær`() {
        val søkerFnr = randomFnr()
        val underkategori: BehandlingUnderkategori =
            lagMockJournalføringDto(bruker = NavnOgIdent(navn = "Mock", søkerFnr))
                .copy(
                    journalpostTittel = ordinærJournalpostTittel,
                    kategori = null,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    opprettOgKnyttTilNyBehandling = true,
                ).hentUnderkategori()
        assertEquals(BehandlingUnderkategori.ORDINÆR, underkategori)
    }

    @Test
    fun `Skal utlede utvidet når søknad om utvidet journalføres, men underkategori ikke er satt`() {
        val søkerFnr = randomFnr()
        val underkategori: BehandlingUnderkategori =
            lagMockJournalføringDto(bruker = NavnOgIdent(navn = "Mock", søkerFnr))
                .copy(
                    journalpostTittel = utvidetJournalpostTittel,
                    kategori = null,
                    underkategori = null,
                    opprettOgKnyttTilNyBehandling = true,
                ).hentUnderkategori()
        assertEquals(BehandlingUnderkategori.UTVIDET, underkategori)
    }

    @Test
    fun `Skal utlede utvidet når søknad om utvidet journalføres, og underkategori er satt til utvidet`() {
        val søkerFnr = randomFnr()
        val underkategori: BehandlingUnderkategori =
            lagMockJournalføringDto(bruker = NavnOgIdent(navn = "Mock", søkerFnr))
                .copy(
                    journalpostTittel = utvidetJournalpostTittel,
                    kategori = null,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                    opprettOgKnyttTilNyBehandling = true,
                ).hentUnderkategori()
        assertEquals(BehandlingUnderkategori.UTVIDET, underkategori)
    }
}
