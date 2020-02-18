package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.ba.sak.common.BaseService
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.arkivering.Dokument
import no.nav.familie.kontrakter.felles.arkivering.FilType
import no.nav.familie.kontrakter.felles.distribusjon.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgave
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity.post
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonTjeneste(
        @Value("\${FAMILIE_INTEGRASJONER_API_URL}")
        private val integrasjonerServiceUri: URI,
        restTemplateBuilderMedProxy: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
) : BaseService(OAUTH2_CLIENT_CONFIG_KEY, restTemplateBuilderMedProxy, clientConfigurationProperties, oAuth2AccessTokenService) {

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentAktørId(personident: String?): AktørId {
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
    fun hentPersoninfoFor(personIdent: String): Personinfo {
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
    fun hentBehandlendeEnhetForPersonident(personident: String): List<Arbeidsfordelingsenhet> {
        val uri = URI.create("$integrasjonerServiceUri/arbeidsfordeling/enhet/BAR")

        val headers: MultiValueMap<String, String> = LinkedMultiValueMap()
        headers.add("Nav-Personident", personident)
        val httpEntity: HttpEntity<*> = HttpEntity<Any?>(headers)

        return try {
            val response = restOperations.exchange<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, HttpMethod.GET, httpEntity)
            val data = response.body?.data
            data ?: throw IntegrasjonException("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt",
                                               null,
                                               uri,
                                               personident)
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet", e, uri, personident)
        }
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentBehandlendeEnhet(geografiskTilknytning: String?, diskresjonskode: String?): List<Arbeidsfordelingsenhet> {
        val uri = UriComponentsBuilder.fromPath("$integrasjonerServiceUri/arbeidsfordeling/enhet")
                .queryParam("tema", "BAR")
                .queryParam("geografi", geografiskTilknytning)
                .queryParam("diskresjonskode", diskresjonskode)
                .build().toUri()

        val headers: MultiValueMap<String, String> = LinkedMultiValueMap()
        headers.add(NavHttpHeaders.NAV_CONSUMER_ID.asString(), "familie-ba-sak")
        val httpEntity: HttpEntity<*> = HttpEntity<Any?>(headers)

        return try {
            val response = restOperations.exchange<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, HttpMethod.GET, httpEntity)
            val data = response.body?.data
            data ?: throw IntegrasjonException("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt",
                    null,
                    uri)
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet", e, uri)
        }
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun journalFørVedtaksbrev(pdf: ByteArray, fnr: String, fagsakId: String): String {
        return lagerJournalpostForVedtaksbrev(fnr, fagsakId, pdf)
    }

    fun distribuerVedtaksbrev(journalpostId: String) {
        val uri = URI.create("$integrasjonerServiceUri/dist/v1")
        logger.info("Kaller dokdist-tjeneste med journalpostId $journalpostId")

        Result.runCatching {
            sendDistribusjonRequest(uri, DistribuerJournalpostRequest(journalpostId, "BA", "familie-ba-sak"))
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    Assert.hasText(it.body?.data, "BestillingsId fra integrasjonstjenesten mot dokdist er tom")
                    logger.info("Distribusjon av vedtaksbrev bestilt. BestillingsId:  $it")
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved distribusjon av vedtaksbrev", it, uri, "")
                }
        )
    }

    fun lagerJournalpostForVedtaksbrev(fnr: String, fagsakId: String, pdfByteArray: ByteArray): String {
        val uri = URI.create("$integrasjonerServiceUri/arkiv/v2")
        logger.info("Sender vedtak pdf til DokArkiv: $uri")

        return Result.runCatching {
            val dokumenter = listOf(Dokument(pdfByteArray, FilType.PDFA, dokumentType = VEDTAK_DOKUMENT_TYPE))
            val arkiverDokumentRequest = ArkiverDokumentRequest(fnr, true, dokumenter, fagsakId, "9999")
            val arkiverDokumentResponse = sendJournalFørRequest(uri, arkiverDokumentRequest)
            arkiverDokumentResponse
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    Assert.notNull(it.body?.data, "Ressurs mangler data")

                    val arkiverDokumentResponse =
                            objectMapper.convertValue<ArkiverDokumentResponse>(it.body?.data, ArkiverDokumentResponse::class.java)
                    Assert.isTrue(arkiverDokumentResponse.ferdigstilt,
                                  "Klarte ikke ferdigstille journalpost med id ${arkiverDokumentResponse.journalpostId}")
                    arkiverDokumentResponse.journalpostId
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved lager journalpost.", it, uri, fnr)
                }
        )
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgave): String {
        val uri = URI.create("$integrasjonerServiceUri/oppgave/")

        return Result.runCatching {
            sendOppgave(uri, opprettOppgave)
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    it.body?.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                       null,
                                                                                       uri,
                                                                                       opprettOppgave.ident.ident)
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved opprett oppgave.",
                                               it,
                                               uri,
                                               opprettOppgave.ident.ident)
                }
        )
    }

    private fun sendJournalFørRequest(journalFørEndpoint: URI,
                                      arkiverDokumentRequest: ArkiverDokumentRequest)
            : ResponseEntity<Ressurs<ArkiverDokumentResponse>> {
        val headers = HttpHeaders()
        headers.add("Content-Type", "application/json;charset=UTF-8")
        headers.acceptCharset = listOf(Charsets.UTF_8)
        return restOperations.exchange(journalFørEndpoint, HttpMethod.POST, HttpEntity<Any>(arkiverDokumentRequest, headers))
    }

    private fun sendOppgave(journalFørEndpoint: URI,
                            opprettOppgave: OpprettOppgave)
            : ResponseEntity<Ressurs<OppgaveResponse>> {
        val headers = HttpHeaders()
        headers.add("Content-Type", "application/json;charset=UTF-8")
        headers.acceptCharset = listOf(Charsets.UTF_8)
        return restOperations.exchange(journalFørEndpoint, HttpMethod.POST, HttpEntity<Any>(opprettOppgave, headers))
    }


    private fun sendDistribusjonRequest(uri: URI,
                                        distribuerJournalpostRequest: DistribuerJournalpostRequest): ResponseEntity<Ressurs<String>> {
        return restOperations.exchange(post(uri)
                                               .acceptCharset(Charsets.UTF_8)
                                               .header("Content-Type", "application/json;charset=UTF-8")
                                               .body(distribuerJournalpostRequest))
    }

    private inline fun <reified T> assertGenerelleSuksessKriterier(it: ResponseEntity<Ressurs<T>>) {
        Assert.notNull(it.body, "Finner ikke ressurs")
        Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS,
                      "Ressurs returnerer ${it.body?.status} men har http status kode ${it.statusCode}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonTjeneste::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        const val VEDTAK_DOKUMENT_TYPE = "BARNETRYGD_VEDTAK"

        private const val OAUTH2_CLIENT_CONFIG_KEY = "familie-integrasjoner-clientcredentials"
    }
}
