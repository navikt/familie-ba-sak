package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.datagenerator.lagTilgangsstyrtJournalpost
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class JournalføringControllerTest {
    private val innkommendeJournalføringService = mockk<InnkommendeJournalføringService>()
    private val tilgangService: TilgangService = mockk()

    private val journalføringController: JournalføringController =
        JournalføringController(
            innkommendeJournalføringService = innkommendeJournalføringService,
            tilgangService = tilgangService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class HentJournalposterForBruker {
        @Test
        fun `skal returnere liste av tilgangsstyrte journalposter`() {
            // Arrange
            val personIdent = PersonIdent(randomFnr())
            val journalpostId = "1"

            every { innkommendeJournalføringService.hentJournalposterForBruker(personIdent.ident) } returns
                listOf(
                    lagTilgangsstyrtJournalpost(personIdent.ident, journalpostId = journalpostId, harTilgang = true),
                )

            // Act
            val responseEntity = journalføringController.hentJournalposterForBruker(personIdentBody = personIdent)

            // Assert
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(responseEntity.body).isNotNull
            assertThat(responseEntity.body!!.data).isNotNull
            val journalposter = responseEntity.body!!.data!!
            assertThat(journalposter).hasSize(1)
            val tilgangsstyrtJournalpost = journalposter.single()
            assertThat(tilgangsstyrtJournalpost.journalpost.journalpostId).isEqualTo("1")
            assertThat(tilgangsstyrtJournalpost.journalpostTilgang.harTilgang).isTrue
        }
    }

    @Nested
    inner class Tilgangskontroll {
        private val fødselsnummer = randomFnr()
        private val journalpostId = "1"
        private val dokumentInfoId = "2"
        private val journalpost = lagTestJournalpost(personIdent = fødselsnummer, journalpostId = journalpostId, avsenderMottakerIdType = null, kanal = "NAV_NO")

        @BeforeEach
        fun setUp() {
            every { innkommendeJournalføringService.hentJournalpost(journalpostId) } returns journalpost
            every { innkommendeJournalføringService.hentDokument(journalpostId, dokumentInfoId) } returns ByteArray(0)
            every { innkommendeJournalføringService.hentJournalposterForBruker(fødselsnummer) } returns emptyList()
        }

        @Test
        fun `skal kreve veilederrolle ved henting av journalposter for bruker`() {
            // Act
            journalføringController.hentJournalposterForBruker(PersonIdent(fødselsnummer))

            // Assert
            verify(exactly = 1) {
                tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente journalposter for bruker")
            }
        }

        @Test
        fun `skal ikke hente journalposter for bruker når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente journalposter for bruker")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { journalføringController.hentJournalposterForBruker(PersonIdent(fødselsnummer)) }

            verify(exactly = 0) { innkommendeJournalføringService.hentJournalposterForBruker(any()) }
        }

        @Test
        fun `skal kreve veilederrolle for å hente journalpost`() {
            // Act
            journalføringController.hentJournalpost(journalpostId)

            // Assert
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente journalpost") }
        }

        @Test
        fun `skal kreve veilederrolle for å hente dokument`() {
            // Act
            journalføringController.hentDokument(journalpostId, dokumentInfoId)

            // Assert
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente dokument") }
        }

        @Test
        fun `skal kreve veilederrolle for å hente dokument som PDF`() {
            // Act
            journalføringController.hentDokumentBytearray(journalpostId, dokumentInfoId)

            // Assert
            verify(exactly = 1) { tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente dokument") }
        }

        @Test
        fun `skal ikke hente dokument når kaller mangler veilederrolle`() {
            // Arrange
            every {
                tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER, handling = "Hente dokument")
            } throws RolleTilgangskontrollFeil("Krever VEILEDER")

            // Act & Assert
            assertThrows<RolleTilgangskontrollFeil> { journalføringController.hentDokument(journalpostId, dokumentInfoId) }
            assertThrows<RolleTilgangskontrollFeil> { journalføringController.hentDokumentBytearray(journalpostId, dokumentInfoId) }

            verify(exactly = 0) { innkommendeJournalføringService.hentDokument(any(), any()) }
        }
    }
}
