package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.*
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
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

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<String>> =
            postForEntity<ResponseEntity<Ressurs<String>>>(uri = URI.create("$familieOppdragUri/oppdrag"), utbetalingsoppdrag)
                    .also { assertGenerelleSuksessKriterier(it.body) }

    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<DetaljertSimuleringResultat>> =
            postForEntity<ResponseEntity<Ressurs<DetaljertSimuleringResultat>>>(
                    uri = URI.create("$familieOppdragUri/simulering/v1"),
                    utbetalingsoppdrag)
                    .also { assertGenerelleSuksessKriterier(it.body) }

    fun hentEtterbetalingsbeløp(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<RestSimulerResultat>> =
            postForEntity<ResponseEntity<Ressurs<RestSimulerResultat>>>(
                    uri = URI.create("$familieOppdragUri/simulering/etterbetalingsbelop"),
                    utbetalingsoppdrag)
                    .also { assertGenerelleSuksessKriterier(it.body) }

    fun hentStatus(oppdragId: OppdragId): ResponseEntity<Ressurs<OppdragStatus>> =
            postForEntity<ResponseEntity<Ressurs<OppdragStatus>>>(
                    uri = URI.create("$familieOppdragUri/status"),
                    oppdragId)
                    .also { assertGenerelleSuksessKriterier(it.body) }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): ResponseEntity<Ressurs<String>> =
            postForEntity<ResponseEntity<Ressurs<String>>>(
                    uri = URI.create("$familieOppdragUri/grensesnittavstemming/$FAGSYSTEM/?fom=$fraDato&tom=$tilDato"),
                    "")
                    .also { assertGenerelleSuksessKriterier(it.body) }

    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime,
                                perioderTilAvstemming: List<PerioderForBehandling>): ResponseEntity<Ressurs<String>> =
            postForEntity<ResponseEntity<Ressurs<String>>>(
                    uri = URI.create("$familieOppdragUri/v2/konsistensavstemming"),
                    KonsistensavstemmingRequestV2(fagsystem = FAGSYSTEM,
                                                  avstemmingstidspunkt = avstemmingsdato,
                                                  perioderForBehandlinger = perioderTilAvstemming))
                    .also { assertGenerelleSuksessKriterier(it.body) }
}
