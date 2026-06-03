package no.nav.familie.ba.sak.integrasjoner.ef

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Component
class EfSakRestKlient(
    @Value("\${FAMILIE_EF_SAK_API_URL}") private val efSakBaseUrl: URI,
    @Qualifier("efSakRestClient") private val restClient: RestClient,
) {
    fun hentPerioderMedFullOvergangsstønad(personIdent: String): EksternePerioderResponse {
        val uri = URI.create("$efSakBaseUrl/ekstern/perioder/full-overgangsstonad")

        return kallEksternTjeneste<Ressurs<EksternePerioderResponse>>(
            tjeneste = "ef-sak overgangsstønad",
            uri = uri,
            formål = "Hente perioder med full overgangsstønad",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(PersonIdent(personIdent))
                .retrieve()
                .body()!!
        }.getDataOrThrow()
    }
}
