package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.sikkerhet.Rolle
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpStatusCodeException
import tools.jackson.module.kotlin.readValue

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
)
class SecurityConfigurationTest : WebSpringAuthTestRunner() {
    @Nested
    inner class PermitAll {
        @Test
        fun `internal endepunkt er tilgjengelig uten token`() {
            val response =
                restClient
                    .get()
                    .uri(hentUrl("/internal/health/liveness"))
                    .retrieve()
                    .toEntity(String::class.java)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @Nested
    inner class UtenToken {
        @Test
        fun `api-kall uten token returnerer 401`() {
            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/fagsaker/1"))
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            val body = jsonMapper.readValue<Ressurs<Any>>(feil.responseBodyAsString)
            assertThat(body.status).isEqualTo(Ressurs.Status.FEILET)
            assertThat(body.frontendFeilmelding).isEqualTo("Kall ikke autorisert")
        }
    }

    @Nested
    inner class InternAppTilgang {
        @ParameterizedTest
        @EnumSource(value = Rolle::class, names = ["VEILEDER", "SAKSBEHANDLER", "BESLUTTER", "FORVALTER"])
        fun `saksbehandlere har tilgang til interne endepunkter`(
            rolle: Rolle,
        ) {
            val headers = hentHeaders(groups = listOf(rolle.name))

            try {
                restClient
                    .get()
                    .uri(hentUrl("/api/fagsaker/1"))
                    .headers { h -> h.addAll(headers) }
                    .retrieve()
                    .toEntity(String::class.java)
            } catch (e: HttpStatusCodeException) {
                assertThat(e.statusCode).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
            }
        }

        @Test
        fun `m2m-token fra teamfamilie-app har tilgang til interne endepunkter`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(token(mapOf("azp_name" to "dev-gcp:teamfamilie:tilfeldig-applikasjon")))
                }

            try {
                restClient
                    .get()
                    .uri(hentUrl("/api/fagsaker/1"))
                    .headers { h -> h.addAll(headers) }
                    .retrieve()
                    .toEntity(String::class.java)
            } catch (e: HttpStatusCodeException) {
                assertThat(e.statusCode).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
            }
        }

        @Test
        fun `m2m-token uten teamfamilie-namespace har ikke tilgang til interne endepunkter`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(token(mapOf("azp_name" to "dev-gcp:ukjent-namespace:ukjent-applikasjon")))
                }

            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/fagsaker/1"))
                        .headers { h -> h.addAll(headers) }
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @Nested
    inner class EksternKlagetilgang {
        private fun klageToken() = token(mapOf("azp_name" to "dev-gcp:teamfamilie:familie-klage"))

        @Test
        fun `token fra annen applikasjon enn klage har ikke tilgang til klage-endepunkt`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(token(mapOf("azp_name" to "dev-gcp:teamfamilie:ikke-klage")))
                }

            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/klage/fagsaker/1/kan-opprette-revurdering-klage"))
                        .headers { h -> h.addAll(headers) }
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `klage-token har tilgang til klage-endepunkt`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(klageToken())
                }

            try {
                restClient
                    .get()
                    .uri(hentUrl("/api/klage/fagsaker/1/kan-opprette-revurdering-klage"))
                    .headers { h -> h.addAll(headers) }
                    .retrieve()
                    .toEntity(String::class.java)
            } catch (e: HttpStatusCodeException) {
                assertThat(e.statusCode).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
            }
        }
    }

    @Nested
    inner class EksternPensjonstilgang {
        private fun pensjonToken(applikasjonNavn: String) = token(mapOf("azp_name" to "dev-gcp:pensjonopptjening:$applikasjonNavn"))

        @Test
        fun `pensjon-token har ikke tilgang til generelt api-endepunkt`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(pensjonToken("omsorgsopptjening-start-innlesning"))
                }

            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/fagsaker/1"))
                        .headers { h -> h.addAll(headers) }
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @ParameterizedTest
        @ValueSource(strings = ["omsorgsopptjening-start-innlesning", "omsorgsopptjening-start-innlesning-q1"])
        fun `pensjon-token har tilgang til pensjon-endepunkt`(
            applikasjonNavn: String,
        ) {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(pensjonToken(applikasjonNavn))
                }

            try {
                restClient
                    .get()
                    .uri(hentUrl("/api/ekstern/pensjon/bestill-personer-med-barnetrygd/2023"))
                    .headers { h -> h.addAll(headers) }
                    .retrieve()
                    .toEntity(String::class.java)
            } catch (e: HttpStatusCodeException) {
                assertThat(e.statusCode).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
            }
        }
    }

    @Nested
    inner class EksternBisystilgang {
        private fun bisysToken(applikasjonNavn: String) = token(mapOf("azp_name" to "dev-gcp:bidrag:$applikasjonNavn"))

        @Test
        fun `bisys-token har ikke tilgang til generelt api-endepunkt`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(bisysToken("bidrag-grunnlag"))
                }

            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/fagsaker/1"))
                        .headers { h -> h.addAll(headers) }
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @ParameterizedTest
        @ValueSource(strings = ["bidrag-grunnlag", "bidrag-grunnlag-feature"])
        fun `bisys-token har tilgang til bisys-endepunkt`(
            applikasjonNavn: String,
        ) {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(bisysToken(applikasjonNavn))
                }

            try {
                restClient
                    .post()
                    .uri(hentUrl("/api/bisys/hent-utvidet-barnetrygd"))
                    .headers { h -> h.addAll(headers) }
                    .retrieve()
                    .toEntity(String::class.java)
            } catch (e: HttpStatusCodeException) {
                assertThat(e.statusCode).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
            }
        }
    }

    @Nested
    inner class TokenXIsolasjon {
        @Test
        fun `tokenx-token blir avvist på azuread-endepunkt`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(hentTokenForTokenX("12345678910"))
                }

            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/fagsaker/1"))
                        .headers { h -> h.addAll(headers) }
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `azure-token blir avvist på tokenx-endepunkt`() {
            val headers = hentHeaders()

            val feil =
                assertThrows<HttpStatusCodeException> {
                    restClient
                        .get()
                        .uri(hentUrl("/api/minside/barnetrygd"))
                        .headers { h -> h.addAll(headers) }
                        .retrieve()
                        .toEntity(String::class.java)
                }

            assertThat(feil.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
