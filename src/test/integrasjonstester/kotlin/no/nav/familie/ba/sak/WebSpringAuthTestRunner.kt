package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.config.AbstractMockkSpringRunner
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestClient

@SpringBootTest(
    classes = [ApplicationConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "rolle.veileder: VEILEDER",
        "rolle.saksbehandler: SAKSBEHANDLER",
        "rolle.beslutter: BESLUTTER",
        "rolle.forvalter: FORVALTER",
    ],
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
abstract class WebSpringAuthTestRunner : AbstractMockkSpringRunner() {
    @Autowired
    @Qualifier("utenAuthRestClient")
    lateinit var restClient: RestClient

    @LocalServerPort
    private val port = 0

    fun hentUrl(path: String) = "http://localhost:$port$path"

    fun token(
        claims: Map<String, Any>,
        subject: String = DEFAULT_SUBJECT,
        audience: String = DEFAULT_AUDIENCE,
        issuerId: String = AZUREAD_ISSUER_ID,
        clientId: String = DEFAULT_CLIENT_ID,
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId,
                clientId,
                DefaultOAuth2TokenCallback(
                    issuerId = issuerId,
                    subject = subject,
                    audience = listOf(audience),
                    claims = claims,
                    expiry = 3600,
                ),
            ).serialize()

    fun hentHeaders(groups: List<String>? = null): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.setBearerAuth(
            token(
                mapOf(
                    "groups" to (groups ?: listOf(BehandlerRolle.SAKSBEHANDLER.name)),
                    "azp" to "azp-test",
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        return httpHeaders
    }

    fun hentHeadersForSystembruker(groups: List<String>? = null): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.setBearerAuth(
            token(
                mapOf(
                    "groups" to (groups ?: listOf(BehandlerRolle.SYSTEM.name)),
                    "azp_name" to ":teamfamilie:azp-test",
                    "name" to SYSTEM_FORKORTELSE,
                    "preferred_username" to SYSTEM_FORKORTELSE,
                ),
            ),
        )
        return httpHeaders
    }

    /** Genererer et TokenX-token. [fnr] legges inn som `pid`-claim; null utelater claimet. */
    fun hentTokenForTokenX(fnr: String? = null): String =
        token(
            claims =
                mutableMapOf("acr" to "idporten-loa-high").apply {
                    if (fnr != null) {
                        this["pid"] = fnr
                    }
                },
            subject = fnr ?: DEFAULT_SUBJECT,
            audience = DEFAULT_AUDIENCE,
            issuerId = TOKENX_ISSUER_ID,
        )

    companion object {
        const val AZUREAD_ISSUER_ID = "azuread"
        const val TOKENX_ISSUER_ID = "tokenx"
        const val DEFAULT_SUBJECT = "subject"
        const val DEFAULT_AUDIENCE = "some-audience"
        const val DEFAULT_CLIENT_ID = "theclientid"
    }
}
