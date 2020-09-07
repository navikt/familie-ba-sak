package no.nav.familie.ba.sak.integrasjoner

import medAktørId
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.dokument.DokumentController.BrevType
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.domene.ArbeidsforholdRequest
import no.nav.familie.ba.sak.integrasjoner.domene.Tilgang
import no.nav.familie.ba.sak.journalføring.domene.*
import no.nav.familie.ba.sak.oppgave.FinnOppgaveRequest
import no.nav.familie.ba.sak.oppgave.OppgaverOgAntall
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.log.NavHttpHeaders
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class IntegrasjonClient(@Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
                        @Qualifier("jwtBearer") restOperations: RestOperations,
                        private val environment: Environment)
    : AbstractRestClient(restOperations, "integrasjon") {

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

    fun hentAlleEØSLand(): KodeverkDto {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/eea")

        return try {
            getForEntity<Ressurs<KodeverkDto>>(uri).getDataOrThrow()
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av landkoder eea", e, uri)
        }
    }

    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentBehandlendeEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        if (environment.activeProfiles.contains("e2e")) {
            return listOf(Arbeidsfordelingsenhet("2970", "enhetsNavn"))
        }
        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
                .pathSegment("arbeidsfordeling", "enhet", "BAR")
                .build().toUri()

        return try {
            val response =
                    getForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, httpHeaders = HttpHeaders().medPersonident(ident))
            response.data ?: throw IntegrasjonException("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt",
                                                        null,
                                                        uri)
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet", e, uri)
        }
    }


    @Retryable(value = [IntegrasjonException::class], maxAttempts = 3, backoff = Backoff(delay = 5000))
    fun hentArbeidsforhold(ident: String, ansettelsesperiodeFom: LocalDate): List<Arbeidsforhold> {

        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
                .pathSegment("aareg", "arbeidsforhold")
                .build().toUri()

        return try {
            val response = postForEntity<Ressurs<List<Arbeidsforhold>>>(uri, ArbeidsforholdRequest(ident, ansettelsesperiodeFom))
            response.getDataOrThrow()
        } catch (e: RestClientException) {
            var feilmelding = "Kall mot integrasjon feilet ved henting av arbeidsforhold."
            if (e is HttpStatusCodeException && e.responseBodyAsString.isNotEmpty()) {
                feilmelding += " body=${e.responseBodyAsString}"
            }
            throw IntegrasjonException(feilmelding, e, uri, ident)
        }
    }

    fun distribuerBrev(journalpostId: String): Ressurs<String> {
        val uri = URI.create("$integrasjonUri/dist/v1")
        logger.info("Kaller dokdist-tjeneste med journalpostId $journalpostId")

        Result.runCatching {
            val journalpostRequest = DistribuerJournalpostRequest(
                    journalpostId, "BA", "FAMILIE_BA_SAK")
            postForEntity<Ressurs<String>>(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    if (it.getDataOrThrow().isBlank()) error("BestillingsId fra integrasjonstjenesten mot dokdist er tom")
                    else {
                        logger.info("Distribusjon av brev bestilt")
                        secureLogger.info("Distribusjon av brev bestilt med data i responsen: ${it.data}")
                        return it
                    }
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved distribusjon av brev", it, uri, "")
                }
        )
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/ferdigstill")

        Result.runCatching {
            val response = patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
            assertGenerelleSuksessKriterier(response)
        }.onFailure {
            throw IntegrasjonException("Kan ikke ferdigstille $oppgaveId. response=${responseBody(it)}", it, uri)
        }
    }

    private fun responseBody(it: Throwable): String? {
        return if (it is RestClientResponseException) it.responseBodyAsString else ""
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): String {
        val uri = URI.create("$integrasjonUri/oppgave/v2")

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgave, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    it.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                 null,
                                                                                 uri,
                                                                                 opprettOppgave.ident?.ident)
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved opprett oppgave. response=$message",
                                               it,
                                               uri,
                                               opprettOppgave.ident?.ident)
                }
        )
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): String {
        val baseUri = URI.create("$integrasjonUri/oppgave/$oppgaveId/fordel")
        val uri = if (saksbehandler == null)
            baseUri
        else
            UriComponentsBuilder.fromUri(baseUri).queryParam("saksbehandler", saksbehandler).build().toUri()

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(uri, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    it.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                 null,
                                                                                 uri
                    )
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave. response=$message",
                                               it,
                                               uri
                    )
                }
        )
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId")

        return Result.runCatching {
            getForEntity<Ressurs<Oppgave>>(uri)
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    secureLogger.info("Oppgave fra $uri med id $oppgaveId inneholder: ${it.data}")
                    it.data!!
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

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): OppgaverOgAntall {
        return finnOppgaveRequest.run {
            val uri = URI.create("$integrasjonUri/oppgave/v2")

            try {
                val ressurs =
                        postForEntity<Ressurs<OppgaverOgAntall>>(uri, finnOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
                assertGenerelleSuksessKriterier(ressurs)
                ressurs.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
            } catch (e: Exception) {
                val message = if (e is RestClientResponseException) e.responseBodyAsString else ""
                throw IntegrasjonException("Kall mot integrasjon feilet ved hentOppgaver. response=$message",
                                           e,
                                           uri,
                                           "behandlingstema: ${behandlingstema}, oppgavetype: ${oppgavetype}, enhet: ${enhet}, saksbehandler: ${saksbehandler}")
            }
        }
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String) {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet")
        exchange(
                networkRequest = {
                    putForEntity<Ressurs<Any>>(uri, "")
                },
                onFailure = {
                    IntegrasjonException("Kall mot integrasjon feilet ved ferdigstillJournalpost. response=${responseBody(it)}",
                                         it,
                                         uri)
                }
        )
    }

    fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")
        return exchange(
                networkRequest = {
                    putForEntity<Ressurs<OppdaterJournalpostResponse>>(uri, request)
                },
                onFailure = {
                    IntegrasjonException("Kall mot integrasjon feilet ved oppdaterJournalpost", it, uri, request.bruker.id)
                }
        )
    }

    fun leggTilLogiskVedlegg(request: LogiskVedleggRequest, dokumentinfoId: String): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg")
        return exchange(
                networkRequest = {
                    postForEntity<Ressurs<LogiskVedleggResponse>>(uri, request)
                },
                onFailure = {
                    IntegrasjonException("Kall mot integrasjon feilet ved leggTilLogiskVedlegg", it, uri, null)
                }
        )
    }

    fun slettLogiskVedlegg(logiskVedleggId: String, dokumentinfoId: String): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg/$logiskVedleggId")
        return exchange(
                networkRequest = {
                    deleteForEntity<Ressurs<LogiskVedleggResponse>>(uri)
                },
                onFailure = {
                    IntegrasjonException("Kall mot integrasjon feilet ved slettLogiskVedlegg", it, uri, null)
                }
        )
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentInfoId")
        return exchange(
                networkRequest = {
                    getForEntity<Ressurs<ByteArray>>(uri)
                },
                onFailure = {
                    throw IntegrasjonException("Kall mot integrasjon feilet ved hentDokument", it, uri, null)
                }
        )
    }

    fun journalFørVedtaksbrev(fnr: String, fagsakId: String, vedtak: Vedtak): String {
        if (vedtak.ansvarligEnhet == "9999") {
            logger.error("Informasjon om enhet mangler på bruker og er satt til fallback-verdi, 9999")
        }
        val vedleggPdf = hentVedlegg(VEDTAK_VEDLEGG_FILNAVN) ?: error("Klarte ikke hente vedlegg $VEDTAK_VEDLEGG_FILNAVN")
        val brev = listOf(Dokument(vedtak.stønadBrevPdF!!, FilType.PDFA, dokumentType = BrevType.VEDTAK.arkivType))
        val vedlegg = listOf(Dokument(vedleggPdf, FilType.PDFA,
                                      dokumentType = VEDLEGG_DOKUMENT_TYPE,
                                      tittel = VEDTAK_VEDLEGG_TITTEL))
        return lagJournalpostForBrev(fnr, fagsakId, vedtak.ansvarligEnhet, brev, vedlegg)
    }

    fun journalførManueltBrev(fnr: String, fagsakId: String, journalførendeEnhet: String, brev: ByteArray, brevType: String): String {
        return lagJournalpostForBrev(fnr = fnr,
                                     fagsakId = fagsakId,
                                     journalførendeEnhet = journalførendeEnhet,
                                     brev = listOf(Dokument(brev, FilType.PDFA, dokumentType = brevType)))
    }

    fun lagJournalpostForBrev(fnr: String,
                              fagsakId: String,
                              journalførendeEnhet: String? = null,
                              brev: List<Dokument>,
                              vedlegg: List<Dokument> = emptyList()): String {
        val uri = URI.create("$integrasjonUri/arkiv/v3")
        logger.info("Sender vedtak pdf til DokArkiv: $uri")

        return Result.runCatching {
            val arkiverDokumentRequest = ArkiverDokumentRequest(fnr = fnr,
                                                                forsøkFerdigstill = true,
                                                                hoveddokumentvarianter = brev,
                                                                vedleggsdokumenter = vedlegg,
                                                                fagsakId = fagsakId,
                                                                journalførendeEnhet = journalførendeEnhet)
            val arkiverDokumentResponse = postForEntity<Ressurs<ArkiverDokumentResponse>>(uri, arkiverDokumentRequest)
            arkiverDokumentResponse
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)
                    val arkiverDokumentResponse = it.data ?: error("Ressurs mangler data")
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

    private inline fun <reified T> exchange(networkRequest: () -> Ressurs<T>?, onFailure: (Throwable) -> RuntimeException): T {
        return try {
            val response = networkRequest.invoke()
            validerOgPakkUt(response)
        } catch (e: Exception) {
            throw onFailure(e)
        }
    }

    private inline fun <reified T> validerOgPakkUt(ressurs: Ressurs<T>?): T {
        assertGenerelleSuksessKriterier(ressurs)
        return ressurs!!.data ?: error("Henting av ressurs feilet med status ${ressurs.status} i response")
    }

    val tilgangUri = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANGER).build().toUri()

    fun sjekkTilgangTilPersoner(personer: Set<Person>): List<Tilgang> {
        return sjekkTilgangTilPersoner(
                personer.map { it.personIdent.ident }
        )
    }

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        return postForEntity(tilgangUri, personIdenter)
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
        const val VEDLEGG_DOKUMENT_TYPE = "BARNETRYGD_VEDLEGG"
        const val VEDTAK_VEDLEGG_FILNAVN = "NAV_33-0005bm-10.2016.pdf"
        const val VEDTAK_VEDLEGG_TITTEL = "Stønadsmottakerens rettigheter og plikter (Barnetrygd)"
        private const val PATH_TILGANGER = "tilgang/personer"

        fun hentVedlegg(vedleggsnavn: String): ByteArray? {
            val inputStream = this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
            return inputStream?.readAllBytes()
        }
    }
}