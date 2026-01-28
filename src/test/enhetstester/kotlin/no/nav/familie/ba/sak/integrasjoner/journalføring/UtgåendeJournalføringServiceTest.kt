package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.restklient.client.RessursException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class UtgåendeJournalføringServiceTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val utgåendeJournalføringService =
        UtgåendeJournalføringService(
            integrasjonKlient = integrasjonKlient,
        )

    @Nested
    inner class JournalførDokument {
        @Test
        fun `skal opprette journalpost og returnere journalpostId`() {
            // Arrange
            every { integrasjonKlient.journalførDokument(any()) } returns ArkiverDokumentResponse(journalpostId = "1", ferdigstilt = true)

            // Act
            val journalpostId = utgåendeJournalføringService.journalførDokument(fnr = "", fagsakId = "", brev = emptyList(), eksternReferanseId = "1234")

            // Assert
            verify(exactly = 1) { integrasjonKlient.journalførDokument(any()) }
            assertThat(journalpostId).isNotNull
        }

        @Test
        fun `skal kaste feil dersom journalpost med eksternReferanseId allerede finnes`() {
            // Arrange
            every { integrasjonKlient.journalførDokument(any()) } throws RessursException(ressurs = mockk(), httpStatus = HttpStatus.CONFLICT, cause = mockk())

            val eksisterendeJournalpost =
                mockk<Journalpost> {
                    every { eksternReferanseId } returns "1234"
                    every { journalpostId } returns "1"
                }
            every { integrasjonKlient.hentJournalposterForBruker(any()) } returns
                listOf(
                    eksisterendeJournalpost,
                )

            // Act
            val journalpostId = utgåendeJournalføringService.journalførDokument(fnr = "", fagsakId = "", brev = emptyList(), eksternReferanseId = "1234")

            // Assert
            verify(exactly = 1) { integrasjonKlient.journalførDokument(any()) }
            verify(exactly = 1) { integrasjonKlient.hentJournalposterForBruker(any()) }
            assertThat(journalpostId).isEqualTo(eksisterendeJournalpost.journalpostId)
        }
    }
}
