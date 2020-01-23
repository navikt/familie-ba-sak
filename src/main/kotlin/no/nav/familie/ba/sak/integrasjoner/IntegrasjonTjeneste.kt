package no.nav.familie.ba.sak.integrasjoner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.ba.sak.common.BaseService
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.arkivering.Dokument
import no.nav.familie.kontrakter.felles.arkivering.FilType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
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

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun journalFørVedtaksbrev(pdf: ByteArray, fnr: String, callback: (journalpostID: String) -> Unit) {
        val journalpostId= lagerJournalpostForVedtaksbrev(fnr, pdf)
        if(journalpostId== null){
            throw IntegrasjonException("Kall mot integrasjon feilet ved lager journalpost. fnr: ${fnr}")
        }
        callback(journalpostId)
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun distribuerVedtaksbrev(journalpostId: String) {
        val uri = URI.create("$integrasjonerServiceUri/dist/v1/$journalpostId")
        val request = RequestEntity<Any>(HttpMethod.GET, uri)
        logger.info("Kaller dokdist-tjeneste med journalpostId $journalpostId")
        return try {
            val response = restOperations.exchange(request, typeReference<Ressurs<String>>())
            val bestillingsId: String = response.body?.data.orEmpty()
            if (bestillingsId.isEmpty()) {
                throw IntegrasjonException("BestillingsId fra integrasjonstjenesten mot dokdist er tom")
            } else {
                logger.info("Distribusjon av vedtaksbrev bestilt. BestillingsId:  $bestillingsId")
            }
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved distribusjon av vedtaksbrev", e, uri, "")
        }
    }

    fun lagerJournalpostForVedtaksbrev(fnr: String, pdfByteArray: ByteArray): String?{
        val uri = URI.create("$integrasjonerServiceUri/arkiv/v1")
        logger.info("Sender vedtak pdf til DokArkiv: ${uri}");

        return runCatching<ArkiverDokumentResponse> {
            val dokumenter= listOf(Dokument(pdfByteArray, VEDTAK_FILTYPE, VEDTAK_FILNAVN, null, VEDTAK_DOKUMENT_TYPE))
            val arkiverDokumentRequest= ArkiverDokumentRequest(fnr, true, dokumenter)
            val arkiverDokumentResponse= sendJournalFørRequest(uri, arkiverDokumentRequest)
            arkiverDokumentResponse
        }.onFailure {
            throw IntegrasjonException("Kall mot integrasjon feilet ved lager journalpost.", it, uri, fnr)
        }.getOrNull()?.journalpostId
    }

    private fun sendJournalFørRequest(journalFørEndpoint : URI, arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
        val response= restTemplate.exchange(journalFørEndpoint, HttpMethod.POST, HttpEntity<Any>(arkiverDokumentRequest, headers), Ressurs::class.java)
        val mapper= ObjectMapper().registerKotlinModule()
        return mapper.convertValue(response.body!!.data, ArkiverDokumentResponse::class.java)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonTjeneste::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        val VEDTAK_FILTYPE= FilType.PDFA
        const val VEDTAK_FILNAVN= "ba_vb.pdf"
        const val VEDTAK_DOKUMENT_TYPE= "BARNETRYGD_VEDTAK"

        //for testability make it visible
        const val OAUTH2_CLIENT_CONFIG_KEY = "familie-integrasjoner-clientcredentials"
    }
}