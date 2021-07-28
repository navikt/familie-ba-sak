package no.nav.familie.ba.sak.integrasjoner.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI


class InfotrygdFeedClientTest : AbstractSpringIntegrationTestDev() {

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
                       .withRequestBody(equalToJson(
                               objectMapper.writeValueAsString(InfotrygdFødselhendelsesFeedDto(fnrBarn = request.fnrBarn.first())))))
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
