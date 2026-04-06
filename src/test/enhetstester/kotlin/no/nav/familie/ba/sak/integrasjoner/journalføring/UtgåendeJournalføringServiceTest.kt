package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.OverstyrInnsynsregel
import no.nav.familie.restklient.client.RessursException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
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

            val fagsak = lagFagsak()

            // Act
            val journalpostId = utgåendeJournalføringService.journalførDokument(fagsak = fagsak, brev = emptyList(), eksternReferanseId = "1234")

            // Assert
            verify(exactly = 1) { integrasjonKlient.journalførDokument(any()) }
            assertThat(journalpostId).isNotNull
        }

        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["SKJERMET_BARN"], mode = EXCLUDE)
        fun `skal ikke overstyre journalpost med overstyrt innsynsregel ved ikke skjermet barn fagsak`(fagsakType: FagsakType) {
            // Arrange
            val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()

            every { integrasjonKlient.journalførDokument(capture(arkiverDokumentRequestSlot)) } returns ArkiverDokumentResponse(journalpostId = "1", ferdigstilt = true)

            val fagsak = lagFagsak(type = fagsakType)

            // Act
            utgåendeJournalføringService.journalførDokument(fagsak = fagsak, brev = emptyList(), eksternReferanseId = "1234")

            // Assert
            val arkiverDokumentRequest = arkiverDokumentRequestSlot.captured

            assertThat(arkiverDokumentRequest.overstyrInnsynsregler).isNull()
        }

        @Test
        fun `skal opprette journalpost med overstyrt innsynsregel SKJULES_BRUKERS_SIKKERHET dersom fagsak er skjermet barn`() {
            // Arrange
            val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()

            every { integrasjonKlient.journalførDokument(capture(arkiverDokumentRequestSlot)) } returns ArkiverDokumentResponse(journalpostId = "1", ferdigstilt = true)

            val fagsak = lagFagsak(type = FagsakType.SKJERMET_BARN)

            // Act
            utgåendeJournalføringService.journalførDokument(fagsak = fagsak, brev = emptyList(), eksternReferanseId = "1234")

            // Assert
            val arkiverDokumentRequest = arkiverDokumentRequestSlot.captured

            assertThat(arkiverDokumentRequest.overstyrInnsynsregler).isEqualTo(OverstyrInnsynsregel.SKJULES_BRUKERS_SIKKERHET)
        }

        @Test
        fun `skal kaste feil dersom journalpost med eksternReferanseId allerede finnes`() {
            // Arrange
            every { integrasjonKlient.journalførDokument(any()) } throws RessursException(ressurs = mockk(), httpStatus = HttpStatus.CONFLICT, cause = mockk())

            val fagsak = lagFagsak()

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
            val journalpostId = utgåendeJournalføringService.journalførDokument(fagsak = fagsak, brev = emptyList(), eksternReferanseId = "1234")

            // Assert
            verify(exactly = 1) { integrasjonKlient.journalførDokument(any()) }
            verify(exactly = 1) { integrasjonKlient.hentJournalposterForBruker(any()) }
            assertThat(journalpostId).isEqualTo(eksisterendeJournalpost.journalpostId)
        }
    }
}
