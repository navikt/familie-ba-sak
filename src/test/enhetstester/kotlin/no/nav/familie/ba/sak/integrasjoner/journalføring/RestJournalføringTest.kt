package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.kjerne.verdikjedetester.lagMockRestJournalføring
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Sak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RestJournalføringTest {
    @Nested
    inner class OppdaterMedDokumentOgSak {
        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til UTL_ORG dersom oppgavetype er BEH_SED`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val oppgaveType = "BEH_SED"
            val restJournalføring = lagMockRestJournalføring(NavnOgIdent("testbruker", "testIdent"))

            // Act
            val oppdaterJournalpostRequest = restJournalføring.oppdaterMedDokumentOgSak(sak, oppgaveType)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.UTL_ORG)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til FNR dersom ident er fylt ut og det ikke er BEH_SED`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val oppgaveType = "BEH_SAK"
            val restJournalføring = lagMockRestJournalføring(NavnOgIdent("testbruker", "testIdent"))

            // Act
            val oppdaterJournalpostRequest = restJournalføring.oppdaterMedDokumentOgSak(sak, oppgaveType)

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

            val oppgaveType = "BEH_SAK"
            val restJournalføring = lagMockRestJournalføring(NavnOgIdent("testbruker", ""))

            // Act
            val oppdaterJournalpostRequest = restJournalføring.oppdaterMedDokumentOgSak(sak, oppgaveType)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isNull()
        }
    }
}
