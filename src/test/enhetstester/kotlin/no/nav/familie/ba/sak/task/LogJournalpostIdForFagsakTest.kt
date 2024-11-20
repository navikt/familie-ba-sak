package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class LogJournalpostIdForFagsakTest {
    val mockIntegrasjonClient = mockk<IntegrasjonClient>()
    val mockFagsakRepository = mockk<FagsakRepository>()

    val logJournalpostIdForFagsak = LogJournalpostIdForFagsakTask(mockIntegrasjonClient, mockFagsakRepository)

    @Test
    fun `Skal kaste feil dersom fagsak med id ikke finnes`() {
        // Arrange
        val fagsak = lagFagsak()
        val task = LogJournalpostIdForFagsakTask.opprettTask(fagsak.id.toString())

        every { mockFagsakRepository.finnFagsak(fagsak.id) } returns null

        every { mockIntegrasjonClient.hentJournalposterForBruker(any()) } returns
            listOf(
                lagTestJournalpost(
                    personIdent = "12345",
                    journalpostId = "12345",
                    avsenderMottakerIdType = null,
                    kanal = "NAV_NO",
                ),
            )

        // Act && Assert
        val feilmelding =
            assertThrows<Feil> {
                logJournalpostIdForFagsak.doTask(task)
            }.message

        assertThat(feilmelding).contains("Fagsak med id ${fagsak.id} ikke funnet ved forsøk på oppslag av journalposter")
    }

    @Test
    fun `Skal logge journalpost ider tilhørende fagsak`() {
        // Arrange
        val fagsak = lagFagsak()
        val task = LogJournalpostIdForFagsakTask.opprettTask(fagsak.id.toString())

        every { mockFagsakRepository.finnFagsak(fagsak.id) } returns fagsak

        every { mockIntegrasjonClient.hentJournalposterForBruker(any()) } returns
            listOf(
                lagTestJournalpost(
                    personIdent = "12345",
                    journalpostId = "12345",
                    avsenderMottakerIdType = null,
                    kanal = "NAV_NO",
                ),
            )

        // Act
        logJournalpostIdForFagsak.doTask(task)

        // Assert
        verify(exactly = 1) { mockIntegrasjonClient.hentJournalposterForBruker(any()) }
    }
}
