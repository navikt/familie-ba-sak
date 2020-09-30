package no.nav.familie.ba.sak.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI


@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles("dev", "integrasjonstest", "mock-oauth", "mock-pdl")
@AutoConfigureWireMock(port = 0)
@TestInstance(Lifecycle.PER_CLASS)
class InfotrygdFeedClientTest {

    lateinit var client: InfotrygdFeedClient

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @Autowired
    lateinit var environment: Environment

    @BeforeEach
    fun setUp() {
        resetAllRequests()
        client = InfotrygdFeedClient(
                URI.create("http://localhost:${environment["wiremock.server.port"]}/api"),
                restOperations, environment
        )
    }

    @Test
    @Tag("integration")
    fun `skal legge til fødselsnummer i infotrygd feed`() {
        stubFor(post("/api/barnetrygd/v1/feed/foedselsmelding").willReturn(
                okJson(objectMapper.writeValueAsString(success("Create")))))
        val request = InfotrygdFødselhendelsesFeedTaskDto(listOf("fnr"))

        request.fnrBarn.forEach {
            client.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto(fnrBarn = it))
        }

        verify(anyRequestedFor(anyUrl())
                       .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-sak"))
                       .withRequestBody(equalToJson(objectMapper.writeValueAsString(request))))
    }

    @Test
    @Tag("integration")
    fun `Invokering av Infotrygd feed genererer http feil`() {
        stubFor(post("/api/barnetrygd/v1/feed/foedselsmelding").willReturn(aResponse().withStatus(401)))

        assertThrows<HttpClientErrorException> {
            client.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto("fnr"))
        }
    }

    @Test
    @Tag("integration")
    fun `Invokering av Infotrygd returnerer ulovlig response format`() {
        stubFor(post("/api/barnetrygd/v1/feed/foedselsmelding").willReturn(aResponse().withBody("Create")))

        assertThrows<RuntimeException> {
            client.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto("fnr"))
        }
    }
}
