package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import java.net.URI

class FødselshendelseKlient(
        private val baSakUrl: String,
        private val headers: HttpHeaders,
        restOperations: RestOperations
) : AbstractRestClient(restOperations, "familie-ba-sak") {

    fun sendTilSak(nyBehandling: NyBehandling) {
        val uri = URI.create("$baSakUrl/api/behandlinger")
        //logger.info("Sender søknad til {}", uri)
        try {
            val response = putForEntity<Ressurs<String>>(uri, nyBehandling)
            //logger.info("Søknad sendt til sak. Status=${response.status}")
        } catch (e: RestClientResponseException) {
            //logger.warn("Innsending til sak feilet. Responskode: {}, body: {}", e.rawStatusCode, e.responseBodyAsString)
            throw IllegalStateException("Innsending til sak feilet. Status: " + e.rawStatusCode
                                        + ", body: " + e.responseBodyAsString, e)
        } catch (e: RestClientException) {
            throw IllegalStateException("Innsending til sak feilet.", e)
        }
    }
}