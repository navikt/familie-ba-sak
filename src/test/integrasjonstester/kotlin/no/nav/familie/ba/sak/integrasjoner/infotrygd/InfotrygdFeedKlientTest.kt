package no.nav.familie.ba.sak.integrasjoner.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.StartBehandlingDto
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.NavHttpHeaders
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.net.URI

class InfotrygdFeedKlientTest : AbstractSpringIntegrationTest() {
    lateinit var klient: InfotrygdFeedKlient

    @Autowired
    @Qualifier("utenAuthRestClient")
    lateinit var restClient: RestClient

    @BeforeEach
    fun setUp() {
        klient =
            InfotrygdFeedKlient(
                URI.create(wireMockServer.baseUrl() + "/api"),
                restClient,
                1L,
            )
    }

    @AfterEach
    fun clearTest() {
        wireMockServer.resetAll()
    }

    @Test
    @Tag("integration")
    fun `skal sende startbehandlingsmelding til Infotrygd feed`() {
        // Arrange
        wireMockServer.stubFor(
            post("/api/barnetrygd/v1/feed/startbehandlingsmelding").willReturn(
                okJson(jsonMapper.writeValueAsString(success("Create"))),
            ),
        )
        val request = StartBehandlingDto("fnr")

        // Act
        klient.sendStartBehandlingTilInfotrygd(request)

        // Assert
        wireMockServer.verify(
            anyRequestedFor(anyUrl())
                .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("srvfamilie-ba-sak"))
                .withRequestBody(
                    equalToJson(
                        jsonMapper.writeValueAsString(request),
                    ),
                ),
        )
    }

    @Test
    @Tag("integration")
    fun `Invokering av Infotrygd feed genererer http feil`() {
        // Arrange
        wireMockServer.stubFor(post("/api/barnetrygd/v1/feed/startbehandlingsmelding").willReturn(aResponse().withStatus(401)))

        // Act & Assert
        assertThrows<HttpClientErrorException> {
            klient.sendStartBehandlingTilInfotrygd(StartBehandlingDto("fnr"))
        }
    }

    @Test
    @Tag("integration")
    fun `Invokering av Infotrygd returnerer ulovlig response format`() {
        // Arrange
        wireMockServer.stubFor(post("/api/barnetrygd/v1/feed/startbehandlingsmelding").willReturn(aResponse().withBody("Create")))

        // Act & Assert
        assertThrows<RuntimeException> {
            klient.sendStartBehandlingTilInfotrygd(StartBehandlingDto("fnr"))
        }
    }
}
