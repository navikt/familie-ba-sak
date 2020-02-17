package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.common.BaseService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
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
                HttpEntity(objectMapper.writeValueAsString(utbetalingsoppdrag), headers))
    }

    fun hentStatus(statusFraOppdragDTO: StatusFraOppdragDTO): ResponseEntity<Ressurs<OppdragProtokollStatus>> {
        val headers = HttpHeaders()
        headers.add("Content-Type", "application/json;charset=UTF-8")
        headers.acceptCharset = listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/status"),
                HttpMethod.POST,
                HttpEntity(objectMapper.writeValueAsString(statusFraOppdragDTO), headers))
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

    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime, oppdragTilAvstemming: List<OppdragId>): ResponseEntity<Ressurs<String>> {
        val headers = HttpHeaders()
        headers.add("Content-Type", "application/json;charset=UTF-8")
        headers.acceptCharset = listOf(Charsets.UTF_8)
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))

        return restOperations.exchange(
                URI.create("$familieOppdragUri/konsistensavstemming/$FAGSYSTEM/?avstemmingsdato=$avstemmingsdato"),
                HttpMethod.POST,
                HttpEntity<List<OppdragId>>(oppdragTilAvstemming, headers))
    }
}
