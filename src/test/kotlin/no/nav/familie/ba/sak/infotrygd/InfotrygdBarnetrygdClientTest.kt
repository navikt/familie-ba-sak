package no.nav.familie.ba.sak.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.commons.foedselsnummer.FoedselsNr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestOperations
import java.net.URI

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles("dev", "integrasjonstest", "mock-oauth")
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@Disabled
class InfotrygdBarnetrygdClientTest {

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @Autowired
    lateinit var environment: Environment

    lateinit var client: InfotrygdBarnetrygdClient

    @BeforeEach
    fun setUp() {
        resetAllRequests()
        client = InfotrygdBarnetrygdClient(
                URI.create("http://localhost:${environment.get("wiremock.server.port")}/api"),
                restOperations,
                environment
        )
    }

    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr`() {
        stubFor(post("/api/infotrygd/barnetrygd/personsok").willReturn(okJson(objectMapper.writeValueAsString(
                InfotrygdSøkResponse(true)))))

        val søkersIdenter = ClientMocks.søkerFnr.toList()
        val barnasIdenter = ClientMocks.barnFnr.toList()
        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter.map {
            FoedselsNr(it)
        }, barnasIdenter.map { FoedselsNr(it) })

        val finnesIkkeHosInfotrygd = client.finnesIkkeHosInfotrygd(søkersIdenter, barnasIdenter)

        verify(anyRequestedFor(anyUrl()).withRequestBody(equalToJson(objectMapper.writeValueAsString (infotrygdSøkRequest))))
        Assertions.assertEquals(true, finnesIkkeHosInfotrygd)
    }

    /*
    Denne testen er en kopi av tilsvarende test i InfotrygdFeedClientTest.
    Testen kjører med suksess lokalt, men feiler på byggeserveren med en RuntimeException i AbstractRestClient.executeMedMetrics.

    @Test
    fun `Invokering av Infotrygd-Barnetrygd genererer http feil`() {
        stubFor(post("/api/infotrygd/barnetrygd/personsok").willReturn(aResponse().withStatus(401)))

        assertThrows<HttpClientErrorException> {
            client.finnesIkkeHosInfotrygd(ClientMocks.søkerFnr.toList(), ClientMocks.barnFnr.toList())
        }
    }
    */
}