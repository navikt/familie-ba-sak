package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.oppgave.lagArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.datagenerator.oppgave.lagEnhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val MIDLERTIDIG_ENHET_4863 = "4863"
private const val VIKAFOSSEN_ENHET_2103 = "2103"

class NavIdentOgEnhetsnummerServiceTest {
    private val mockedArbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val navIdentOgEnhetsnummerService: NavIdentOgEnhetsnummerService =
        NavIdentOgEnhetsnummerService(
            arbeidsfordelingPåBehandlingRepository = mockedArbeidsfordelingPåBehandlingRepository,
            integrasjonClient = mockedIntegrasjonClient,
        )

    @Test
    fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
        // Arrange
        val behandlingId = 1L

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = MIDLERTIDIG_ENHET_4863,
            )

        // Act & assert
        val exception =
            assertThrows<Feil> {
                navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                    behandlingId = behandlingId,
                    navIdent = null,
                )
            }
        assertThat(exception.message).isEqualTo("Kan ikke sette midlertidig enhet 4863 om man mangler NAV-ident")
    }

    @Test
    fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
        // Arrange
        val behandlingId = 1L
        val navIdent = "1"
        val enhetNavIdentHarTilgangTil1 = MIDLERTIDIG_ENHET_4863
        val enhetNavIdentHarTilgangTil2 = VIKAFOSSEN_ENHET_2103

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = MIDLERTIDIG_ENHET_4863,
            )

        every {
            mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                navIdent = navIdent,
            )
        } returns
            listOf(
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil1,
                ),
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil2,
                ),
            )

        // Act & assert
        val exception =
            assertThrows<Feil> {
                navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                    behandlingId = behandlingId,
                    navIdent = navIdent,
                )
            }
        assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for nav-ident $navIdent")
    }

    @Test
    fun `skal returnere NAV-ident og første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling returnerer midlertidig enhet 4863`() {
        // Arrange
        val behandlingId = 1L
        val navIdent = "1"
        val enhetNavIdentHarTilgangTil1 = MIDLERTIDIG_ENHET_4863
        val enhetNavIdentHarTilgangTil2 = VIKAFOSSEN_ENHET_2103
        val enhetNavIdentHarTilgangTil3 = "1234"
        val enhetNavIdentHarTilgangTil4 = "4321"

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = MIDLERTIDIG_ENHET_4863,
            )

        every {
            mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                navIdent = navIdent,
            )
        } returns
            listOf(
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil1,
                ),
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil2,
                ),
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil3,
                ),
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil4,
                ),
            )

        // Act
        val navIdentOgEnhetsnummer =
            navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                behandlingId = behandlingId,
                navIdent = navIdent,
            )

        // Assert
        assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
        assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil3)
    }

    @Test
    fun `skal kaste feil hvis arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident er null`() {
        // Arrange
        val behandlingId = 1L

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = VIKAFOSSEN_ENHET_2103,
            )

        // Act & assert
        val exception =
            assertThrows<Feil> {
                navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                    behandlingId = behandlingId,
                    navIdent = null,
                )
            }
        assertThat(exception.message).isEqualTo("Kan ikke sette Vikafossen enhet 2103 om man mangler NAV-ident")
    }

    @Test
    fun `skal returnere Vikafossen 2103 uten NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
        // Arrange
        val behandlingId = 1L
        val navIdent = "1"
        val enhetNavIdentHarTilgangTil1 = "1234"
        val enhetNavIdentHarTilgangTil2 = "4321"

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = VIKAFOSSEN_ENHET_2103,
            )

        every {
            mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                navIdent = navIdent,
            )
        } returns
            listOf(
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil1,
                ),
                lagEnhet(
                    enhetsnummer = enhetNavIdentHarTilgangTil2,
                ),
            )

        // Act
        val navIdentOgEnhetsnummer =
            navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                behandlingId = behandlingId,
                navIdent = navIdent,
            )

        // Assert
        assertThat(navIdentOgEnhetsnummer.navIdent).isNull()
        assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(VIKAFOSSEN_ENHET_2103)
    }

    @Test
    fun `skal returnere Vikafossen 2103 med NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
        // Arrange
        val behandlingId = 1L
        val navIdent = "1"

        every {
            mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
            )
        } returns
            lagArbeidsfordelingPåBehandling(
                behandlingId = behandlingId,
                behandlendeEnhetId = VIKAFOSSEN_ENHET_2103,
            )

        every {
            mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                navIdent = navIdent,
            )
        } returns
            listOf(
                lagEnhet(
                    enhetsnummer = "1234",
                ),
                lagEnhet(
                    enhetsnummer = VIKAFOSSEN_ENHET_2103,
                ),
            )

        // Act
        val navIdentOgEnhetsnummer =
            navIdentOgEnhetsnummerService.hentNavIdentOgEnhetsnummer(
                behandlingId = behandlingId,
                navIdent = navIdent,
            )

        // Assert
        assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
        assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(VIKAFOSSEN_ENHET_2103)
    }
}
