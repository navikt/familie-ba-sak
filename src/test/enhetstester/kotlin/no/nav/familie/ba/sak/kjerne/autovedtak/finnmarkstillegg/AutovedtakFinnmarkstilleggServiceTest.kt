package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresseOgDeltBostedPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AutovedtakFinnmarkstilleggServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val pdlRestClient = mockk<SystemOnlyPdlRestClient>()
    private val autovedtakFinnmarkstilleggService =
        AutovedtakFinnmarkstilleggService(
            autovedtakService = mockk(),
            fagsakService = mockk(),
            taskService = mockk(),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            pdlRestClient = pdlRestClient,
            behandlingService = mockk(),
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak = fagsak)
    private val søkerIdent = "12345678910"
    private val barnIdent = "12345678911"
    private val persongrunnlag =
        lagTestPersonopplysningGrunnlag(
            behandling.id,
            søkerIdent,
            listOf(barnIdent),
        )

    private val adresse = Vegadresse(null, null, null, null, null, null, null, null)
    private val bostedsadresseUtenforFinnmark =
        Bostedsadresse(gyldigFraOgMed = LocalDate.now(), vegadresse = adresse.copy(kommunenummer = "0301"))

    private val bostedsadresseIFinnmark =
        Bostedsadresse(gyldigFraOgMed = LocalDate.now(), vegadresse = adresse.copy(kommunenummer = "5601"))

    @BeforeEach
    fun setUp() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns behandling
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
    }

    @Test
    fun `skal returnere false når det ikke finnes noen siste iverksatte behandling`() {
        // Arrange
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(0L) } returns null

        // Act
        val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = 0L))

        // Assert
        assertThat(skalAutovedtakBehandles).isFalse()
    }

    @Test
    fun `skal returnere false når ingen av personene bor i Finnmark eller Nord-Troms`() {
        // Arrange
        every { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
            mapOf(
                søkerIdent to PdlBostedsadresseOgDeltBostedPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                barnIdent to PdlBostedsadresseOgDeltBostedPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
            )

        // Act
        val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

        // Assert
        assertThat(skalAutovedtakBehandles).isFalse()
    }

    @Test
    fun `skal returnere true når minst en person bor i Finnmark eller Nord-Troms`() {
        // Arrange
        every { pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
            mapOf(
                søkerIdent to PdlBostedsadresseOgDeltBostedPerson(bostedsadresse = listOf(bostedsadresseIFinnmark), deltBosted = emptyList()),
                barnIdent to PdlBostedsadresseOgDeltBostedPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
            )

        // Act
        val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

        // Assert
        assertThat(skalAutovedtakBehandles).isTrue()
    }
}
