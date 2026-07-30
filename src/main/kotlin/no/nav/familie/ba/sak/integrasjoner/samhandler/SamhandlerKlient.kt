package no.nav.familie.ba.sak.integrasjoner.samhandler

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfo
import no.nav.familie.kontrakter.ba.tss.SøkSamhandlerInfoRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Service
class SamhandlerKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("økonomiRestClient") private val restClient: RestClient,
) {
    @Cacheable("hent-samhandler", cacheManager = "dailyCache")
    fun hentSamhandler(orgNummer: String): SamhandlerInfo {
        val uri = URI.create("$familieOppdragUri/tss/orgnr/$orgNummer")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter samhandler fra TSS",
        ) {
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body()!!
        }
    }

    fun søkSamhandlere(
        navn: String?,
        postnummer: String?,
        område: String?,
        side: Int,
    ): SøkSamhandlerInfo {
        val uri = URI.create("$familieOppdragUri/tss/navn")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Søk samhandler fra TSS",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(SøkSamhandlerInfoRequest(navn, side, postnummer, område))
                .retrieve()
                .body()!!
        }
    }
}
