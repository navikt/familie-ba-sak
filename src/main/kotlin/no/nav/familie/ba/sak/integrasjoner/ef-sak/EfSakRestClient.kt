package no.nav.familie.ba.sak.integrasjoner.`ef-sak`

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EfSakRestClient(
    @Value("\${FAMILIE_EF_SAK_API_URL}") private val efSakBaseUrl: URI,
    restTemplate: RestOperations
) :
    AbstractRestClient(restTemplate, "ef-sak") {

    fun hentPerioderMedFullOvergangsstønad(personIdent: String): PerioderOvergangsstønadResponse {
        val uri =
            UriComponentsBuilder.fromUri(efSakBaseUrl).pathSegment("/ekstern/perioder/full-overgangsstonad").build()
                .toUri()

        try {
            val response = postForEntity<Ressurs<PerioderOvergangsstønadResponse>>(uri, PersonIdent(personIdent))
            secureLogger.info("Response fra ef-sak ved henting av perioder med full overgangsstønad på person $personIdent: ${response.toSecureString()}, ${response.data?.convertDataClassToJson()}")

            return response.getDataOrThrow()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av full overgangsstønadsperioder fra ef-sak", e)
        }
    }

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
