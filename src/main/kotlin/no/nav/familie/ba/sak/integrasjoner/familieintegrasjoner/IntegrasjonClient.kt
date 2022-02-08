package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.MDCOperations
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsforholdRequest
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Skyggesak
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

const val DEFAULT_JOURNALFØRENDE_ENHET = "9999"

@Component
class IntegrasjonClient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations
) :
    AbstractRestClient(restOperations, "integrasjon") {

    @Cacheable("alle-eøs-land", cacheManager = "kodeverkCache")
    fun hentAlleEØSLand(): KodeverkDto {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/eea")

        return kallEksternTjeneste(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent EØS land"
        ) {
            getForEntity(uri)
        }
    }

    @Cacheable("land", cacheManager = "kodeverkCache")
    fun hentLand(landkode: String): String {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/$landkode")

        return kallEksternTjeneste(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent landkoder for $landkode"
        ) {
            getForEntity(uri)
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

        return kallEksternTjeneste(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent behandlende enhet"
        ) {
            postForEntity(uri, mapOf("ident" to ident))
        }
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

        return kallEksternTjeneste(
            tjeneste = "aareg",
            uri = uri,
            formål = "Hent arbeidsforhold"
        ) {
            postForEntity(uri, ArbeidsforholdRequest(ident, ansettelsesperiodeFom))
        }
    }

    fun distribuerBrev(journalpostId: String): String {
        val uri = URI.create("$integrasjonUri/dist/v1")

        return kallEksternTjeneste(
            tjeneste = "dokdist",
            uri = uri,
            formål = "Distribuer brev"
        ) {
            val journalpostRequest = DistribuerJournalpostRequest(
                journalpostId, Fagsystem.BA, "FAMILIE_BA_SAK"
            )
            postForEntity(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8())
        }
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/ferdigstill")

        kallEksternTjeneste(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Ferdigstill oppgave"
        ) {
            patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
        }
    }

    @Cacheable("enhet", cacheManager = "kodeverkCache")
    fun hentEnhet(enhetId: String?): NavKontorEnhet {
        val uri = URI.create("$integrasjonUri/arbeidsfordeling/nav-kontor/$enhetId")

        return kallEksternTjeneste(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent nav kontor for enhet $enhetId"
        ) {
            getForEntity(uri)
        }
    }

    private fun responseBody(it: Throwable): String {
        return if (it is RestClientResponseException) it.responseBodyAsString else ""
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/opprett")

        return kallEksternTjeneste(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Opprett oppgave"
        ) {
            postForEntity(
                uri,
                opprettOppgave,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/${patchOppgave.id}/oppdater")

        return kallEksternTjeneste(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Patch oppgave"
        ) {
            patchForEntity(
                uri,
                patchOppgave,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): OppgaveResponse {
        val baseUri = URI.create("$integrasjonUri/oppgave/$oppgaveId/fordel")
        val uri = if (saksbehandler == null)
            baseUri
        else
            UriComponentsBuilder.fromUri(baseUri).queryParam("saksbehandler", saksbehandler).build().toUri()

        return kallEksternTjeneste(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Fordel oppgave"
        ) {
            postForEntity(
                uri,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId")

        return kallEksternTjeneste(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Finn oppgave med id $oppgaveId"
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentJournalpost(journalpostId: String): Journalpost {
        val uri = URI.create("$integrasjonUri/journalpost?journalpostId=$journalpostId")

        return kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalpost id $journalpostId"
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        val uri = URI.create("$integrasjonUri/journalpost")

        return kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalposter for bruker"
        ) {
            postForEntity(uri, journalposterForBrukerRequest)
        }
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        val uri = URI.create("$integrasjonUri/oppgave/v4")

        return kallEksternTjeneste(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Hent oppgaver"
        ) {
            postForEntity(
                uri,
                finnOppgaveRequest,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String) {
        val uri =
            URI.create("$integrasjonUri/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet")

        kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalposter for bruker"
        ) {
            putForEntity<Ressurs<Any>>(uri, "")
        }
    }

    fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")

        return kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Oppdater journalpost"
        ) {
            putForEntity(uri, request)
        }
    }

    fun leggTilLogiskVedlegg(request: LogiskVedleggRequest, dokumentinfoId: String): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg")

        return kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Legg til logisk vedlegg på dokument $dokumentinfoId"
        ) {
            postForEntity(uri, request)
        }
    }

    fun slettLogiskVedlegg(logiskVedleggId: String, dokumentinfoId: String): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg/$logiskVedleggId")
        return kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Slett logisk vedlegg på dokument $dokumentinfoId"
        ) {
            deleteForEntity(uri)
        }
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentInfoId")

        return kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent dokument $dokumentInfoId"
        ) {
            getForEntity(uri)
        }
    }

    fun journalførManueltBrev(
        fnr: String,
        fagsakId: String,
        journalførendeEnhet: String,
        brev: ByteArray,
        dokumenttype: Dokumenttype,
        førsteside: Førsteside?
    ): String {
        return journalførDokument(
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

    fun journalførDokument(
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
        val journalpost = kallEksternTjeneste(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Journalfør dokument på fagsak $fagsakId",
        ) {
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

            postForEntity<Ressurs<ArkiverDokumentResponse>>(uri, arkiverDokumentRequest)
        }

        if (!journalpost.ferdigstilt) {
            error("Klarte ikke ferdigstille journalpost med id ${journalpost.journalpostId}")
        }
        return journalpost.journalpostId
    }

    fun opprettSkyggesak(aktør: Aktør, fagsakId: Long) {
        val uri = URI.create("$integrasjonUri/skyggesak/v1")

        kallEksternTjeneste<Ressurs<Unit>>(
            tjeneste = "skyggesak",
            uri = uri,
            formål = "Opprett skyggesak på fagsak $fagsakId"
        ) {
            postForEntity(uri, Skyggesak(aktør.aktørId, fagsakId.toString()))
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IntegrasjonClient::class.java)
        const val VEDTAK_VEDLEGG_FILNAVN = "NAV_33-0005bm-10.2016.pdf"
        const val VEDTAK_VEDLEGG_TITTEL = "Stønadsmottakerens rettigheter og plikter (Barnetrygd)"

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
