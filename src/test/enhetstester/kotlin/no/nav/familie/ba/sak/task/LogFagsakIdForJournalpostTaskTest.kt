package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LogFagsakIdForJournalpostTaskTest {
    val mockIntegrasjonKlient = mockk<IntegrasjonKlient>()

    val logFagsakIdForJournalpostTask = LogFagsakIdForJournalpostTask(mockIntegrasjonKlient)

    @Test
    fun `Skal hente journalpost og logge ut sak data`() {
        // Arrange
        val task = LogFagsakIdForJournalpostTask.opprettTask("12345")

        val journalpost =
            lagTestJournalpost(
                personIdent = "12345",
                journalpostId = "12345",
                avsenderMottakerIdType = null,
                kanal = "NAV_NO",
            )

        every { mockIntegrasjonKlient.hentJournalpost("12345") } returns journalpost

        // Act
        logFagsakIdForJournalpostTask.doTask(task)

        // Assert
        verify(exactly = 1) { mockIntegrasjonKlient.hentJournalpost("12345") }
    }

    @Test
    fun `Skal kaste feil dersom journalpost ikke inneholder sak informasjon`() {
        // Arrange
        val task = LogFagsakIdForJournalpostTask.opprettTask("12345")

        val journalpost =
            lagTestJournalpost(
                personIdent = "12345",
                journalpostId = "12345",
                avsenderMottakerIdType = null,
                kanal = "NAV_NO",
                sak = null,
            )

        every { mockIntegrasjonKlient.hentJournalpost("12345") } returns journalpost

        // Act && Assert

        val feilmelding =
            assertThrows<Feil> {
                logFagsakIdForJournalpostTask.doTask(task)
            }.message

        assertThat(feilmelding).contains("Fant ikke fagsak informasjon i journalpost 12345")

        // Assert
        verify(exactly = 1) { mockIntegrasjonKlient.hentJournalpost("12345") }
    }
}
