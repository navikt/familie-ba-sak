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

class OppgaveArbeidsfordelingServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val oppgaveArbeidsfordelingService: OppgaveArbeidsfordelingService = OppgaveArbeidsfordelingService(integrasjonClient = mockedIntegrasjonClient)

    @Nested
    inner class FinnOppgaveArbeidsfordelingTest {
        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette ${BarnetrygdEnhet.MIDLERTIDIG_ENHET} om man mangler NAV-ident")
        }

        @Test
        fun `skal kaste feil om arbeidsfordeling returnerer midlertidig enhet 4863 og NAV-ident ikke har tilgang til noen andre enheter enn 4863 og 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.MIDLERTIDIG_ENHET
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VIKAFOSSEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
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

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.DRAMMEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnavn)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    behandlendeEnhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = null,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke sette ${BarnetrygdEnhet.VIKAFOSSEN} om man mangler NAV-ident")
        }

        @Test
        fun `skal returnere Vikafossen 2103 uten NAV-ident om arbeidsfordeling returnerer Vikafossen 2103 og NAV-ident ikke har tilgang til Vikafossen 2103`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.STEINKJER
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.VADSØ

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer,
                    behandlendeEnhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isNull()
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
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
                    behandlendeEnhetNavn = BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
        }

        @Test
        fun `skal returnere behandlendeEnhetId uten NAV-ident om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = "1234",
                    behandlendeEnhetNavn = "Fiktiv enhet",
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = null,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isNull()
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(arbeidsfordelingPåBehandling.behandlendeEnhetId)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(arbeidsfordelingPåBehandling.behandlendeEnhetNavn)
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
                    behandlendeEnhetNavn = "Fiktiv enhet",
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
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

            val enhetNavIdentHarTilgangTil1 = BarnetrygdEnhet.OSLO
            val enhetNavIdentHarTilgangTil2 = BarnetrygdEnhet.DRAMMEN

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                    behandlendeEnhetNavn = BarnetrygdEnhet.STEINKJER.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
                        enhetsnummer = enhetNavIdentHarTilgangTil1.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil1.enhetsnavn,
                    ),
                    lagEnhet(
                        enhetsnummer = enhetNavIdentHarTilgangTil2.enhetsnummer,
                        enhetsnavn = enhetNavIdentHarTilgangTil2.enhetsnavn,
                    ),
                )

            // Act
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(enhetNavIdentHarTilgangTil1.enhetsnavn)
        }

        @Test
        fun `skal returnere NAV-ident og arbeidsfordeling enhetsnummer om arbeidsfordeling ikke returnere 2103 eller 4863 og NAV-ident har tilgang arbeidsfordeling enheten`() {
            // Arrange
            val behandlingId = 1L
            val navIdent = NavIdent("1")

            val arbeidsfordelingEnhet = BarnetrygdEnhet.OSLO

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = arbeidsfordelingEnhet.enhetsnummer,
                    behandlendeEnhetNavn = arbeidsfordelingEnhet.enhetsnavn,
                )

            every {
                mockedIntegrasjonClient.hentEnheterSomNavIdentHarTilgangTil(
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
            val oppgaveArbeidsfordeling =
                oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )

            // Assert
            assertThat(oppgaveArbeidsfordeling.navIdent).isEqualTo(navIdent)
            assertThat(oppgaveArbeidsfordeling.enhetsnummer).isEqualTo(arbeidsfordelingEnhet.enhetsnummer)
            assertThat(oppgaveArbeidsfordeling.enhetsnavn).isEqualTo(arbeidsfordelingEnhet.enhetsnavn)
        }
    }

    @Nested
    inner class OppgaveArbeidsfordelingTest {
        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mindre enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    OppgaveArbeidsfordeling(
                        null,
                        "123",
                        "Enhet 123",
                    )
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }

        @Test
        fun `skal kaste exception om enhetsnummer blir satt til mer enn 4 siffer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalArgumentException> {
                    OppgaveArbeidsfordeling(
                        null,
                        "12345",
                        "Enhet 12345",
                    )
                }
            assertThat(exception.message).isEqualTo("Enhetsnummer må være 4 siffer")
        }
    }
}
