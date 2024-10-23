package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.integrasjoner.lagTilgangsstyrtJournalpost
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.unleash.UnleashService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class JournalføringControllerTest {
    private val innkommendeJournalføringService: InnkommendeJournalføringService = mockk()
    private val tilgangService: TilgangService = mockk()
    private val unleashService: UnleashService = mockk()
    private val journalføringController: JournalføringController =
        JournalføringController(
            innkommendeJournalføringService = innkommendeJournalføringService,
            tilgangService = tilgangService,
            unleashService = unleashService,
        )

    @Nested
    inner class HentJournalposterForBruker {
        @Test
        fun `skal returnere liste av journalposter når toggle er av`() {
            // Arrange
            val personIdent = PersonIdent("123")
            val journalpostId = "1"

            every { innkommendeJournalføringService.hentJournalposterForBruker(personIdent.ident) } returns listOf(lagTilgangsstyrtJournalpost(personIdent.ident, journalpostId = journalpostId))
            every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NYTT_RETUR_OBJEKT_FOR_JOURNALPOSTER, false) } returns false

            // Act
            val responseEntity = journalføringController.hentJournalposterForBruker(personIdentBody = personIdent)

            // Assert
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(responseEntity.body).isNotNull
            assertThat(responseEntity.body!!.data).isNotNull
            val journalposter = responseEntity.body!!.data!!
            assertThat(journalposter).hasSize(1)
            assertThat(journalposter.single()).isInstanceOf(Journalpost::class.java)
            val journalpost = journalposter.single() as Journalpost
            assertThat(journalpost.journalpostId).isEqualTo("1")
        }

        @Test
        fun `skal returnere liste av tilgangsstyrte journalposter når toggle er på`() {
            // Arrange
            val personIdent = PersonIdent("123")
            val journalpostId = "1"

            every { innkommendeJournalføringService.hentJournalposterForBruker(personIdent.ident) } returns listOf(lagTilgangsstyrtJournalpost(personIdent.ident, journalpostId = journalpostId, harTilgang = true))
            every { unleashService.isEnabled(FeatureToggleConfig.BRUK_NYTT_RETUR_OBJEKT_FOR_JOURNALPOSTER, false) } returns true

            // Act
            val responseEntity = journalføringController.hentJournalposterForBruker(personIdentBody = personIdent)

            // Assert
            assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(responseEntity.body).isNotNull
            assertThat(responseEntity.body!!.data).isNotNull
            val journalposter = responseEntity.body!!.data!!
            assertThat(journalposter).hasSize(1)
            assertThat(journalposter.single()).isInstanceOf(TilgangsstyrtJournalpost::class.java)
            val tilgangsstyrtJournalpost = journalposter.single() as TilgangsstyrtJournalpost
            assertThat(tilgangsstyrtJournalpost.journalpost.journalpostId).isEqualTo("1")
            assertThat(tilgangsstyrtJournalpost.harTilgang).isTrue
        }
    }
}
