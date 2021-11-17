package no.nav.familie.ba.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles(
    "dev",
    "mock-rest-template-config",
    "mock-oauth",
    "mock-pdl",
    "mock-task-repository",
    "mock-tilbakekreving-klient",
    "mock-infotrygd-barnetrygd",
    "mock-infotrygd-feed",
    "mock-økonomi",
    "mock-brev-klient"
)
@Tag("integration")
abstract class AbstractSpringIntegrationTestDev(
    personopplysningerService: PersonopplysningerService? = null
) : AbstractMockkSpringRunner(personopplysningerService) {
    protected final val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    init {
        wireMockServer.start()
    }

    @AfterAll
    fun stopWiremockServer() {
        wireMockServer.stop()
    }
}
