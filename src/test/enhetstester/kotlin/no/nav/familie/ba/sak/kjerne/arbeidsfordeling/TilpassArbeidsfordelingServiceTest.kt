package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.oppgave.lagEnhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.enhet.Enhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilpassArbeidsfordelingServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService =
        TilpassArbeidsfordelingService(integrasjonClient = mockedIntegrasjonClient)

    @Nested
    inner class TilpassArbeidsfordelingTilSaksbehandler {
        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
            // Arrange

            val arbeidsfordelingPåBehandling =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VIKAFOSSEN

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for nav-ident $navIdent")
        }

        @Test
        fun `skal returnere NAV-ident og første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling returnerer midlertidig enhet 4863`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.DRAMMEN

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnavn)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident er null`() {
            // Arrange

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${BarnetrygdEnhet.VIKAFOSSEN} om man mangler NAV-ident")
        }

        @Test
        fun `skal returnere Vikafossen 2103 uten NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.STEINKJER
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VADSØ

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere Vikafossen 2103 med NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = "1234",
                        enhetsnavn = "Fiktiv enhet",
                    ),
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere behandlendeEnhetId uten NAV-ident om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident er null`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = "1234",
                    enhetNavn = "Fiktiv enhet",
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = null,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(arbeidsfordelingsenhet.enhetNavn)
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident ikke har tilgang til noen enheter`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = "1234",
                    enhetNavn = "Fiktiv enhet",
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Fant ingen passende enhetsnummer for NAV-ident $navIdent")
        }

        @Test
        fun `skal returnere NAV-ident og første enhet NAV-ident har tilgang om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident ikke har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.DRAMMEN

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.STEINKJER.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnavn)
        }

        @Test
        fun `skal returnere NAV-ident og arbeidsfordeling enhetsnummer om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = BarnetrygdEnhet.OSLO

            val arbeidsfordelingPåBehandling =
                Arbeidsfordelingsenhet(
                    enhetId = arbeidsfordelingEnhet.enhetsnummer,
                    enhetNavn = arbeidsfordelingEnhet.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(
                    navIdent = navIdent,
                )
            } returns
                listOf(
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = arbeidsfordelingEnhet.enhetsnummer,
                        enhetsnavn = arbeidsfordelingEnhet.enhetsnavn,
                    ),
                )

            // Act
            val tilpassetArbeidsfordelingsenhet =
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(arbeidsfordelingEnhet.enhetsnummer)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(arbeidsfordelingEnhet.enhetsnavn)
        }
    }

    @Nested
    inner class BestemTilordnetRessursPåOppgave {
        @Test
        fun `skal returnere navIdent dersom navIdent har tilgang til arbeidsfordelingsenhet`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )
            val navIdent = NavIdent("1")

            every { mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent) } returns
                listOf(
                    Enhet(
                        enhetsnummer = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                    ),
                )

            // Act
            val tilordnetRessurs =
                tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isEqualTo(navIdent)
        }

        @Test
        fun `skal returnere null dersom navIdent ikke har tilgang til arbeidsfordelingsenhet`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )
            val navIdent = NavIdent("1")

            every { mockedIntegrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent = navIdent) } returns
                listOf(
                    Enhet(
                        enhetsnummer = BarnetrygdEnhet.OSLO.enhetsnummer,
                        enhetsnavn = BarnetrygdEnhet.OSLO.enhetsnavn,
                    ),
                )

            // Act
            val tilordnetRessurs =
                tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isNull()
        }

        @Test
        fun `skal returnere null dersom navIdent er null`() {
            // Arrange
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )
            val navIdent = null

            // Act
            val tilordnetRessurs =
                tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            // Assert
            assertThat(tilordnetRessurs).isNull()
        }
    }
}
