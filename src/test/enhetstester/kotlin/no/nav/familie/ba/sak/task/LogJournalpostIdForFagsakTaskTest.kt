package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LogJournalpostIdForFagsakTaskTest {
    val mockIntegrasjonKlient = mockk<IntegrasjonKlient>()
    val mockFagsakRepository = mockk<FagsakRepository>()

    val logJournalpostIdForFagsak = LogJournalpostIdForFagsakTask(mockIntegrasjonKlient, mockFagsakRepository)

    @Test
    fun `Skal kaste feil dersom fagsak med id ikke finnes`() {
        // Arrange
        val fagsak = lagFagsak()
        val task = LogJournalpostIdForFagsakTask.opprettTask(fagsak.id.toString())

        every { mockFagsakRepository.finnFagsak(fagsak.id) } returns null

        every { mockIntegrasjonKlient.hentJournalposterForBruker(any()) } returns
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

        every { mockIntegrasjonKlient.hentJournalposterForBruker(any()) } returns
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
        verify(exactly = 1) { mockIntegrasjonKlient.hentJournalposterForBruker(any()) }
    }
}
