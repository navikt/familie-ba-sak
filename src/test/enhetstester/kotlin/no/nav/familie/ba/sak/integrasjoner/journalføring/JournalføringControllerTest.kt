package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagTilgangsstyrtJournalpost
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class JournalføringControllerTest {
    private val innkommendeJournalføringService: InnkommendeJournalføringService = mockk()
    private val innkommendeJournalføringServiceV2 = mockk<InnkommendeJournalføringServiceV2>()
    private val tilgangService: TilgangService = mockk()
    private val unleashService = mockk<UnleashNextMedContextService>()

    private val journalføringController: JournalføringController =
        JournalføringController(
            innkommendeJournalføringService = innkommendeJournalføringService,
            innkommendeJournalføringServiceV2 = innkommendeJournalføringServiceV2,
            tilgangService = tilgangService,
            unleashService = unleashService,
        )

    @BeforeEach
    fun init() {
        every { unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE, false) } returns true
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
