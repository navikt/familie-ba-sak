package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.datagenerator.lagMockJournalføringDto
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Sak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JournalføringDtoTest {
    @Nested
    inner class OppdaterMedDokumentOgSak {
        @Test
        fun `Skal beholde originalt avsender mottaker type dersom kanal er EESSI`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost = lagTestJournalpost("testIdent", "1", AvsenderMottakerIdType.UTL_ORG, "EESSI")
            val journalføringDto = lagMockJournalføringDto(NavnOgIdent("testbruker", "testIdent"))

            // Act
            val oppdaterJournalpostRequest = journalføringDto.oppdaterMedDokumentOgSak(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.UTL_ORG)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til FNR dersom ident er fylt ut`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalføringDto = lagMockJournalføringDto(NavnOgIdent("testbruker", "testIdent"))
            val journalpost = lagTestJournalpost("testIdent", "1", null, "NAV_NO")

            // Act
            val oppdaterJournalpostRequest = journalføringDto.oppdaterMedDokumentOgSak(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.FNR)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til null dersom ident er blank`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost = lagTestJournalpost("", "1", AvsenderMottakerIdType.FNR, "NAV_NO")
            val journalføringDto = lagMockJournalføringDto(NavnOgIdent("testbruker", ""))

            // Act
            val oppdaterJournalpostRequest = journalføringDto.oppdaterMedDokumentOgSak(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isNull()
        }
    }
}
