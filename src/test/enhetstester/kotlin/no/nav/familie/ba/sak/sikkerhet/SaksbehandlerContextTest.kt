package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class SaksbehandlerContextTest {
    private val mockIntegrasjonClient = mockk<IntegrasjonClient>()
    private val kode6GruppeId = "kode6GruppeId"
    private val mockUnleashNextMedContextService = mockk<UnleashNextMedContextService>()

    private val saksbehandlerContext = SaksbehandlerContext(kode6GruppeId, mockIntegrasjonClient, mockUnleashNextMedContextService)

    @BeforeEach
    fun beforeEach() {
        mockkObject(SikkerhetContext)
    }

    @AfterEach
    fun afterEach() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class HentSaksbehandlerSignaturTilBrevTest {
        @Test
        fun `skal returnere tom streng dersom SB har kode6 gruppen`() {
            // Arrange
            every { SikkerhetContext.hentGrupper() } returns listOf(kode6GruppeId)
            every { mockUnleashNextMedContextService.isEnabled(FeatureToggle.BRUK_NY_SAKSBEHANDLER_NAVN_FORMAT_I_SIGNATUR) } returns true

            // Act
            val saksbehandlerSignatur = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

            // Assert
            assertThat(saksbehandlerSignatur).isEqualTo("")
        }

        @Test
        fun `skal returnere navn fra token dersom kall mot integrasjoner feiler`() {
            // Arrange
            every { SikkerhetContext.hentGrupper() } returns emptyList()
            every { mockIntegrasjonClient.hentSaksbehandler(any()) } throws Exception()
            every { mockUnleashNextMedContextService.isEnabled(FeatureToggle.BRUK_NY_SAKSBEHANDLER_NAVN_FORMAT_I_SIGNATUR) } returns true
            every { SikkerhetContext.hentSaksbehandlerNavn() } returns "Etternavn, Fornavn"

            // Act
            val saksbehandlerSignatur = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

            // Assert
            assertThat(saksbehandlerSignatur).isEqualTo("Etternavn, Fornavn")

            verify(exactly = 1) { SikkerhetContext.hentSaksbehandlerNavn() }
        }

        @Test
        fun `skal returnere navn fra token dersom feature toggle er skrudd av`() {
            // Arrange
            every { SikkerhetContext.hentGrupper() } returns emptyList()
            every { mockUnleashNextMedContextService.isEnabled(FeatureToggle.BRUK_NY_SAKSBEHANDLER_NAVN_FORMAT_I_SIGNATUR) } returns false
            every { SikkerhetContext.hentSaksbehandlerNavn() } returns "Etternavn, Fornavn"

            // Act
            val saksbehandlerSignatur = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

            // Assert
            assertThat(saksbehandlerSignatur).isEqualTo("Etternavn, Fornavn")

            verify(exactly = 1) { SikkerhetContext.hentSaksbehandlerNavn() }
        }

        @Test
        fun `skal returnere navn fra integrasjoner`() {
            // Arrange
            val saksbehandler =
                Saksbehandler(
                    azureId = UUID.randomUUID(),
                    navIdent = "navIdent",
                    fornavn = "fornavn",
                    etternavn = "etternavn",
                    enhet = "enhet",
                )

            every { SikkerhetContext.hentGrupper() } returns emptyList()
            every { mockIntegrasjonClient.hentSaksbehandler(any()) } returns saksbehandler
            every { mockUnleashNextMedContextService.isEnabled(FeatureToggle.BRUK_NY_SAKSBEHANDLER_NAVN_FORMAT_I_SIGNATUR) } returns true

            // Act
            val saksbehandlerSignatur = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

            // Assert
            assertThat(saksbehandlerSignatur).isEqualTo("fornavn etternavn")

            verify(exactly = 1) { mockIntegrasjonClient.hentSaksbehandler(any()) }
        }
    }
}
