package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.datagenerator.nyOrdinærBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.postForEntity
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
    "mock-pdl",
    "mock-ident-klient",
    "mock-infotrygd-barnetrygd",
    "fake-tilbakekreving-klient",
    "mock-brev-klient",
    "fake-økonomi-klient",
    "mock-infotrygd-feed",
    "mock-rest-template-config",
    "mock-task-repository",
    "mock-task-service",
    "mock-unleash",
)
class RolletilgangTest(
    @Autowired
    private val fagsakService: FagsakService,
) : WebSpringAuthTestRunner() {
    @Test
    fun `Skal kaste feil når innlogget veileder prøver å opprette fagsak gjennom rest-endepunkt`() {
        val fnr = randomFnr()

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("VEILDER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity<String>(
                jsonMapper.writeValueAsString(
                    FagsakRequest(
                        personIdent = fnr,
                    ),
                ),
                header,
            )

        val error =
            assertThrows<HttpClientErrorException> {
                restTemplate.postForEntity<Ressurs<Fagsak>>(
                    hentUrl("/api/fagsaker"),
                    requestEntity,
                )
            }

        val ressurs: Ressurs<Fagsak> = jsonMapper.readValue(error.responseBodyAsString)

        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
        assertEquals(Ressurs.Status.IKKE_TILGANG, ressurs.status)
        assertEquals(
            "Mock McMockface med rolle VEILEDER har ikke tilgang til å opprette fagsak. Krever SAKSBEHANDLER.",
            ressurs.melding,
        )
    }

    @Test
    fun `Skal få 201 når innlogget saksbehandler prøver å opprette fagsak gjennom rest-endepunkt, tester også db-tilgangskontroll`() {
        val fnr = randomFnr()

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("VEILDER", "SAKSBEHANDLER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity<String>(
                jsonMapper.writeValueAsString(
                    FagsakRequest(
                        personIdent = fnr,
                    ),
                ),
                header,
            )

        val response = restTemplate.postForEntity<Ressurs<Fagsak>>(hentUrl("/api/fagsaker"), requestEntity)
        val ressurs = response.body

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(Ressurs.Status.SUKSESS, ressurs?.status)
    }

    @Test
    fun `Skal kaste feil når innlogget veileder prøver å opprette behandling gjennom test-rest-endepunkt som validerer på db-nivå`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))

        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("VEILDER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity<String>(
                jsonMapper.writeValueAsString(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.data!!.id)),
                header,
            )

        val error =
            assertThrows<HttpClientErrorException> {
                restTemplate.postForEntity<Ressurs<Behandling>>(
                    hentUrl("/rolletilgang/test-behandlinger"),
                    requestEntity,
                )
            }

        val ressurs: Ressurs<Behandling> = jsonMapper.readValue(error.responseBodyAsString)

        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
        assertEquals(Ressurs.Status.IKKE_TILGANG, ressurs.status)
        assertEquals(
            "Mock McMockface med rolle VEILEDER har ikke skrivetilgang til databasen.",
            ressurs.melding,
        )
    }

    @Test
    fun `Skal kaste feil når innlogget saksbehandler prøver å kalle på et forvalterendepunkt`() {
        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("SAKSBEHANDLER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity<String>(
                jsonMapper.writeValueAsString(
                    listOf(1L, 2L),
                ),
                header,
            )

        val error =
            assertThrows<HttpClientErrorException> {
                restTemplate.postForEntity<Ressurs<Fagsak>>(
                    hentUrl("/api/forvalter/ferdigstill-oppgaver"),
                    requestEntity,
                )
            }

        val ressurs: Ressurs<Fagsak> = jsonMapper.readValue(error.responseBodyAsString)

        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
        assertEquals(Ressurs.Status.IKKE_TILGANG, ressurs.status)
        assertEquals(
            "Mock McMockface har ikke tilgang til å Ferdigstill liste med oppgaver. Krever FORVALTER",
            ressurs.melding,
        )
    }

    @Test
    fun `Skal få 200 OK når innlogget forvalter prøver å kalle på et forvalterendepunkt`() {
        val header = HttpHeaders()
        header.contentType = MediaType.APPLICATION_JSON
        header.setBearerAuth(
            token(
                mapOf(
                    "groups" to listOf("FORVALTER"),
                    "azp" to "azp-test",
                    "name" to "Mock McMockface Forvalter",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity<String>(
                jsonMapper.writeValueAsString(
                    emptyList<Long>(),
                ),
                header,
            )

        val response = restTemplate.postForEntity<String>(hentUrl("/api/forvalter/ferdigstill-oppgaver"), requestEntity)

        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
