package no.nav.familie.ba.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ba.sak.common.DbContainerInitializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles(
    "postgres",
    "mock-økonomi",
    "mock-pdl",
    "mock-task-repository",
    "mock-infotrygd-barnetrygd",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-infotrygd-feed",
    "mock-oauth",
    "mock-rest-template-config",
)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@Tag("integration")
abstract class AbstractSpringIntegrationTest : AbstractMockkSpringRunner() {
    protected final val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    init {
        wireMockServer.start()
    }

    @AfterAll
    fun stopWiremockServer() {
        wireMockServer.stop()
    }
}
