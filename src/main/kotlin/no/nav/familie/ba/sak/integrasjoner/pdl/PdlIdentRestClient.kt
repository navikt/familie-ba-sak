package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class PdlIdentRestClient(
    @Value("\${PDL_URL}") pdlBaseUrl: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "pdl.ident") {
    protected val pdlUri = UriUtil.uri(pdlBaseUrl, PATH_GRAPHQL)

    private val hentIdenterQuery = hentGraphqlQuery("hentIdenter")

    @Cacheable("identer", cacheManager = "shortCache")
    fun hentIdenter(personIdent: String, historikk: Boolean): List<IdentInformasjon> {
        val hentIdenter = hentIdenter(personIdent)

        return if (historikk) {
            hentIdenter.data.pdlIdenter!!.identer.map { it }
        } else {
            hentIdenter.data.pdlIdenter!!.identer.filter { !it.historisk }.map { it }
        }
    }

    private fun hentIdenter(personIdent: String): PdlHentIdenterResponse {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentIdenterQuery
        )
        val response = postForEntity<PdlHentIdenterResponse>(
            pdlUri,
            pdlPersonRequest,
            httpHeaders()
        )

        if (!response.harFeil()) return response
        throw Feil(
            message = "Fant ikke identer for person: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
            httpStatus = HttpStatus.NOT_FOUND
        )
    }

    fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add("Tema", PDL_TEMA)
        }
    }

    companion object {

        private const val PATH_GRAPHQL = "graphql"
        private const val PDL_TEMA = "BAR"
    }
}