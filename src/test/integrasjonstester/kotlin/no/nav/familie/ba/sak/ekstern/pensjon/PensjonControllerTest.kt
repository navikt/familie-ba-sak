package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.exchange
import java.util.Arrays
import java.util.UUID

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
    "mock-pdl",
    "mock-ident-klient",
    "mock-brev-klient",
)
class PensjonControllerTest : WebSpringAuthTestRunner() {
    @Test
    fun `Verifiser at pensjon-endepunkt - bestillPersonerMedBarnetrygdForGittÅrPåKafka - for henting av identer med barnetrygd - returnerer en gyldig UUID som string`() {
        val headers = HttpHeaders()
        headers.accept = Arrays.asList(MediaType.TEXT_PLAIN)
        headers.setBearerAuth(
            hentTokenForPsys(),
        )
        val entity: HttpEntity<String> = HttpEntity<String>(headers)
        val responseEntity: ResponseEntity<String> =
            restTemplate.exchange<String>(
                hentUrl("/api/ekstern/pensjon/bestill-personer-med-barnetrygd/2023"),
                HttpMethod.GET,
                entity,
            )
        assertEquals(UUID.fromString(responseEntity.body.toString()).toString(), responseEntity.body.toString())
    }

    @Test
    fun `Skal kaste feil tilgang når psys kaller tjenste som ikke er psys-relatert`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(
            hentTokenForPsys(),
        )

        val error =
            assertThrows<HttpStatusCodeException> {
                restTemplate.exchange<String>(
                    hentUrl("/api/samhandler/orgnr/987654321"),
                    HttpMethod.GET,
                    HttpEntity<String>(headers),
                )
            }

        assertThat(error.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    private fun hentTokenForPsys() = token(mapOf("azp_name" to "dev-gcp:pensjonopptjening:omsorgsopptjening-start-innlesning"))
}
