package no.nav.familie.ba.sak.integrasjoner.`ef-sak`

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class EfSakRestClient(
    @Value("\${FAMILIE_EF_SAK_API_URL}") private val efSakBaseUrl: URI,
    @Qualifier("jwtBearer") restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "ef-sak") {

    fun hentPerioderMedFullOvergangsstønad(personIdent: String): PerioderOvergangsstønadResponse {

        val uri = UriUtil.uri(efSakBaseUrl, "/ekstern/perioder/full-overgangsstonad")

        return kallEksternTjeneste(
            tjeneste = "tilgangskontroll",
            uri = uri,
            formål = "Hente perioder med full overgangsstønad"
        ) { postForEntity(uri, PersonIdent(personIdent)) }
    }

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
