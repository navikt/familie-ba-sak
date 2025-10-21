package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.common.kallEksternTjenesteUtenRespons
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyAktivBrukerIModiaContext
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsforholdRequest
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Skyggesak
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.brev.mottaker.ManuellAdresseInfo
import no.nav.familie.ba.sak.kjerne.modiacontext.ModiaContext
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknad
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.dokdistkanal.Distribusjonskanal
import no.nav.familie.kontrakter.felles.dokdistkanal.DokdistkanalRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.kontrakter.felles.saksbehandler.SaksbehandlerGrupper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

const val DEFAULT_JOURNALFØRENDE_ENHET = "9999"

@Component
class IntegrasjonKlient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "integrasjon") {
    @Cacheable("alle-eøs-land", cacheManager = "dailyCache")
    fun hentAlleEØSLand(): KodeverkDto {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/eea")

        return kallEksternTjenesteRessurs(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent EØS land",
        ) {
            getForEntity(uri)
        }
    }

    @Cacheable("land", cacheManager = "dailyCache")
    fun hentLand(landkode: String): String {
        if (landkode.length != 3) {
            throw Feil("Støtter bare landkoder med tre bokstaver")
        }

        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/$landkode")

        return kallEksternTjenesteRessurs(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent landkoder for $landkode",
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    @Cacheable("poststeder", cacheManager = "dailyCache")
    fun hentPoststeder(): KodeverkDto {
        val uri = URI.create("$integrasjonUri/kodeverk/poststed")

        return kallEksternTjenesteRessurs(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent postnumre",
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    @Cacheable("behandlendeEnhet", cacheManager = "shortCache")
    fun hentBehandlendeEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        val uri =
            UriComponentsBuilder
                .fromUri(integrasjonUri)
                .pathSegment("arbeidsfordeling", "enhet", "BAR")
                .build()
                .toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent behandlende enhet",
        ) {
            postForEntity(uri, mapOf("ident" to ident))
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    @Cacheable("saksbehandler", cacheManager = "shortCache")
    fun hentSaksbehandler(id: String): Saksbehandler {
        val uri =
            UriComponentsBuilder
                .fromUri(integrasjonUri)
                .pathSegment("saksbehandler", id)
                .build()
                .toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "saksbehandler",
            uri = uri,
            formål = "Hent saksbehandler",
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    @Cacheable("saksbehandler", cacheManager = "shortCache")
    fun hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent: NavIdent): List<BarnetrygdEnhet> {
        val uri =
            UriComponentsBuilder
                .fromUri(integrasjonUri)
                .pathSegment("saksbehandler", navIdent.ident, "grupper")
                .build()
                .toUri()

        val saksbehandlerSineGrupper =
            kallEksternTjenesteRessurs<SaksbehandlerGrupper>(
                tjeneste = "saksbehandler",
                uri = uri,
                formål = "Henter gruppene til saksbehandler",
            ) {
                getForEntity(uri)
            }

        return saksbehandlerSineGrupper.value.mapNotNull { navn ->
            BarnetrygdEnhet.entries.find { it.gruppenavn == navn.displayName }
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentArbeidsforhold(
        ident: String,
        ansettelsesperiodeFom: LocalDate,
    ): List<Arbeidsforhold> {
        val uri =
            UriComponentsBuilder
                .fromUri(integrasjonUri)
                .pathSegment("aareg", "arbeidsforhold")
                .build()
                .toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "aareg",
            uri = uri,
            formål = "Hent arbeidsforhold",
        ) {
            postForEntity(uri, ArbeidsforholdRequest(ident, ansettelsesperiodeFom))
        }
    }

    fun distribuerBrev(distribuerDokumentDTO: DistribuerDokumentDTO): String {
        val uri = URI.create("$integrasjonUri/dist/v1")

        val resultat: String =
            kallEksternTjenesteRessurs(
                tjeneste = "dokdist",
                uri = uri,
                formål = "Distribuer brev",
            ) {
                val journalpostRequest =
                    DistribuerJournalpostRequest(
                        journalpostId = distribuerDokumentDTO.journalpostId,
                        bestillendeFagsystem = Fagsystem.BA,
                        dokumentProdApp = "FAMILIE_BA_SAK",
                        distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
                        distribusjonstype = distribuerDokumentDTO.brevmal.distribusjonstype,
                        adresse = distribuerDokumentDTO.manuellAdresseInfo?.let { lagManuellAdresse(it) },
                    )
                postForEntity(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8())
            }

        if (resultat.isBlank()) throw Feil("BestillingsId fra integrasjonstjenesten mot dokdist er tom")
        return resultat
    }

    private fun lagManuellAdresse(manuellAdresseInfo: ManuellAdresseInfo) =
        ManuellAdresse(
            adresseType =
                when (manuellAdresseInfo.landkode) {
                    "NO" -> AdresseType.norskPostadresse
                    else -> AdresseType.utenlandskPostadresse
                },
            adresselinje1 = manuellAdresseInfo.adresselinje1,
            adresselinje2 = manuellAdresseInfo.adresselinje2,
            postnummer = manuellAdresseInfo.postnummer,
            poststed = manuellAdresseInfo.poststed,
            land = manuellAdresseInfo.landkode,
        )

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/ferdigstill")

        kallEksternTjenesteUtenRespons(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Ferdigstill oppgave",
        ) {
            patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
        }
    }

    fun oppdaterOppgave(
        oppgaveId: Long,
        oppdatertOppgave: Oppgave,
    ) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/oppdater")

        kallEksternTjenesteUtenRespons(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Oppdater oppgave",
        ) {
            patchForEntity<Ressurs<OppgaveResponse>>(uri, oppdatertOppgave)
        }
    }

    @Cacheable("enhet", cacheManager = "dailyCache")
    fun hentEnhet(enhetId: String): NavKontorEnhet {
        val uri = URI.create("$integrasjonUri/arbeidsfordeling/nav-kontor/$enhetId")

        return kallEksternTjenesteRessurs(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent nav kontor for enhet $enhetId",
        ) {
            getForEntity(uri)
        }
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/opprett")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Opprett oppgave",
        ) {
            postForEntity(
                uri,
                opprettOppgave,
                HttpHeaders().medContentTypeJsonUTF8(),
            )
        }
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/${patchOppgave.id}/oppdater")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Patch oppgave",
        ) {
            patchForEntity(
                uri,
                patchOppgave,
                HttpHeaders().medContentTypeJsonUTF8(),
            )
        }
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String?,
    ): OppgaveResponse {
        val baseUri = URI.create("$integrasjonUri/oppgave/$oppgaveId/fordel")
        val uri =
            if (saksbehandler == null) {
                baseUri
            } else {
                UriComponentsBuilder
                    .fromUri(baseUri)
                    .queryParam("saksbehandler", saksbehandler)
                    .build()
                    .toUri()
            }

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Fordel oppgave",
        ) {
            postForEntity(
                uri,
                HttpHeaders().medContentTypeJsonUTF8(),
            )
        }
    }

    fun tilordneEnhetOgRessursForOppgave(
        oppgaveId: Long,
        nyEnhet: String,
    ): OppgaveResponse {
        val baseUri = URI.create("$integrasjonUri/oppgave/$oppgaveId/enhet/$nyEnhet")
        val uri =
            UriComponentsBuilder
                .fromUri(baseUri)
                .queryParam("fjernMappeFraOppgave", true)
                .queryParam("nullstillTilordnetRessurs", true)
                .build()
                .toUri() // fjerner alltid mappe fra Barnetrygd siden hver enhet sin mappestruktur

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Bytt enhet",
        ) {
            patchForEntity(
                uri,
                HttpHeaders().medContentTypeJsonUTF8(),
            )
        }
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Finn oppgave med id $oppgaveId",
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentJournalpost(journalpostId: String): Journalpost {
        val uri = URI.create("$integrasjonUri/journalpost/tilgangsstyrt/baks?journalpostId=$journalpostId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalpost id $journalpostId",
        ) {
            getForEntity(uri)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        val uri = URI.create("$integrasjonUri/journalpost")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalposter for bruker",
        ) {
            postForEntity(uri, journalposterForBrukerRequest)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentTilgangsstyrteJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<TilgangsstyrtJournalpost> {
        val uri = URI.create("$integrasjonUri/journalpost/tilgangsstyrt/baks")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent tilgangsstyrte journalposter for bruker",
        ) {
            postForEntity(uri, journalposterForBrukerRequest)
        }
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        val uri = URI.create("$integrasjonUri/oppgave/v4")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Hent oppgaver",
        ) {
            postForEntity(
                uri,
                finnOppgaveRequest,
                HttpHeaders().medContentTypeJsonUTF8(),
            )
        }
    }

    fun ferdigstillJournalpost(
        journalpostId: String,
        journalførendeEnhet: String,
    ) {
        val uri =
            URI.create("$integrasjonUri/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet")

        kallEksternTjenesteUtenRespons(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalposter for bruker",
        ) {
            putForEntity<Ressurs<Any>>(uri, "")
        }
    }

    fun oppdaterJournalpost(
        request: OppdaterJournalpostRequest,
        journalpostId: String,
    ): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Oppdater journalpost",
        ) {
            putForEntity(uri, request)
        }
    }

    fun leggTilLogiskVedlegg(
        request: LogiskVedleggRequest,
        dokumentinfoId: String,
    ): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Legg til logisk vedlegg på dokument $dokumentinfoId",
        ) {
            postForEntity(uri, request)
        }
    }

    fun slettLogiskVedlegg(
        logiskVedleggId: String,
        dokumentinfoId: String,
    ): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg/$logiskVedleggId")
        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Slett logisk vedlegg på dokument $dokumentinfoId",
        ) {
            deleteForEntity(uri)
        }
    }

    fun hentDokument(
        dokumentInfoId: String,
        journalpostId: String,
    ): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/tilgangsstyrt/baks/$journalpostId/$dokumentInfoId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent dokument $dokumentInfoId",
        ) {
            getForEntity(uri)
        }
    }

    fun journalførDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest,
    ): ArkiverDokumentResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v4")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Journalfør dokument på fagsak ${arkiverDokumentRequest.fagsakId}",
        ) {
            postForEntity(uri, arkiverDokumentRequest)
        }
    }

    fun opprettSkyggesak(
        aktør: Aktør,
        fagsakId: Long,
    ) {
        val uri = URI.create("$integrasjonUri/skyggesak/v1")

        kallEksternTjenesteUtenRespons<Unit>(
            tjeneste = "skyggesak",
            uri = uri,
            formål = "Opprett skyggesak på fagsak $fagsakId",
        ) {
            postForEntity(uri, Skyggesak(aktør.aktørId, fagsakId.toString()))
        }
    }

    @Cacheable("landkoder-ISO_3166-1_alfa-2", cacheManager = "dailyCache")
    fun hentLandkoderISO2(): Map<String, String> {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoderISO2")

        return kallEksternTjenesteRessurs(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent landkoderISO2",
        ) {
            getForEntity(uri)
        }
    }

    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        val uri = URI.create("$integrasjonUri/organisasjon/$organisasjonsnummer")
        return kallEksternTjenesteRessurs(
            tjeneste = "organisasjon",
            uri = uri,
            formål = "Hent organisasjon $organisasjonsnummer",
        ) {
            getForEntity(uri)
        }
    }

    fun hentDistribusjonskanal(request: DokdistkanalRequest): Distribusjonskanal {
        val uri = URI.create("$integrasjonUri/dokdistkanal/BAR")
        return kallEksternTjenesteRessurs(
            tjeneste = "dokdistkanal",
            uri = uri,
            formål = "Hent distribusjonskanal for bruker/mottaker",
        ) {
            postForEntity(uri, request)
        }
    }

    fun settNyAktivBrukerIModiaContext(nyAktivBruker: RestNyAktivBrukerIModiaContext): ModiaContext {
        val uri = URI.create("$integrasjonUri/modia-context-holder/sett-aktiv-bruker")
        return kallEksternTjenesteRessurs(
            tjeneste = "modia.context.holder.sett",
            uri = uri,
            formål = "Oppdatere aktiv bruker i Modia-kontekst for innlogget saksbehandler",
        ) {
            postForEntity(
                uri = uri,
                payload = nyAktivBruker,
                httpHeaders = HttpHeaders().medContentTypeJsonUTF8(),
            )
        }
    }

    fun hentModiaContext(): ModiaContext {
        val uri = URI.create("$integrasjonUri/modia-context-holder")
        return kallEksternTjenesteRessurs(
            tjeneste = "modia.context.holder.hent",
            uri = uri,
            formål = "Hente Modia-kontekst for innlogget saksbehandler",
        ) {
            getForEntity(uri)
        }
    }

    fun hentVersjonertBarnetrygdSøknad(journalpostId: String): VersjonertBarnetrygdSøknad {
        val uri = URI.create("$integrasjonUri/baks/versjonertsoknad/ba/$journalpostId")
        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hente versjonert barnetrygd søknad",
        ) {
            getForEntity<Ressurs<VersjonertBarnetrygdSøknad>>(uri)
        }
    }

    fun hentAInntektUrl(personIdent: PersonIdent): String {
        val url = URI.create("$integrasjonUri/arbeid-og-inntekt/hent-url")

        return kallEksternTjenesteRessurs(
            tjeneste = "a-inntekt-url",
            uri = url,
            formål = "Hent URL for person til A-inntekt",
        ) {
            postForEntity(
                url,
                personIdent,
            )
        }
    }

    fun sjekkErEgenAnsattBulk(personIdenter: List<String>): Map<String, Boolean> {
        val url = URI.create("$integrasjonUri/egenansatt/bulk")

        val egenAnsattResponse =
            kallEksternTjenesteRessurs(
                tjeneste = "skjermede-personer-pip",
                uri = url,
                formål = "Sjekk om personer er egen ansatt",
            ) {
                postForEntity<Ressurs<Map<String, Boolean>>>(
                    url,
                    personIdenter,
                )
            }
        return egenAnsattResponse
    }

    companion object {
        const val VEDTAK_VEDLEGG_FILNAVN = "NAV_33-0005bm-10.2016.pdf"
        const val VEDTAK_VEDLEGG_TITTEL = "Stønadsmottakerens rettigheter og plikter (Barnetrygd)"

        fun hentVedlegg(vedleggsnavn: String): ByteArray? {
            val inputStream = this::class.java.classLoader.getResourceAsStream("dokumenter/$vedleggsnavn")
            return inputStream?.readAllBytes()
        }
    }
}
