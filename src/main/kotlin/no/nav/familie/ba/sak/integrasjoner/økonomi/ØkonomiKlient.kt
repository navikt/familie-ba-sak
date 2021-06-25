package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.RestSimulerResultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDateTime

@Service
class ØkonomiKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearer") restOperations: RestOperations
) : AbstractRestClient(restOperations, "økonomi_barnetrygd") {

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): Ressurs<String> =
        postForEntity<Ressurs<String>>(uri = URI.create("$familieOppdragUri/oppdrag"), utbetalingsoppdrag)
            .also { assertGenerelleSuksessKriterier(it) }

    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): Ressurs<DetaljertSimuleringResultat>? =
        postForEntity(
            uri = URI.create("$familieOppdragUri/simulering/v1"),
            utbetalingsoppdrag
        )

    fun hentEtterbetalingsbeløp(utbetalingsoppdrag: Utbetalingsoppdrag): Ressurs<RestSimulerResultat> =
        postForEntity<Ressurs<RestSimulerResultat>>(
            uri = URI.create("$familieOppdragUri/simulering/etterbetalingsbelop"),
            utbetalingsoppdrag
        )
            .also { assertGenerelleSuksessKriterier(it) }

    fun hentStatus(oppdragId: OppdragId): Ressurs<OppdragStatus> =
        postForEntity<Ressurs<OppdragStatus>>(
            uri = URI.create("$familieOppdragUri/status"),
            oppdragId
        )
            .also { assertGenerelleSuksessKriterier(it) }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): Ressurs<String> =
        postForEntity<Ressurs<String>>(
            uri = URI.create("$familieOppdragUri/grensesnittavstemming"),
            GrensesnittavstemmingRequest(
                fagsystem = FAGSYSTEM,
                fra = fraDato,
                til = tilDato
            )
        )
            .also { assertGenerelleSuksessKriterier(it) }

    fun konsistensavstemOppdrag(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>
    ): Ressurs<String> =
        postForEntity<Ressurs<String>>(
            uri = URI.create("$familieOppdragUri/v2/konsistensavstemming"),
            KonsistensavstemmingRequestV2(
                fagsystem = FAGSYSTEM,
                avstemmingstidspunkt = avstemmingsdato,
                perioderForBehandlinger = perioderTilAvstemming
            )
        )
            .also { assertGenerelleSuksessKriterier(it) }
}
