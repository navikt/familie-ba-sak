package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.MockserverKlient
import org.junit.jupiter.api.AfterAll
import org.slf4j.LoggerFactory
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.GenericContainer

val MOCK_SERVER_IMAGE = "ghcr.io/navikt/familie-mock-server/familie-mock-server:latest"


@ActiveProfiles(
        "postgres",
        "mock-oauth",
        "mock-localdate-service",
        "mock-tilbakekreving-klient",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
        "mock-rest-template-config",
        "mock-task-repository",
        "mock-task-service"
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

    init {
        mockServer.start()
        logger.info("Startet mock server på port ${mockServer.exposedPorts} på host ${mockServer.firstMappedPort}")
    }

    companion object {
        val logger = LoggerFactory.getLogger(AbstractVerdikjedetest::class.java)
    }
}

/**
 * Hack needed because testcontainers use of generics confuses Kotlin.
 * Må bruke fixed host port for at klientene våres kan konfigureres med fast port.
 */
class KMockServerSQLContainer(imageName: String) : FixedHostPortGenericContainer<KMockServerSQLContainer>(imageName)