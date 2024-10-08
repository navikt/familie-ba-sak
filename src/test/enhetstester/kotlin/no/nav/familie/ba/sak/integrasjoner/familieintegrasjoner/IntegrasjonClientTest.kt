package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.oppgave.lagEnhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.enhet.Enhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI

class IntegrasjonClientTest {
    private val mockedRestOperations: RestOperations = mockk()
    private val baseUri = URI("http://localhost:8080")
    private val integrasjonClient: IntegrasjonClient =
        IntegrasjonClient(
            baseUri,
            mockedRestOperations,
        )

    @Test
    fun `skal hente enheter som NAV-ident har tilgang til`() {
        // Arrange
        val navIdent = NavIdent("1")

        val enhet1 = lagEnhet(BarnetrygdEnhet.VADSØ.enhetsnummer)
        val enhet2 = lagEnhet(BarnetrygdEnhet.OSLO.enhetsnummer)

        every {
            mockedRestOperations.exchange<Ressurs<List<Enhet>>>(
                eq(URI("$baseUri/enhetstilganger")),
                eq(HttpMethod.POST),
                any(),
            )
        } returns
            ResponseEntity<Ressurs<List<Enhet>>>(
                Ressurs.success(
                    listOf(
                        enhet1,
                        enhet2,
                    ),
                ),
                HttpStatus.OK,
            )

        // Act
        val enheter = integrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent)

        // Assert
        assertThat(enheter).hasSize(2)
        assertThat(enheter).contains(enhet1, enhet2)
    }
}
