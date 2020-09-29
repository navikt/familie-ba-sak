package no.nav.familie.ba.sak.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.http.sts.StsRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class StsOnlyPdlRestClient(@Value("\${PDL_URL}") pdlBaseUrl: URI,
                           @Qualifier("sts") override val restTemplate: RestOperations,
                           stsRestClient: StsRestClient)
    : PdlRestClient(pdlBaseUrl, restTemplate, stsRestClient) {

    fun hentAdressebeskyttelse(personIdent: String): List<Adressebeskyttelse> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = hentGraphqlQuery("hent-adressebeskyttelse"))

        val response = try {
            postForEntity<PdlAdressebeskyttelseResponse>(pdlUri, pdlPersonRequest, httpHeaders())
        } catch (e: Exception) {
            throw Feil(message = "Feil ved oppslag på person. Gav feil: ${e.message}",
                       frontendFeilmelding = "Feil oppsto ved oppslag på person $personIdent",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }

        if (!response.harFeil()) return response.data.person!!.adressebeskyttelse
        throw Feil(message = "Fant ikke data på person: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke data for person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }
}