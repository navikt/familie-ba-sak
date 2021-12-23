package no.nav.familie.ba.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles(
    "dev",
    "mock-rest-template-config",
    "mock-oauth",
    "mock-pdl",
    "mock-ident-client",
    "mock-task-repository",
    "mock-tilbakekreving-klient",
    "mock-infotrygd-barnetrygd",
    "mock-infotrygd-feed",
    "mock-økonomi",
    "mock-brev-klient"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
abstract class AbstractSpringIntegrationTestDev : AbstractMockkSpringRunner() {
    protected final val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    init {
        wireMockServer.start()
    }

    @AfterAll
    fun stopWiremockServer() {
        wireMockServer.stop()
    }
}
