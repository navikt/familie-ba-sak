package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.config.RestTemplateConfig.Companion.RETRY_BACKOFF_500MS
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@Service
class ØkonomiKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearer") restOperations: RestOperations
) : AbstractRestClient(restOperations, "økonomi_barnetrygd") {

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): Ressurs<String> =
        postForEntity<Ressurs<String>>(uri = URI.create("$familieOppdragUri/oppdrag"), utbetalingsoppdrag)
            .also { assertGenerelleSuksessKriterier(it) }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_500MS)
    )
    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): Ressurs<DetaljertSimuleringResultat>? {
        val uri = URI.create("$familieOppdragUri/simulering/v1")

        return kallEksternTjeneste(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter simulering fra Økonomi",
        ) {

            postForEntity(
                uri = uri,
                utbetalingsoppdrag
            )
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter opprdagstatus fra Økonomi",
        ) {
            postForEntity(uri = uri, oppdragId)
        }
    }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): Ressurs<String> {
        val uri = URI.create("$familieOppdragUri/grensesnittavstemming")
        return kallEksternTjeneste(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Gjør avstemming mot oppdrag",
        ) {
            postForEntity<Ressurs<String>>(
                uri = uri,
                GrensesnittavstemmingRequest(
                    fagsystem = FAGSYSTEM,
                    fra = fraDato,
                    til = tilDato
                )
            ).also { assertGenerelleSuksessKriterier(it) }
        }
    }

    fun konsistensavstemOppdrag(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>
    ): Ressurs<String> {
        val uri = URI.create("$familieOppdragUri/v2/konsistensavstemming")

        return kallEksternTjeneste(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Gjør konsistensavstemming mot oppdrag (Deprecated)",
        ) {
            postForEntity<Ressurs<String>>(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = perioderTilAvstemming
                )
            ).also { assertGenerelleSuksessKriterier(it) }
        }
    }

    fun konsistensavstemOppdragStart(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID
    ): Ressurs<String> {
        val uri = URI.create(
            "$familieOppdragUri/v2/konsistensavstemming" +
                "?sendStartmelding=true&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId"
        )

        return kallEksternTjeneste(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Start konsistensavstemming mot oppdrag i batch",
        ) {
            postForEntity<Ressurs<String>>(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList()
                )
            ).also { assertGenerelleSuksessKriterier(it) }
        }
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
    ): Ressurs<String> {
        val uri = URI.create(
            "$familieOppdragUri/v2/konsistensavstemming" +
                "?sendStartmelding=false&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId"
        )
        return kallEksternTjeneste(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Konsistenstavstemmer chunk mot oppdrag",
        ) {
            postForEntity<Ressurs<String>>(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = perioderTilAvstemming
                )
            ).also { assertGenerelleSuksessKriterier(it) }
        }
    }

    fun konsistensavstemOppdragAvslutt(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID
    ): Ressurs<String> {
        val uri = URI.create(
            "$familieOppdragUri/v2/konsistensavstemming" +
                "?sendStartmelding=false&sendAvsluttmelding=true&transaksjonsId=$transaksjonsId"
        )
        return kallEksternTjeneste(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Avslutt konsistensavstemming mot oppdrag",
        ) {
            postForEntity<Ressurs<String>>(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList()
                )
            ).also { assertGenerelleSuksessKriterier(it) }
        }
    }
}
