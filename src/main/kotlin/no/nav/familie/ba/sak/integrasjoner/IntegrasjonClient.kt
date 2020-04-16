package no.nav.familie.ba.sak.integrasjoner

import medAktørId
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.domene.Journalpost
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.journalføring.domene.Journalpost
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.ba.sak.oppgave.domene.OppgaveDto
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.arkivering.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.arkivering.Dokument
import no.nav.familie.kontrakter.felles.arkivering.FilType
import no.nav.familie.kontrakter.felles.distribusjon.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgave
import no.nav.familie.log.NavHttpHeaders
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonClient(@Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
                        @Qualifier("jwtBearer") restOperations: RestOperations)
    : AbstractRestClient(restOperations, "integrasjon") {

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentAktørId(personident: String): AktørId {
        if (personident.isEmpty()) {
            throw IntegrasjonException("Ved henting av aktør id er personident null eller tom")
        }
        val uri = URI.create("$integrasjonUri/aktoer/v1")
        logger.info("Henter aktørId fra $uri")
        return try {
            val response = getForEntity<Ressurs<MutableMap<*, *>>>(uri, HttpHeaders().medPersonident(personident))
            secureLogger.info("Vekslet inn fnr: {} til aktørId: {}", personident, response)
            val aktørId = response.data?.get("aktørId").toString()
            if (aktørId.isEmpty()) {
                throw IntegrasjonException(msg = "Kan ikke finne aktørId for ident", ident = personident)
            } else {
                AktørId(aktørId)
            }
        } catch (e: RestClientException) {
            throw IntegrasjonException("Ukjent feil ved henting av aktørId", e, uri, personident)
        }
    }

    fun hentPersonIdent(aktørId: String?): PersonIdent? {
        if (aktørId == null || aktørId.isEmpty()) {
            throw IntegrasjonException("Ved henting av personident er aktørId null eller tom")
        }
        val uri = URI.create("$integrasjonUri/aktoer/v1/fraaktorid")
        log.info("Henter fnr fra $uri")

        return try {
            val response: Ressurs<Map<*, *>> = getForEntity(uri, HttpHeaders().medAktørId(aktørId))
            assertGenerelleSuksessKriterier(response)

            secureLogger.info("Vekslet inn aktørId: {} til fnr: {}",
                              aktørId,
                              response.data!!["personIdent"])
            val personIdent = response.data!!["personIdent"].toString()
            if (personIdent.isEmpty()) {
                throw IntegrasjonException("Personident fra integrasjonstjenesten er tom")
            } else {
                PersonIdent(personIdent)
            }
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av personIdent", e, uri, aktørId)
        }
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentPersoninfoFor(personIdent: String): Personinfo {
        logger.info("Henter personinfo fra $integrasjonUri")

        val uri = URI.create("$integrasjonUri/personopplysning/v1/info/BAR")

        return try {
            val response = getForEntity<Ressurs<Personinfo>>(uri, HttpHeaders().medPersonident(personIdent))
            assertGenerelleSuksessKriterier(response)

            secureLogger.info("Personinfo fra $uri for {}: {}", personIdent, response.data)
            response.data!!
        }catch(e: HttpClientErrorException){
            if(e.statusCode === HttpStatus.NOT_FOUND){
                throw e;
            }else{
                throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av personinfo", e, uri, personIdent);
            }
        }catch (e: Exception) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av personinfo", e, uri, personIdent)
        }
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentBehandlendeEnhet(geografiskTilknytning: String?, diskresjonskode: String?): List<Arbeidsfordelingsenhet> {
        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
                .pathSegment("arbeidsfordeling", "enhet")
                .queryParam("tema", "BAR")
                .queryParam("geografi", geografiskTilknytning)
                .queryParam("diskresjonskode", diskresjonskode)
                .build().toUri()

        return try {
            val response = getForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(uri)
            response.data ?: throw IntegrasjonException("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt",
                                                        null,
                                                        uri)
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet", e, uri)
        }
    }

    fun distribuerVedtaksbrev(journalpostId: String) {
        val uri = URI.create("$integrasjonUri/dist/v1")
        logger.info("Kaller dokdist-tjeneste med journalpostId $journalpostId")

        Result.runCatching {
            val journalpostRequest = DistribuerJournalpostRequest(
                    journalpostId, "BA", "FAMILIE_BA_SAK")
            postForEntity<Ressurs<String>>(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    if (it?.data?.isBlank() != false) error("BestillingsId fra integrasjonstjenesten mot dokdist er tom")
                    logger.info("Distribusjon av vedtaksbrev bestilt. BestillingsId:  $it")
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved distribusjon av vedtaksbrev", it, uri, "")
                }
        )
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/ferdigstill")

        Result.runCatching {
            val response = patchForEntity<Ressurs<String>>(uri, "")
            assertGenerelleSuksessKriterier(response)
        }.onFailure {
            val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
            throw IntegrasjonException("Kan ikke ferdigstille $oppgaveId. response=$message", it, uri)
        }
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgave): String {
        val uri = URI.create("$integrasjonUri/oppgave/")

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgave, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    it?.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                  null,
                                                                                  uri,
                                                                                  opprettOppgave.ident.ident)
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved opprett oppgave. response=$message",
                                               it,
                                               uri,
                                               opprettOppgave.ident.ident)
                }
        )
    }

    fun finnOppgaveMedId(oppgaveId: Long): Ressurs<OppgaveDto> {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId")

        return Result.runCatching {
            getForEntity<Ressurs<OppgaveDto>>(uri)
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    it
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved henting av oppgave med id $oppgaveId. response=$message",
                                               it,
                                               uri)
                }
        )
    }

    fun hentJournalpost(journalpostId: String): Ressurs<Journalpost> {
        val uri = URI.create("$integrasjonUri/journalpost?journalpostId=$journalpostId")
        logger.debug("henter journalpost med id {}", journalpostId)

        return Result.runCatching {
            getForEntity<Ressurs<Journalpost>>(uri)
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    it
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Henting av journalpost med id $journalpostId feilet. response=$message",
                                               it,
                                               uri)
                }
        )
    }

    fun finnOppgaverKnyttetTilSaksbehandlerOgEnhet(behandlingstema: String?,
                                                   oppgavetype: String?,
                                                   enhet: String?,
                                                   saksbehandler: String?): List<OppgaveDto> {

        val uriBuilder = UriComponentsBuilder.fromUriString("$integrasjonUri/oppgave")

        uriBuilder.queryParam("tema", "BAR")
        behandlingstema?.apply { uriBuilder.queryParam("behandlingstema", this) }
        oppgavetype?.apply { uriBuilder.queryParam("oppgavetype", this) }
        enhet?.apply { uriBuilder.queryParam("enhet", this) }
        saksbehandler?.apply { uriBuilder.queryParam("saksbehandler", this) }

        val uri = uriBuilder.build().toUri()

        return try {
            val ressurs = getForEntity<Ressurs<List<OppgaveDto>>>(uri, HttpHeaders().medContentTypeJsonUTF8())
            assertGenerelleSuksessKriterier(ressurs)
            ressurs.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
        } catch (e: Exception) {
            val message = if (e is RestClientResponseException) e.responseBodyAsString else ""
            throw IntegrasjonException("Kall mot integrasjon feilet ved finnOppgaverKnyttetTilSaksbehandlerOgEnhet. response=$message",
                                       e,
                                       uri,
                                       "behandlingstema: ${behandlingstema}, oppgavetype: ${oppgavetype}, enhet: ${enhet}, saksbehandler: ${saksbehandler}")
        }
    }

    fun ferdigstillJournalpost(journalpostId: String, behandlendeEnhet: String) {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$behandlendeEnhet")

        Result.runCatching {
            execute{ putForEntity<Ressurs<Any>>(uri, "") }
        }.onFailure {
            val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
            throw IntegrasjonException("Kall mot integrasjon feilet ved ferdigstillJournalpost. response=$message", it, uri)
        }
    }

    fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")
        return try {
            val ressurs = putForEntity<Ressurs<OppdaterJournalpostResponse>>(uri, request)
            assertGenerelleSuksessKriterier(ressurs)
            ressurs?.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
        } catch (e: Exception) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved oppdaterJournalpost", e, uri, request.bruker?.id)
        }
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        val uri = URI.create("$integrasjonUri/journalpost?journalpostId=$journalpostId")
        return try {
            val ressurs = getForEntity<Ressurs<Journalpost>>(uri)
            assertGenerelleSuksessKriterier(ressurs)
            ressurs.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
        } catch (e: Exception) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved hentJournalpost", e, uri, null)
        }
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentInfoId")
        return try {
            val ressurs = getForEntity<Ressurs<ByteArray>>(uri)
            assertGenerelleSuksessKriterier(ressurs)
            ressurs.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
        } catch (e: Exception) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved hentDokument", e, uri, null)
        }
    }


    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun journalFørVedtaksbrev(fnr: String, fagsakId: String, pdf: ByteArray): String {
        return lagJournalpostForVedtaksbrev(fnr, fagsakId, pdf)
    }

    fun lagJournalpostForVedtaksbrev(fnr: String, fagsakId: String, pdfByteArray: ByteArray): String {
        val uri = URI.create("$integrasjonUri/arkiv/v2")
        logger.info("Sender vedtak pdf til DokArkiv: $uri")

        return Result.runCatching {
            val dokumenter = listOf(Dokument(pdfByteArray, FilType.PDFA, dokumentType = VEDTAK_DOKUMENT_TYPE))
            val arkiverDokumentRequest = ArkiverDokumentRequest(fnr, true, dokumenter, fagsakId, "9999")
            val arkiverDokumentResponse = postForEntity<Ressurs<ArkiverDokumentResponse>>(uri, arkiverDokumentRequest)
            arkiverDokumentResponse
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    val arkiverDokumentResponse = it?.data ?: error("Ressurs mangler data")
                    if (!arkiverDokumentResponse.ferdigstilt) {
                        error("Klarte ikke ferdigstille journalpost med id ${arkiverDokumentResponse.journalpostId}")
                    }
                    arkiverDokumentResponse.journalpostId
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved lag journalpost.", it, uri, fnr)
                }
        )
    }

    private fun <T> execute(xForEntity: () -> Ressurs<T>?): T? {
        val response = xForEntity.invoke()
        return validerOgPakkUt(response)
    }

    private fun <T> validerOgPakkUt(ressurs: Ressurs<T>?): T {
        when {
            ressurs == null -> error("Finner ikke ressurs")
            ressurs.data == null -> error("Ressurs mangler data")
            ressurs.status != Ressurs.Status.SUKSESS -> error("Ressurs returnerer 2xx men har ressurs status failure")
            else -> return ressurs.data!!
        }
    }

    val tilgangUri = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANGER).build().toUri()

    fun sjekkTilgangTilPersoner(personer: Set<Person>): List<Tilgang> {
        return sjekkTilgangTilPersoner(
                personer.map { it.personIdent.ident }
        )
    }

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        return postForEntity(tilgangUri, personIdenter)!!
    }

    private fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
        this.add("Content-Type", "application/json;charset=UTF-8")
        this.acceptCharset = listOf(Charsets.UTF_8)
        return this
    }

    private fun HttpHeaders.medPersonident(personident: String): HttpHeaders {
        this.add(NavHttpHeaders.NAV_PERSONIDENT.asString(), personident)
        return this
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        const val VEDTAK_DOKUMENT_TYPE = "BARNETRYGD_VEDTAK"
        private const val PATH_TILGANGER = "tilgang/personer"
    }
}