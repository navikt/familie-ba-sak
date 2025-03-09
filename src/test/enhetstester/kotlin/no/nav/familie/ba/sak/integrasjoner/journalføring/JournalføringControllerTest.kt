package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagMockRestJournalføring
import no.nav.familie.ba.sak.datagenerator.lagTilgangsstyrtJournalpost
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class JournalføringControllerTest {
    private val innkommendeJournalføringService: InnkommendeJournalføringService = mockk()
    private val tilgangService: TilgangService = mockk()
    private val unleashService: UnleashNextMedContextService = mockk()
    private val journalføringController: JournalføringController =
        JournalføringController(
            innkommendeJournalføringService = innkommendeJournalføringService,
            tilgangService = tilgangService,
            unleashService = unleashService,
        )

    @BeforeEach
    fun oppsett() {
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
        every { unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false) } returns true
    }

    @Nested
    inner class JournalførV2 {
        @Test
        fun `skal kaste exception om valideringen slår ut`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    opprettOgKnyttTilNyBehandling = true,
                    nyBehandlingstype = null,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    journalføringController.journalførV2(
                        journalpostId = "1",
                        oppgaveId = "1",
                        journalførendeEnhet = BarnetrygdEnhet.OSLO.enhetsnummer,
                        request = restJournalføring,
                    )
                }
            assertThat(exception.frontendFeilmelding).isEqualTo("Mangler behandlingstype ved oppretting av ny behandling.")
        }

        @Test
        fun `skal journalføre og returnere iden til fagsaken`() {
            // Arrange
            val journalpostId = "456"
            val oppgaveId = "789"
            val behandlendeEnhet = BarnetrygdEnhet.OSLO.enhetsnummer
            val restJournalføring = lagMockRestJournalføring()
            val fagsakId = "321"

            every {
                innkommendeJournalføringService.journalfør(
                    request = restJournalføring,
                    journalpostId = journalpostId,
                    behandlendeEnhet = behandlendeEnhet,
                    oppgaveId = oppgaveId,
                )
            } returns fagsakId

            // Act
            val responseEntity =
                journalføringController.journalførV2(
                    journalpostId = journalpostId,
                    oppgaveId = oppgaveId,
                    journalførendeEnhet = behandlendeEnhet,
                    request = restJournalføring,
                )

            // Assert
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(responseEntity.body).isNotNull()
            assertThat(responseEntity.body!!.data).isNotNull()
            assertThat(responseEntity.body!!.data).isEqualTo(fagsakId)
        }
    }

    @Nested
    inner class HentJournalposterForBruker {
        @Test
        fun `skal returnere liste av tilgangsstyrte journalposter`() {
            // Arrange
            val personIdent = PersonIdent("123")
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
            assertThat(tilgangsstyrtJournalpost.harTilgang).isTrue
        }
    }
}
