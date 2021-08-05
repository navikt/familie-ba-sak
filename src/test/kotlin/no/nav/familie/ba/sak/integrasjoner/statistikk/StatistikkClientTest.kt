package no.nav.familie.ba.sak.integrasjoner.statistikk

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.web.client.RestOperations
import java.net.URI

class StatistikkClientTest: AbstractSpringIntegrationTestDev() {

    lateinit var client: StatistikkClient

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @Autowired
    lateinit var environment: Environment

    @BeforeEach
    fun setUp() {
        WireMock.resetAllRequests()
        client = StatistikkClient(
            URI.create("http://localhost:${environment["wiremock.server.port"]}/api"),
            restOperations
        )
    }

    @Test
    @Tag("integration")
    fun `skal legge til fødselsnummer i infotrygd feed`() {
        WireMock.stubFor(
            WireMock.get("/api/statistikk/sak/1").willReturn(WireMock.okJson("{\"foo\": \"bar\"}")
            )
        )

        client.hentSakStatistikk(1)

    }
}