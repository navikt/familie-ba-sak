package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.common.BaseService
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.*
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.exchange
import java.net.URI
import java.time.LocalDateTime

private const val OAUTH2_CLIENT_CONFIG_KEY = "familie-oppdrag-clientcredentials"

@Service
class ØkonomiKlient(
        @Value("\${FAMILIE_OPPDRAG_API_URL}")
        private val familieOppdragUri: String,
        restTemplateBuilderMedProxy: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
) : BaseService(
        OAUTH2_CLIENT_CONFIG_KEY,
        restTemplateBuilderMedProxy,
        clientConfigurationProperties,
        oAuth2AccessTokenService
) {

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<String>> {
        val headers = HttpHeaders()
        headers.add("Content-Type", "application/json;charset=UTF-8")
        headers.acceptCharset = listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/oppdrag"),
                HttpMethod.POST,
                HttpEntity(utbetalingsoppdrag, headers))
    }

    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<DetaljertSimuleringResultat>> {
        val headers = HttpHeaders().medContentTypeJsonUTF8()
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/simulering/v1"),
                HttpMethod.POST,
                HttpEntity(utbetalingsoppdrag, headers))
    }

    fun hentEtterbetalingsbeløp(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<RestSimulerResultat>> {
        val headers = HttpHeaders()
                .medContentTypeJsonUTF8()
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/simulering/etterbetalingsbelop"),
                HttpMethod.POST,
                HttpEntity(utbetalingsoppdrag, headers))
    }

    fun hentStatus(oppdragId: OppdragId): ResponseEntity<Ressurs<OppdragStatus>> {
        val headers = HttpHeaders()
                .medContentTypeJsonUTF8()
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/status"),
                HttpMethod.POST,
                HttpEntity(oppdragId, headers))
    }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): ResponseEntity<Ressurs<String>> {
        val headers = HttpHeaders()
        headers.acceptCharset = listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/grensesnittavstemming/$FAGSYSTEM/?fom=$fraDato&tom=$tilDato"),
                HttpMethod.POST,
                HttpEntity<String>(headers))
    }

    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime,
                                perioderTilAvstemming: List<PerioderForBehandling>): ResponseEntity<Ressurs<String>> {
        
        val body = KonsistensavstemmingRequestV2(fagsystem = FAGSYSTEM,
                                                 avstemmingstidspunkt = avstemmingsdato,
                                                 perioderForBehandlinger = perioderTilAvstemming)

        val headers = HttpHeaders().medContentTypeJsonUTF8()

        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/v2/konsistensavstemming"),
                HttpMethod.POST,
                HttpEntity<KonsistensavstemmingRequestV2>(body, headers))
    }
}
