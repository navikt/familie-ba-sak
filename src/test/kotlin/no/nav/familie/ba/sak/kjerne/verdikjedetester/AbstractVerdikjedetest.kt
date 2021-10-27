package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.MockserverKlient
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.FixedHostPortGenericContainer

val MOCK_SERVER_IMAGE = "ghcr.io/navikt/familie-mock-server/familie-mock-server:latest"

class VerdikjedetesterPropertyOverrideContextInitializer :
    ApplicationContextInitializer<ConfigurableApplicationContext?> {

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "FAMILIE_BA_INFOTRYGD_API_URL: http://localhost:1337/rest/api/infotrygd/ba",
            "PDL_URL: http://localhost:1337/rest/api/pdl"
        )
        mockServer.start()
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        val mockServer: KMockServerSQLContainer by lazy {
            val mockServer = KMockServerSQLContainer(MOCK_SERVER_IMAGE)
            mockServer.withExposedPorts(1337)
            mockServer.withFixedExposedPort(1337, 1337)
            mockServer
        }
    }
}

@ActiveProfiles(
    "postgres",
    "mock-oauth",
    "mock-localdate-service",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-økonomi",
    "mock-infotrygd-feed",
    "mock-rest-template-config",
    "mock-task-repository",
    "mock-task-service"
)
@ContextConfiguration(
    initializers = [VerdikjedetesterPropertyOverrideContextInitializer::class]
)
abstract class AbstractVerdikjedetest : WebSpringAuthTestRunner() {

    fun familieBaSakKlient(): FamilieBaSakKlient = FamilieBaSakKlient(
        baSakUrl = hentUrl(""),
        restOperations = restOperations,
        headers = hentHeadersForSystembruker()
    )

    fun mockServerKlient(): MockserverKlient = MockserverKlient(
        mockServerUrl = "http://localhost:1337",
        restOperations = restOperations,
    )
}

/**
 * Hack needed because testcontainers use of generics confuses Kotlin.
 * Må bruke fixed host port for at klientene våres kan konfigureres med fast port.
 */
class KMockServerSQLContainer(imageName: String) : FixedHostPortGenericContainer<KMockServerSQLContainer>(imageName)
