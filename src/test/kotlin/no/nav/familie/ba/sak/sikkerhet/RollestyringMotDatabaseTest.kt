package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRequest
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity


@SpringBootTest(
        classes = [ApplicationConfig::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = ["discoveryUrl=http://localhost:1234/test/.well-known/openid-configuration"],
)
@ActiveProfiles("postgres")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@Tag("integration")
@EnableMockOAuth2Server(port = 1234)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RollestyringMotDatabaseTest(
        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val restTemplate: RestTemplate,

        @Autowired
        private val mockOAuth2Server: MockOAuth2Server
) {

    @LocalServerPort
    private val port = 0

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal kaste feil når innlogget bruker ikke har skrivetilgang`() {
        val fnr = randomFnr()

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(token("azuread", "subject", "audience").toString())
        val requestEnty = HttpEntity<String>(objectMapper.writeValueAsString(FagsakRequest(
                personIdent = fnr
        )), header)

        val response = restTemplate.postForEntity<Ressurs<Fagsak>>("http://localhost:$port/api/fagsaker", requestEnty)

        assertEquals(403, response.statusCode)
        assertEquals("Du har ikke tilgang til denne handlingen", response.body?.melding)
    }

    private fun token(issuerId: String, subject: String, audience: String): String? {
        return mockOAuth2Server.issueToken(
                issuerId,
                "theclientid",
                DefaultOAuth2TokenCallback(
                        issuerId,
                        subject,
                        audience,
                        mapOf("azuread" to mapOf("groups" to "test")),
                        3600
                )
        ).serialize()
    }
}