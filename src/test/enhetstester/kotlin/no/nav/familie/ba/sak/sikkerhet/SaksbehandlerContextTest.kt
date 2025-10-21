package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class SaksbehandlerContextTest {
    private val mockIntegrasjonKlient = mockk<IntegrasjonKlient>()
    private val kode6GruppeId = "kode6GruppeId"

    private val saksbehandlerContext = SaksbehandlerContext(kode6GruppeId, mockIntegrasjonKlient)

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

            // Act
            val saksbehandlerSignatur = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

            // Assert
            assertThat(saksbehandlerSignatur).isEqualTo("")
        }

        @Test
        fun `skal returnere navn fra token dersom kall mot integrasjoner feiler`() {
            // Arrange
            every { SikkerhetContext.hentGrupper() } returns emptyList()
            every { mockIntegrasjonKlient.hentSaksbehandler(any()) } throws Exception()
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
                    enhetsnavn = "enhetsnavn",
                )

            every { SikkerhetContext.hentGrupper() } returns emptyList()
            every { mockIntegrasjonKlient.hentSaksbehandler(any()) } returns saksbehandler

            // Act
            val saksbehandlerSignatur = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

            // Assert
            assertThat(saksbehandlerSignatur).isEqualTo("fornavn etternavn")

            verify(exactly = 1) { mockIntegrasjonKlient.hentSaksbehandler(any()) }
        }
    }
}
