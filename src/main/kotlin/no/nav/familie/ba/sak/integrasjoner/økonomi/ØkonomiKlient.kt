package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.config.RestTemplateConfig.Companion.RETRY_BACKOFF_500MS
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@Service
class ØkonomiKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "økonomi_barnetrygd") {
    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        val uri = URI.create("$familieOppdragUri/oppdrag")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Iverksetter mot oppdrag",
        ) {
            postForEntity(uri = uri, utbetalingsoppdrag)
        }
    }

    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val uri = URI.create("$familieOppdragUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Henter simulering på fagsak ${utbetalingsoppdrag.saksnummer} fra Økonomi",
        ) {
            retryVedException(5000).execute {
                postForEntity(uri = uri, utbetalingsoppdrag)
            }
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Henter oppdragstatus fra Økonomi",
        ) {
            postForEntity(uri = uri, oppdragId)
        }
    }

    fun grensesnittavstemOppdrag(
        fraDato: LocalDateTime,
        tilDato: LocalDateTime,
        avstemmingId: UUID?,
    ): String {
        val uri = URI.create("$familieOppdragUri/grensesnittavstemming")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Gjør grensesnittavstemming mot oppdrag",
        ) {
            postForEntity(
                uri = uri,
                GrensesnittavstemmingRequest(
                    fagsystem = FAGSYSTEM,
                    fra = fraDato,
                    til = tilDato,
                    avstemmingId = avstemmingId,
                ),
            )
        }
    }

    fun konsistensavstemOppdragStart(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/v2/konsistensavstemming" +
                    "?sendStartmelding=true&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Start konsistensavstemming mot oppdrag i batch",
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList(),
                ),
            )
        }
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Konsistenstavstemmer chunk mot oppdrag",
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = perioderTilAvstemming,
                ),
            )
        }
    }

    fun konsistensavstemOppdragAvslutt(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=true&transaksjonsId=$transaksjonsId",
            )
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Avslutt konsistensavstemming mot oppdrag",
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList(),
                ),
            )
        }
    }

    fun hentSisteUtbetalingsoppdragForFagsaker(
        fagsakIder: Set<Long>,
    ): List<UtbetalingsoppdragMedBehandlingOgFagsak> {
        val uri = URI.create("$familieOppdragUri/$FAGSYSTEM/fagsaker/siste-utbetalingsoppdrag")

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Hent utbetalingsoppdrag for fagsaker",
        ) { postForEntity(uri = uri, payload = fagsakIder) }
    }

    fun opprettManuellKvitteringPåOppdrag(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragUri/oppdrag/manuell-kvittering")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "Oppretter kvitteringsmelding på oppdrag og setter status til KVITTERT_OK",
        ) {
            postForEntity(uri = uri, oppdragId)
        }
    }

    companion object {
        const val FAMILIE_OPPDRAG = "familie-oppdrag"
    }
}

data class UtbetalingsoppdragMedBehandlingOgFagsak(
    val fagsakId: Long,
    val behandlingId: Long,
    val utbetalingsoppdrag: Utbetalingsoppdrag,
)
