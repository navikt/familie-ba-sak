package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.common.BaseService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import java.net.URI

@Component
class IntegrasjonTjeneste (
        @Value("\${FAMILIE_INTEGRASJONER_API_URL}")
        private val integrasjonerServiceUri: URI,
        restTemplateBuilderMedProxy: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
) : BaseService(OAUTH2_CLIENT_CONFIG_KEY, restTemplateBuilderMedProxy, clientConfigurationProperties, oAuth2AccessTokenService) {
    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentAktørId(personident: String?): AktørId? {
        if (personident == null || personident.isEmpty()) {
            throw IntegrasjonException("Ved henting av aktør id er personident null eller tom")
        }
        val uri = URI.create("$integrasjonerServiceUri/aktoer/v1")
        logger.info("Henter aktørId fra $integrasjonerServiceUri")
        return try {
            val response: ResponseEntity<Ressurs<MutableMap<*, *>>> = requestMedPersonIdent(uri, personident)
            secureLogger.info("Vekslet inn fnr: {} til aktørId: {}", personident, response.body)
            val aktørId = response.body?.data?.get("aktørId").toString()
            if (aktørId.isEmpty()) {
                throw IntegrasjonException("AktørId fra integrasjonstjenesten er tom")
            } else {
                AktørId(aktørId)
            }
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av aktørId", e, uri, personident)
        }
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentPersoninfoFor(personIdent: String): Personinfo? {
        val uri = URI.create("$integrasjonerServiceUri/personopplysning/v1/info")
        logger.info("Henter personinfo fra $integrasjonerServiceUri")
        return try {
            val response = requestMedPersonIdent<Ressurs<Personinfo>>(uri, personIdent)
            secureLogger.info("Personinfo for {}: {}", personIdent, response.body?.data)
            objectMapper.convertValue<Personinfo>(response.body?.data, Personinfo::class.java)
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av personinfo", e, uri, personIdent)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonTjeneste::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private const val OAUTH2_CLIENT_CONFIG_KEY = "familie-integrasjoner-clientcredentials"
    }
}