package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.integrasjoner.RETRY_BACKOFF_5000MS
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

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
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
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

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        val uri = URI.create("$familieOppdragBackendUri/oppdrag")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Iverksetter mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(utbetalingsoppdrag)
                .retrieve()
                .body()!!
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragBackendUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Henter oppdragstatus fra Økonomi",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(oppdragId)
                .retrieve()
                .body()!!
        }
    }

    fun grensesnittavstemOppdrag(
        fraDato: LocalDateTime,
        tilDato: LocalDateTime,
        avstemmingId: UUID?,
    ): String {
        val uri = URI.create("$familieOppdragBackendUri/grensesnittavstemming")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Gjør grensesnittavstemming mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    GrensesnittavstemmingRequest(
                        fagsystem = FAGSYSTEM,
                        fra = fraDato,
                        til = tilDato,
                        avstemmingId = avstemmingId,
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun konsistensavstemOppdragStart(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/v2/konsistensavstemming" +
                    "?sendStartmelding=true&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Start konsistensavstemming mot oppdrag i batch",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    KonsistensavstemmingRequestV2(
                        fagsystem = FAGSYSTEM,
                        avstemmingstidspunkt = avstemmingsdato,
                        perioderForBehandlinger = emptyList(),
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Konsistenstavstemmer chunk mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    KonsistensavstemmingRequestV2(
                        fagsystem = FAGSYSTEM,
                        avstemmingstidspunkt = avstemmingsdato,
                        perioderForBehandlinger = perioderTilAvstemming,
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun konsistensavstemOppdragAvslutt(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=true&transaksjonsId=$transaksjonsId",
            )
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Avslutt konsistensavstemming mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    KonsistensavstemmingRequestV2(
                        fagsystem = FAGSYSTEM,
                        avstemmingstidspunkt = avstemmingsdato,
                        perioderForBehandlinger = emptyList(),
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun hentSisteUtbetalingsoppdragForFagsaker(
        fagsakIder: Set<Long>,
    ): List<UtbetalingsoppdragMedBehandlingOgFagsak> {
        val uri = URI.create("$familieOppdragBackendUri/$FAGSYSTEM/fagsaker/siste-utbetalingsoppdrag")

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Hent utbetalingsoppdrag for fagsaker",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(fagsakIder)
                .retrieve()
                .body()!!
        }
    }

    fun opprettManuellKvitteringPåOppdrag(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragBackendUri/oppdrag/manuell-kvittering")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Oppretter kvitteringsmelding på oppdrag og setter status til KVITTERT_OK",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(oppdragId)
                .retrieve()
                .body()!!
        }
    }

    companion object {
        private const val FAMILIE_OPPDRAG_BACKEND = "familie-oppdrag-backend"
    }
}
