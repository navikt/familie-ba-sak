package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.oppgave.lagArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.datagenerator.oppgave.lagEnhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.NavIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NavIdentOgEnhetServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val navIdentOgEnhetService: NavIdentOgEnhetService = NavIdentOgEnhetService(integrasjonClient = mockedIntegrasjonClient)

    @Nested
    inner class HentNavIdentOgEnhetTest {
        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette midlertidig enhet 4863 om man mangler NAV-ident")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
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
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }

        @Test
        fun `skal returnere NAV-ident og første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling returnerer midlertidig enhet 4863`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetsnummerForEnhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO.enhetsnummer
            val enhetsnummerForEnhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.DRAMMEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(enhetsnummerForEnhetNavIdentHarTilgangTil1)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette Vikafossen enhet 2103 om man mangler NAV-ident")
        }

        @Test
        fun `skal returnere Vikafossen 2103 uten NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.STEINKJER.enhetsnummer
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VADSØ.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
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
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isNull()
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
        }

        @Test
        fun `skal returnere Vikafossen 2103 med NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
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
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    ),
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
        }

        @Test
        fun `skal returnere behandlendeEnhetId uten NAV-ident om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = "1234",
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = null,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isNull()
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo("1234")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident ikke har tilgang til noen enheter`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = "1234",
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    navIdentOgEnhetService.hentNavIdentOgEnhet(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }

        @Test
        fun `skal returnere NAV-ident og første enhet NAV-ident har tilgang om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident ikke har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetsnummerForEnhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO.enhetsnummer
            val enhetsnummerForEnhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.DRAMMEN.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil1,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetsnummerForEnhetNavIdentHarTilgangTil2,
                    ),
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(enhetsnummerForEnhetNavIdentHarTilgangTil1)
        }

        @Test
        fun `skal returnere NAV-ident og arbeidsfordeling enhetsnummer om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = BarnetrygdEnhet.OSLO.enhetsnummer

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = arbeidsfordelingEnhet,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    ),
                    lagEnhet(
                        enhetsnummer = arbeidsfordelingEnhet,
                    ),
                )

            // Act
            val navIdentOgEnhetsnummer =
                navIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(navIdentOgEnhetsnummer.navIdent).isEqualTo(navIdent)
            assertThat(navIdentOgEnhetsnummer.enhetsnummer).isEqualTo(arbeidsfordelingEnhet)
        }
    }

    @Nested
    inner class NavIdentOgEnhetTest {
        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mindre enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    NavIdentOgEnhet(null, "123", "Enhet 123")
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }

        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mer enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    NavIdentOgEnhet(null, "12345", "Enhet 12345")
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }
    }
}
