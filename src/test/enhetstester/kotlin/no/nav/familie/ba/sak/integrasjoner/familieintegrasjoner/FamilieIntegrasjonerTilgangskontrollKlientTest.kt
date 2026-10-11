package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.net.URI

class FamilieIntegrasjonerTilgangskontrollKlientTest {
    private val restClient = mockk<RestClient>()
    private val familieIntegrasjonerTilgangskontrollKlient = FamilieIntegrasjonerTilgangskontrollKlient(URI("http://localhost"), restClient)

    @Test
    fun `skal gi tilgang uten å kalle familie-integrasjoner i systemkontekst`() {
        // Arrange
        BrukerContextUtil.clearBrukerContext()
        val personIdenter = listOf("12345678910", "10987654321")

        // Act
        val tilganger = familieIntegrasjonerTilgangskontrollKlient.sjekkTilgangTilPersoner(personIdenter)

        // Assert
        assertThat(tilganger).containsExactly(
            Tilgang(personIdent = "12345678910", harTilgang = true),
            Tilgang(personIdent = "10987654321", harTilgang = true),
        )
        verify { restClient wasNot Called }
    }
}
