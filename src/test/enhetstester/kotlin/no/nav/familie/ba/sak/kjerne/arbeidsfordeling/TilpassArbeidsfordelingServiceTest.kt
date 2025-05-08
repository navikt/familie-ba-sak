package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.kontrakter.felles.NavIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilpassArbeidsfordelingServiceTest {
    private val mocketEnhetConfig: EnhetConfig = mockk()

    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService =
        TilpassArbeidsfordelingService(
            enhetConfig = mocketEnhetConfig,
        )

    @Nested
    inner class TilpassArbeidsfordelingTilSaksbehandler {
        @Test
        fun `skal kaste feil om arbeidsfordeling er midlertidig enhet 4863 og NAV-ident er null`() {
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
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident.")
        }

        @Test
        fun `skal kaste midlertidigEnhetIAutomatiskBehandlingFeil om arbeidsfordeling er midlertidig enhet 4863 og NAV-ident er systembruker`() {
            // Arrange
            val arbeidsfordelingPåBehandling =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<MidlertidigEnhetIAutomatiskBehandlingFeil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingPåBehandling,
                        navIdent = NavIdent(SYSTEM_FORKORTELSE),
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} i automatiske behandlinger.")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling er midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns emptyList()

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Nav-Ident har ikke tilgang til noen enheter.")
        }

        @Test
        fun `skal returnere Vikafossen om NAV-identen kun har tilgang til Vikafossen når arbeidsfordeling er midlertidig enhet 4863`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(BarnetrygdEnhet.VIKAFOSSEN)

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
        fun `skal returnere første enhetsnummer som NAV-identen har tilgang til når arbeidsfordeling er midlertidig enhet 4863`() {
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
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(
                    BarnetrygdEnhet.VIKAFOSSEN,
                    BarnetrygdEnhet.OSLO,
                    BarnetrygdEnhet.DRAMMEN,
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
            assertThat(exception.message).isEqualTo("Kan ikke håndtere ${BarnetrygdEnhet.VIKAFOSSEN} om man mangler NAV-ident.")
        }

        @Test
        fun `skal returnere Vikafossen 2103 dersom arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.STEINKJER
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VADSO

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(
                    BarnetrygdEnhet.STEINKJER,
                    BarnetrygdEnhet.VADSO,
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
        fun `skal returnere Vikafossen 2103 dersom arbeidsfordeling er Vikafossen 2103 og NAV-ident har tilgang til Vikafossen 2103`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(BarnetrygdEnhet.VIKAFOSSEN)

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
        fun `skal returnere behandlendeEnhetId dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident er null`() {
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
        fun `skal kaste feil om arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident ikke har tilgang til noen enheter`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = "1234",
                    enhetNavn = "Fiktiv enhet",
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns emptyList()

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = navIdent,
                    )
                }
            assertThat(exception.message).isEqualTo("Nav-Ident har ikke tilgang til noen enheter.")
        }

        @Test
        fun `skal returnere Vikafossen hvis Nav-ident kun har tilgang til Vikafossen`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = "1234",
                    enhetNavn = "Fiktiv enhet",
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(
                    BarnetrygdEnhet.VIKAFOSSEN,
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
        fun `skal returnere første enhet NAV-ident har tilgang til dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident ikke har tilgang arbeidsfordelingsenheten`() {
            // Arrange
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO

            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.STEINKJER.enhetsnavn,
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(
                    BarnetrygdEnhet.VIKAFOSSEN,
                    BarnetrygdEnhet.OSLO,
                    BarnetrygdEnhet.DRAMMEN,
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
        fun `skal returnere opprinnelig arbeidsfordeling dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident har tilgang til arbeidsfordelingsenheten`() {
            // Arrange
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = BarnetrygdEnhet.OSLO

            val arbeidsfordelingPåBehandling =
                Arbeidsfordelingsenhet(
                    enhetId = arbeidsfordelingEnhet.enhetsnummer,
                    enhetNavn = arbeidsfordelingEnhet.enhetsnavn,
                )

            every {
                mocketEnhetConfig.hentAlleEnheterBrukerHarTilgangTil()
            } returns
                listOf(
                    BarnetrygdEnhet.MIDLERTIDIG_ENHET,
                    BarnetrygdEnhet.VIKAFOSSEN,
                    BarnetrygdEnhet.OSLO,
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

        @Test
        fun `skal returnere behandlendeEnhetId og behandlendeEnhetNavn dersom arbeidsfordeling ikke er 2103 eller 4863 og NAV-ident er SYSTEM_FORKORTELSE`() {
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
                    navIdent = NavIdent(SYSTEM_FORKORTELSE),
                )

            // Assert
            assertThat(tilpassetArbeidsfordelingsenhet.enhetId).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(tilpassetArbeidsfordelingsenhet.enhetNavn).isEqualTo(arbeidsfordelingsenhet.enhetNavn)
        }
    }
}
