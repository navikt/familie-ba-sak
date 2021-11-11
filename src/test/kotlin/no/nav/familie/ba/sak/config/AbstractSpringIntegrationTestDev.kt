package no.nav.familie.ba.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles(
    "dev",
    "mock-rest-template-config",
    "mock-oauth",
    "mock-pdl",
    "mock-infotrygd-barnetrygd",
    "mock-infotrygd-feed",
    "mock-økonomi",
    "mock-brev-klient"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
@Tag("integration")
abstract class AbstractSpringIntegrationTestDev {
    protected val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    init {
        wireMockServer.start()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
        wireMockServer.stop()
    }
}
