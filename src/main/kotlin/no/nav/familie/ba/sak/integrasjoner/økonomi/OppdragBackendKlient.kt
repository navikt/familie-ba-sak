package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.integrasjoner.RETRY_BACKOFF_5000MS
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

// Går mot familie-oppdrag-backend som kjører i GCP.
@Service
class OppdragBackendKlient(
    @Value("\${FAMILIE_OPPDRAG_BACKEND_API_URL}")
    private val familieOppdragBackendUri: String,
    @Qualifier("oppdragBackendRestClient") private val restClient: RestClient,
    @Value("$RETRY_BACKOFF_5000MS") private val retryBackoffDelay: Long,
) {
    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val uri = URI.create("$familieOppdragBackendUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag-backend",
            uri = uri,
            formål = "Henter simulering på fagsak ${utbetalingsoppdrag.saksnummer} fra Økonomi",
        ) {
            retryVedException(retryBackoffDelay).execute {
                restClient
                    .post()
                    .uri(uri)
                    .body(utbetalingsoppdrag)
                    .retrieve()
                    .body()!!
            }
        }
    }
}
