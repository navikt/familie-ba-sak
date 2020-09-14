package no.nav.familie.ba.sak.debug

import no.nav.familie.ba.sak.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.ba.sak.pdl.internal.PdlPersonRequest
import no.nav.familie.ba.sak.pdl.internal.PdlPersonRequestVariables
import no.nav.security.token.support.core.api.Unprotected
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestOperations

@RestController
@RequestMapping("/debug")
@Unprotected
class DebugController(
        @Qualifier("sts") val restTemplate: RestOperations
) {

    @GetMapping("version")
    fun version(): String{
        return "debug v-1"
    }

    @GetMapping("invoke")
    fun invoke(): String{
        val hentIdenterQuery= StringUtils.normalizeSpace(PersonInfoQuery::class.java.getResource("/pdl/hentIdenter.graphql").readText().replace("\n", ""))
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables("01101800033"),
                                                query = hentIdenterQuery)
        LOG.info("Post http://familie-mock-server:1337/rest/api/pdl/graphql")
        val response= restTemplate.postForEntity<PdlHentIdenterResponse>("http://familie-mock-server:1337/rest/api/pdl/graphql", pdlPersonRequest, PdlHentIdenterResponse::class.java, httpHeaders("BAR"))
        LOG.info("Response ${response.statusCode} ${response.toString()}")
        return response.toString()
    }

    private fun httpHeaders(tema: String): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            add("Tema", tema)
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(DebugController::class.java)
    }
}