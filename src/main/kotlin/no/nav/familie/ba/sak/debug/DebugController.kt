package no.nav.familie.ba.sak.debug

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.pdl.PdlRestClient
import no.nav.familie.ba.sak.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.ba.sak.pdl.internal.PdlPersonRequest
import no.nav.familie.ba.sak.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.net.URI

@RestController
@RequestMapping("/debug")
@Unprotected
class DebugController(
        @Qualifier("sts") val restTemplate: RestOperations,
        val personopplysningerService: PersonopplysningerService
) {

    @GetMapping("version")
    fun version(): String {
        return "debug v-1"
    }

    @GetMapping("invoke")
    fun invoke(): String? {
        LOG.info(personopplysningerService.toString())
        LOG.info(personopplysningerService.pdlRestClient.toString())
        val residencePermit = Result.runCatching {
            val personInfo = personopplysningerService.hentPersoninfoFor("17128822658")
            val objectMapper = ObjectMapper()
            "${objectMapper.writeValueAsString(personInfo)} ${objectMapper.writeValueAsString(personopplysningerService.hentOpphold(
                    "17128822658"))}"
        }.fold(
                onSuccess = { it },
                onFailure = { it.message }
        )
        return residencePermit
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