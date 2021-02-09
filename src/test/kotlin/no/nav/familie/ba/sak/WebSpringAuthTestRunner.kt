package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate

@SpringBootTest(
        classes = [ApplicationConfig::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = [
            "no.nav.security.jwt.issuer.azuread.discoveryUrl=http://localhost:1234/azuread/.well-known/openid-configuration",
            "no.nav.security.jwt.issuer.azuread.accepted_audience=some-audience",
            "VEILEDER_ROLLE: VEILDER",
            "SAKSBEHANDLER_ROLLE: SAKSBEHANDLER",
            "BESLUTTER_ROLLE: BESLUTTER",
            "ENVIRONMENT_NAME: integrationtest"
        ],
)
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server(port = 1234)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) abstract class WebSpringAuthTestRunner {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @LocalServerPort
    private val port = 0

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    fun hentUrl(path: String) = "http://localhost:$port$path"

    fun token(claims: Map<String, Any>,
              subject: String = defaultSubject,
              audience: String = defaultAudience,
              issuerId: String = defaultIssuerId): String? {
        return mockOAuth2Server.issueToken(
                issuerId,
                "theclientid",
                DefaultOAuth2TokenCallback(
                        issuerId,
                        subject,
                        listOf(audience),
                        claims,
                        3600
                )
        ).serialize()
    }

    companion object {

        const val defaultIssuerId = "azuread"
        const val defaultSubject = "subject"
        const val defaultAudience = "some-audience"
    }
}