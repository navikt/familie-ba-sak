package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.MDCOperations
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsforholdRequest
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Skyggesak
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.tilAdressebeskyttelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.brev.hentOverstyrtDokumenttittel
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

const val DEFAULT_JOURNALFØRENDE_ENHET = "9999"

@Component
class IntegrasjonClient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient
) :
    AbstractRestClient(restOperations, "integrasjon") {

    @Cacheable("alle-eøs-land", cacheManager = "kodeverkCache")
    fun hentAlleEØSLand(): KodeverkDto {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/eea")

        return try {
            getForEntity<Ressurs<KodeverkDto>>(uri).getDataOrThrow()
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av landkoder eea", e, uri)
        }
    }

    @Cacheable("land", cacheManager = "kodeverkCache")
    fun hentLand(landkode: String): String {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/$landkode")

        return try {
            getForEntity<Ressurs<String>>(uri).getDataOrThrow()
        } catch (e: RestClientException) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved uthenting av land", e, uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    @Cacheable("behandlendeEnhet", cacheManager = "kodeverkCache")
    fun hentBehandlendeEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
            .pathSegment("arbeidsfordeling", "enhet", "BAR")
            .build().toUri()

        return Result.runCatching {
            val response =
                postForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, mapOf("ident" to ident))
            response.data ?: throw IntegrasjonException(
                "Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt",
                null,
                uri
            )
        }.fold(
            onSuccess = { it },
            onFailure = {
                throw IntegrasjonException(
                    "Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet",
                    it,
                    uri
                )
            }
        )
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentArbeidsforhold(ident: String, ansettelsesperiodeFom: LocalDate): List<Arbeidsforhold> {

        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
            .pathSegment("aareg", "arbeidsforhold")
            .build().toUri()

        return try {
            val response =
                postForEntity<Ressurs<List<Arbeidsforhold>>>(uri, ArbeidsforholdRequest(ident, ansettelsesperiodeFom))
            response.getDataOrThrow()
        } catch (e: Exception) {
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
                journalpostId, Fagsystem.BA, "FAMILIE_BA_SAK"
            )
            postForEntity<Ressurs<String>>(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8()).also {
                assertGenerelleSuksessKriterier(it)
            }
        }.fold(
            onSuccess = {
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

    @Cacheable("enhet", cacheManager = "kodeverkCache")
    fun hentEnhet(enhetId: String?): NavKontorEnhet {
        val uri = URI.create("$integrasjonUri/arbeidsfordeling/nav-kontor/$enhetId")

        return try {
            getForEntity<Ressurs<NavKontorEnhet>>(uri).getDataOrThrow()
        } catch (e: Exception) {
            throw IntegrasjonException("Kall mot integrasjon feilet ved henting av enhet.", e)
        }
    }

    private fun responseBody(it: Throwable): String {
        return if (it is RestClientResponseException) it.responseBodyAsString else ""
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): String {
        val uri = URI.create("$integrasjonUri/oppgave/opprett")

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(
                uri,
                opprettOppgave,
                HttpHeaders().medContentTypeJsonUTF8()
            ).also {
                assertGenerelleSuksessKriterier(it)
            }
        }.fold(
            onSuccess = {
                it.data?.oppgaveId?.toString() ?: throw IntegrasjonException(
                    "Response fra oppgave mangler oppgaveId.",
                    null,
                    uri,
                    opprettOppgave.ident?.ident
                )
            },
            onFailure = {
                val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                throw IntegrasjonException(
                    "Kall mot integrasjon feilet ved opprett oppgave. response=$message",
                    it,
                    uri,
                    opprettOppgave.ident?.ident
                )
            }
        )
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/${patchOppgave.id}/oppdater")

        return Result.runCatching {
            patchForEntity<Ressurs<OppgaveResponse>>(
                uri,
                patchOppgave,
                HttpHeaders().medContentTypeJsonUTF8()
            ).also {
                assertGenerelleSuksessKriterier(it)
            }
        }.fold(
            onSuccess = {
                it.getDataOrThrow()
            },
            onFailure = {
                val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                throw IntegrasjonException(
                    "Kall mot integrasjon feilet ved oppdater oppgave. response=$message",
                    it,
                    uri,
                    patchOppgave.identer?.find { ident ->
                        ident.gruppe == IdentGruppe.FOLKEREGISTERIDENT
                    }?.ident
                )
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
            postForEntity<Ressurs<OppgaveResponse>>(
                uri,
                HttpHeaders().medContentTypeJsonUTF8()
            ).also {
                assertGenerelleSuksessKriterier(it)
            }
        }.fold(
            onSuccess = {
                it.data?.oppgaveId?.toString() ?: throw IntegrasjonException(
                    "Response fra oppgave mangler oppgaveId.",
                    null,
                    uri
                )
            },
            onFailure = {
                val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                throw IntegrasjonException(
                    "Kall mot integrasjon feilet ved fordel oppgave. response=$message",
                    it,
                    uri
                )
            }
        )
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId")

        return Result.runCatching {
            getForEntity<Ressurs<Oppgave>>(uri).also { assertGenerelleSuksessKriterier(it) }
        }.fold(
            onSuccess = {
                secureLogger.info("Oppgave fra $uri med id $oppgaveId inneholder: ${it.data}")
                it.data!!
            },
            onFailure = {
                val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                throw IntegrasjonException(
                    "Kall mot integrasjon feilet ved henting av oppgave med id $oppgaveId. response=$message",
                    it,
                    uri,
                )
            }
        )
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentJournalpost(journalpostId: String): Ressurs<Journalpost> {
        val uri = URI.create("$integrasjonUri/journalpost?journalpostId=$journalpostId")
        logger.debug("henter journalpost med id {}", journalpostId)

        return Result.runCatching {
            getForEntity<Ressurs<Journalpost>>(uri).also { assertGenerelleSuksessKriterier(it) }
        }.fold(
            onSuccess = { it },
            onFailure = {
                val message = if (it is RestClientResponseException) it.responseBodyAsString else ""

                if (it is HttpClientErrorException.Forbidden) {
                    val defaultIkkeTilgangMelding = "Bruker eller system har ikke tilgang til saf ressurs"
                    logger.warn(it.message ?: defaultIkkeTilgangMelding)

                    return Ressurs.ikkeTilgang(defaultIkkeTilgangMelding)
                }

                throw IntegrasjonException(
                    "Henting av journalpost med id $journalpostId feilet. response=$message",
                    it,
                    uri
                )
            }
        )
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): Ressurs<List<Journalpost>> {
        val uri = URI.create("$integrasjonUri/journalpost")
        secureLogger.info(
            "henter journalposter for bruker med ident ${journalposterForBrukerRequest.brukerId} " +
                "og data $journalposterForBrukerRequest"
        )

        try {
            return postForEntity<Ressurs<List<Journalpost>>>(uri, journalposterForBrukerRequest)
                .also { assertGenerelleSuksessKriterier(it) }
        } catch (exception: Exception) {
            val message = if (exception is RestClientResponseException) exception.responseBodyAsString else ""
            throw IntegrasjonException("Henting av journalposter for bruker feilet. response=$message", exception, uri)
        }
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return finnOppgaveRequest.run {
            val uri = URI.create("$integrasjonUri/oppgave/v4")

            try {
                val ressurs =
                    postForEntity<Ressurs<FinnOppgaveResponseDto>>(
                        uri,
                        finnOppgaveRequest,
                        HttpHeaders().medContentTypeJsonUTF8()
                    )
                assertGenerelleSuksessKriterier(ressurs)
                ressurs.data ?: throw IntegrasjonException("Ressurs mangler.", null, uri, null)
            } catch (exception: Exception) {
                val message = if (exception is RestClientResponseException) exception.responseBodyAsString else ""
                throw IntegrasjonException(
                    "Kall mot integrasjon feilet ved hentOppgaver. response=$message",
                    exception,
                    uri,
                    "behandlingstema: $behandlingstema, oppgavetype: $oppgavetype, enhet: $enhet, " +
                        "saksbehandler: $saksbehandler",
                )
            }
        }
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String) {
        val uri =
            URI.create("$integrasjonUri/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet")
        exchange(
            networkRequest = {
                putForEntity<Ressurs<Any>>(uri, "")
            },
            onFailure = {
                IntegrasjonException(
                    "Kall mot integrasjon feilet ved ferdigstillJournalpost. response=${responseBody(it)}",
                    it,
                    uri
                )
            }
        )
    }

    fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")
        return exchange(
            networkRequest = {
                putForEntity(uri, request)
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
                postForEntity(uri, request)
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
                deleteForEntity(uri)
            },
            onFailure = {
                IntegrasjonException("Kall mot integrasjon feilet ved slettLogiskVedlegg", it, uri, null)
            }
        )
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): Ressurs<ByteArray> {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentInfoId")

        return getForEntity(uri)
    }

    fun journalførVedtaksbrev(fnr: String, fagsakId: String, vedtak: Vedtak, journalførendeEnhet: String): String {
        val vedleggPdf =
            hentVedlegg(VEDTAK_VEDLEGG_FILNAVN) ?: error("Klarte ikke hente vedlegg $VEDTAK_VEDLEGG_FILNAVN")

        val brev = listOf(
            Dokument(
                vedtak.stønadBrevPdF!!,
                filtype = Filtype.PDFA,
                dokumenttype = vedtak.behandling.resultat.tilDokumenttype(),
                tittel = hentOverstyrtDokumenttittel(vedtak.behandling)
            )
        )
        logger.info(
            "Journalfører vedtaksbrev for behandling ${vedtak.behandling.id} med tittel ${
                hentOverstyrtDokumenttittel(vedtak.behandling)
            }"
        )
        val vedlegg = listOf(
            Dokument(
                vedleggPdf, filtype = Filtype.PDFA,
                dokumenttype = Dokumenttype.BARNETRYGD_VEDLEGG,
                tittel = VEDTAK_VEDLEGG_TITTEL
            )
        )
        return lagJournalpostForBrev(
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = brev,
            vedlegg = vedlegg,
            behandlingId = vedtak.behandling.id
        )
    }

    fun journalførManueltBrev(
        fnr: String,
        fagsakId: String,
        journalførendeEnhet: String,
        brev: ByteArray,
        dokumenttype: Dokumenttype,
        førsteside: Førsteside?
    ): String {
        return lagJournalpostForBrev(
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = listOf(
                Dokument(
                    dokument = brev,
                    filtype = Filtype.PDFA,
                    dokumenttype = dokumenttype
                )
            ),
            førsteside = førsteside,
        )
    }

    fun lagJournalpostForBrev(
        fnr: String,
        fagsakId: String,
        journalførendeEnhet: String? = null,
        brev: List<Dokument>,
        vedlegg: List<Dokument> = emptyList(),
        førsteside: Førsteside? = null,
        behandlingId: Long? = null
    ): String {
        val uri = URI.create("$integrasjonUri/arkiv/v4")
        logger.info("Sender pdf til DokArkiv: $uri")

        if (journalførendeEnhet == DEFAULT_JOURNALFØRENDE_ENHET) {
            logger.warn("Informasjon om enhet mangler på bruker og er satt til fallback-verdi, $DEFAULT_JOURNALFØRENDE_ENHET")
        }

        return Result.runCatching {
            val arkiverDokumentRequest = ArkiverDokumentRequest(
                fnr = fnr,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = brev,
                vedleggsdokumenter = vedlegg,
                fagsakId = fagsakId,
                journalførendeEnhet = journalførendeEnhet,
                førsteside = førsteside,
                eksternReferanseId = "${fagsakId}_${behandlingId}_${MDCOperations.getCallId()}"
            )

            postForEntity<Ressurs<ArkiverDokumentResponse>>(uri, arkiverDokumentRequest).also {
                assertGenerelleSuksessKriterier(it)
            }
        }.fold(
            onSuccess = {
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

    fun opprettSkyggesak(aktør: Aktør, fagsakId: Long) {
        val uri = URI.create("$integrasjonUri/skyggesak/v1")

        try {
            postForEntity<Ressurs<Unit>>(uri, Skyggesak(aktør.aktørId, fagsakId.toString()))
        } catch (e: Exception) {
            throw IntegrasjonException(
                "Kall mot integrasjon feilet ved oppretting av skyggesak i Sak for fagsak=$fagsakId",
                e,
                uri
            )
        }
    }

    val tilgangRelasjonerUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_RELASJONER).build().toUri()
    val tilgangPersonUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_PERSON).build().toUri()

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Tilgang {
        if (SikkerhetContext.erSystemKontekst()) {
            return Tilgang(true, null)
        }

        val tilganger = postForEntity<List<Tilgang>>(
            tilgangPersonUri,
            personIdenter,
            HttpHeaders().also {
                it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_BAR)
            }
        )

        return tilganger.firstOrNull { !it.harTilgang } ?: tilganger.first()
    }

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        if (SikkerhetContext.erSystemKontekst()) {
            return Tilgang(true, null)
        }

        return postForEntity(
            tilgangRelasjonerUri, PersonIdent(personIdent),
            HttpHeaders().also {
                it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_BAR)
            }
        )
    }

    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): RestPersonInfo? {
        val harTilgang = sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = systemOnlyPdlRestClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            RestPersonInfo(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false
            )
        } else null
    }

    private inline fun <reified T> exchange(
        networkRequest: () -> Ressurs<T>?,
        onFailure: (Throwable) -> RuntimeException
    ): T {
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

    companion object {

        private val logger = LoggerFactory.getLogger(IntegrasjonClient::class.java)
        const val VEDTAK_VEDLEGG_FILNAVN = "NAV_33-0005bm-10.2016.pdf"
        const val VEDTAK_VEDLEGG_TITTEL = "Stønadsmottakerens rettigheter og plikter (Barnetrygd)"
        private const val PATH_TILGANG_RELASJONER = "tilgang/person-med-relasjoner"
        private const val PATH_TILGANG_PERSON = "tilgang/v2/personer"
        private const val HEADER_NAV_TEMA = "Nav-Tema"
        private val HEADER_NAV_TEMA_BAR = Tema.BAR.name

        fun hentVedlegg(vedleggsnavn: String): ByteArray? {
            val inputStream = this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
            return inputStream?.readAllBytes()
        }
    }
}

fun BehandlingResultat.tilDokumenttype() = when (this) {
    BehandlingResultat.AVSLÅTT -> Dokumenttype.BARNETRYGD_VEDTAK_AVSLAG
    BehandlingResultat.OPPHØRT -> Dokumenttype.BARNETRYGD_OPPHØR
    else -> Dokumenttype.BARNETRYGD_VEDTAK_INNVILGELSE
}
