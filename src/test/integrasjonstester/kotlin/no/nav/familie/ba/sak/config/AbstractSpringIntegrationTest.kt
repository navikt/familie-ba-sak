package no.nav.familie.ba.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
    "mock-pdl",
    "mock-pdl-klient",
    "mock-ident-klient",
    "mock-ef-klient",
    "mock-infotrygd-barnetrygd",
    "fake-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-infotrygd-feed",
    "mock-rest-template-config",
    "mock-localdate-service",
    "mock-sanity-klient",
    "mock-unleash",
    "mock-system-only-integrasjon-klient",
    "fake-integrasjon-klient",
    "fake-ecb-valutakurs-rest-klient",
    "fake-økonomi-klient",
    "fake-env-service",
    "fake-task-repository",
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@MockKExtension.KeepMocks
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
