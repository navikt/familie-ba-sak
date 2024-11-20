package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows

internal class LogFagsakIdForJournalpostTaskTest {
    val mockIntegrasjonClient = mockk<IntegrasjonClient>()

    val logFagsakIdForJournalpostTask = LogFagsakIdForJournalpostTask(mockIntegrasjonClient)

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

        every { mockIntegrasjonClient.hentJournalpost("12345") } returns journalpost

        // Act
        logFagsakIdForJournalpostTask.doTask(task)

        // Assert
        verify(exactly = 1) { mockIntegrasjonClient.hentJournalpost("12345") }
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

        every { mockIntegrasjonClient.hentJournalpost("12345") } returns journalpost

        // Act && Assert

        val feilmelding =
            assertThrows<Feil> {
                logFagsakIdForJournalpostTask.doTask(task)
            }.message

        assertThat(feilmelding).contains("Fant ikke fagsak informasjon i journalpost 12345")

        // Assert
        verify(exactly = 1) { mockIntegrasjonClient.hentJournalpost("12345") }
    }
}
