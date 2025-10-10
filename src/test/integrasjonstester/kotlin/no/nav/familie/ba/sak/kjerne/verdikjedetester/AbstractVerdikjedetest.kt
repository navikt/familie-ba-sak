package no.nav.familie.ba.sak.kjerne.verdikjedetester

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.web.client.RestOperations

class VerdikjedetesterPropertyOverrideContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "PDL_URL: http://localhost:1337/rest/api/pdl",
            "FAMILIE_INTEGRASJONER_API_URL: http://localhost:1337/rest/api/integrasjoner",
        )
    }
}

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
    "mock-localdate-service",
    "fake-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-infotrygd-feed",
    "mock-ef-client",
    "mock-rest-template-config",
    "mock-task-repository",
    "mock-task-service",
    "mock-sanity-client",
    "mock-unleash",
    "mock-infotrygd-barnetrygd",
    "fake-integrasjon-client",
    "fake-valutakurs-rest-client",
    "fake-økonomi-klient",
    "fake-task-repository",
)
@ContextConfiguration(initializers = [VerdikjedetesterPropertyOverrideContextInitializer::class])
@Tag("verdikjedetest")
@AutoConfigureWireMock(port = 1337)
abstract class AbstractVerdikjedetest : WebSpringAuthTestRunner() {
    @AfterAll
    fun tearDownSuper() {
        WireMock.reset()
    }

    @Autowired
    lateinit var restOperations: RestOperations

    fun familieBaSakKlient(): FamilieBaSakKlient =
        FamilieBaSakKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeadersForSystembruker(),
        )
}
