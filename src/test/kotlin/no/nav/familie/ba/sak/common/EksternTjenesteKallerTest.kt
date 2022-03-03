package no.nav.familie.ba.sak.common

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgave
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.ikkeTilgang
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

class EksternTjenesteKallerTest : AbstractSpringIntegrationTestDev() {

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    lateinit var integrasjonClient: IntegrasjonClient

    @BeforeEach
    fun setUp() {
        integrasjonClient = IntegrasjonClient(
            URI.create(wireMockServer.baseUrl() + "/api"),
            restOperations
        )
    }

    @AfterEach
    fun clearTest() {
        MDC.clear()
        wireMockServer.resetAll()
    }

    @Test
    @Tag("integration")
    fun `Tjeneste svarer med 200 OK og feilet ressurs`() {
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(failure<OppgaveResponse>("Opprett oppgave feilet")))
            )
        )

        val feil = assertThrows<IntegrasjonException> { integrasjonClient.opprettOppgave(lagTestOppgave()) }
        assertTrue(feil.message?.contains("Opprett oppgave feilet") == true)
    }

    @Test
    @Tag("integration")
    fun `Tjeneste svarer med 500 og skal feile`() {
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
            )
        )

        val feil = assertThrows<IntegrasjonException> { integrasjonClient.opprettOppgave(lagTestOppgave()) }
        assertTrue(feil.message?.contains("no body") == true)
    }

    @Test
    @Tag("integration")
    fun `Tjeneste svarer med forbidden og skal kaste feil videre`() {
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(ikkeTilgang<OppgaveResponse>("Ikke tilgang til å opprett oppgave")))
            )
        )

        val feil =
            assertThrows<HttpClientErrorException.Forbidden> { integrasjonClient.opprettOppgave(lagTestOppgave()) }
        assertTrue(feil.message?.contains("Ikke tilgang til å opprett oppgave") == true)
    }

    @Test
    @Tag("integration")
    fun `Tjeneste svarer med 404 og not found skal ligge på integrasjon exception`() {
        wireMockServer.stubFor(
            post("/api/oppgave/opprett").willReturn(
                aResponse()
                    .withStatus(404)
            )
        )

        val feil =
            assertThrows<IntegrasjonException> { integrasjonClient.opprettOppgave(lagTestOppgave()) }
        assertTrue(feil.cause is HttpClientErrorException.NotFound)
        assertTrue(feil.message?.contains("404") == true)
    }
}
