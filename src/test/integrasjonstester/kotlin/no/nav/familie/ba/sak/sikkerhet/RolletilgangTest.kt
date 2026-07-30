package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.datagenerator.nyOrdinærBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
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
import org.springframework.web.client.body
import org.springframework.web.client.toEntity
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
    "mock-pdl",
    "mock-pdl-klient",
    "mock-ident-klient",
    "mock-infotrygd-barnetrygd",
    "fake-tilbakekreving-klient",
    "mock-brev-klient",
    "fake-økonomi-klient",
    "mock-infotrygd-feed",
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
                    "groups" to listOf("VEILEDER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity(
                jsonMapper.writeValueAsString(
                    FagsakRequest(
                        personIdent = fnr,
                    ),
                ),
                header,
            )

        val error =
            assertThrows<HttpClientErrorException> {
                restClient
                    .post()
                    .uri(hentUrl("/api/fagsaker"))
                    .headers { h -> h.addAll(requestEntity.headers) }
                    .body(requestEntity.body!!)
                    .retrieve()
                    .body<Ressurs<MinimalFagsakDto>>()
            }

        val ressurs: Ressurs<MinimalFagsakDto> = jsonMapper.readValue(error.responseBodyAsString)

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
                    "groups" to listOf("VEILEDER", "SAKSBEHANDLER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )

        val response =
            restClient
                .post()
                .uri(hentUrl("/api/fagsaker"))
                .headers { it.addAll(header) }
                .body(
                    jsonMapper.writeValueAsString(
                        FagsakRequest(
                            personIdent = fnr,
                        ),
                    ),
                ).retrieve()
                .toEntity<Ressurs<MinimalFagsakDto>>()

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(Ressurs.Status.SUKSESS, response.body!!.status)
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
                    "groups" to listOf("VEILEDER"),
                    "name" to "Mock McMockface",
                    "NAVident" to "Z0000",
                ),
            ),
        )
        val requestEntity =
            HttpEntity(
                jsonMapper.writeValueAsString(nyOrdinærBehandling(fagsakId = fagsak.data!!.id)),
                header,
            )

        val error =
            assertThrows<HttpClientErrorException> {
                restClient
                    .post()
                    .uri(hentUrl("/rolletilgang/test-behandlinger"))
                    .headers { h -> h.addAll(requestEntity.headers) }
                    .body(requestEntity.body!!)
                    .retrieve()
                    .body<Ressurs<MinimalFagsakDto>>()
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
            HttpEntity(
                jsonMapper.writeValueAsString(
                    listOf(1L, 2L),
                ),
                header,
            )

        val error =
            assertThrows<HttpClientErrorException> {
                restClient
                    .post()
                    .uri(hentUrl("/api/forvalter/ferdigstill-oppgaver"))
                    .headers { h -> h.addAll(requestEntity.headers) }
                    .body(requestEntity.body!!)
                    .retrieve()
                    .body<Ressurs<MinimalFagsakDto>>()
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
            HttpEntity(
                jsonMapper.writeValueAsString(
                    emptyList<Long>(),
                ),
                header,
            )

        val response =
            restClient
                .post()
                .uri(hentUrl("/api/forvalter/ferdigstill-oppgaver"))
                .headers { h -> h.addAll(requestEntity.headers) }
                .body(requestEntity.body!!)
                .retrieve()
                .toEntity(String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
