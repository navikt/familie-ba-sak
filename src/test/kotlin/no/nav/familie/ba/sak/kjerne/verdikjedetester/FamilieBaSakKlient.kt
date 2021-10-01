package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.dokument.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import java.net.URI

class FamilieBaSakKlient(
        private val baSakUrl: String,
        restOperations: RestOperations,
        private val headers: HttpHeaders
) : AbstractRestClient(restOperations, "familie-ba-sak") {

    fun opprettFagsak(søkersIdent: String): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker")

        return postForEntity(uri, FagsakRequest(
                personIdent = søkersIdent
        ), headers)
    }

    fun hentFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId")

        return getForEntity(
                uri,
                headers,
        )
    }

    fun hentFagsak(restHentFagsakForPerson: RestHentFagsakForPerson): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/hent-fagsak-paa-person")

        return postForEntity(
                uri,
                restHentFagsakForPerson,
                headers,
        )
    }

    fun journalfør(journalpostId: String,
                   oppgaveId: String,
                   journalførendeEnhet: String,
                   restJournalføring: RestJournalføring): Ressurs<String> {
        val uri =
                URI.create("$baSakUrl/api/journalpost/$journalpostId/journalfør/$oppgaveId?journalfoerendeEnhet=$journalførendeEnhet")
        return postForEntity(
                uri,
                restJournalføring,
                headers
        )
    }

    fun behandlingsresultatStegOgGåVidereTilNesteSteg(behandlingId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/behandlingsresultat")

        return postForEntity(uri, "", headers)
    }

    fun henleggSøknad(behandlingId: Long, restHenleggBehandlingInfo: RestHenleggBehandlingInfo): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/behandlinger/${behandlingId}/henlegg")
        return putForEntity(uri, restHenleggBehandlingInfo, headers)
    }

    fun hentBehandlingslogg(behandlingId: Long): Ressurs<List<Logg>> {
        val uri = URI.create("$baSakUrl/api/logg/${behandlingId}")
        return getForEntity(uri, headers)
    }

    fun opprettBehandling(søkersIdent: String,
                          behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                          behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/behandlinger")

        return postForEntity(
                uri,
                NyBehandling(
                        kategori = BehandlingKategori.NASJONAL,
                        underkategori = BehandlingUnderkategori.ORDINÆR,
                        søkersIdent = søkersIdent,
                        behandlingType = behandlingType,
                        behandlingÅrsak = behandlingÅrsak
                ),
                headers
        )
    }

    fun registrererSøknad(behandlingId: Long, restRegistrerSøknad: RestRegistrerSøknad): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/registrere-søknad-og-hent-persongrunnlag")

        return postForEntity(uri, restRegistrerSøknad, headers)
    }

    fun putVilkår(behandlingId: Long, vilkårId: Long, restPersonResultat: RestPersonResultat): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/vilkaarsvurdering/$behandlingId/$vilkårId")

        return putForEntity(uri, restPersonResultat, headers)
    }

    fun validerVilkårsvurdering(behandlingId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/vilkaarsvurdering/$behandlingId/valider")

        return postForEntity(uri, "", headers)
    }

    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(behandlingId: Long,
                                                  restTilbakekreving: RestTilbakekreving): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/tilbakekreving")

        return postForEntity(uri, restTilbakekreving, headers)
    }


    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(vedtaksperiodeId: Long,
                                                      restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/vedtaksperioder/standardbegrunnelser/$vedtaksperiodeId")

        return putForEntity(uri, restPutVedtaksperiodeMedStandardbegrunnelser, headers)
    }


    fun sendTilBeslutter(fagsakId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId/send-til-beslutter?behandlendeEnhet=$DEFAULT_JOURNALFØRENDE_ENHET")

        return postForEntity(uri, "", headers)
    }

    fun iverksettVedtak(fagsakId: Long,
                        restBeslutningPåVedtak: RestBeslutningPåVedtak,
                        beslutterHeaders: HttpHeaders): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId/iverksett-vedtak")

        return postForEntity(uri, restBeslutningPåVedtak, beslutterHeaders)
    }

    fun migrering(ident: String): Ressurs<MigreringResponseDto> {
        val uri = URI.create("$baSakUrl/api/migrering")

        return postForEntity(uri, PersonIdent(ident), headers)
    }

    fun hentTasker(key: String, value: String): ResponseEntity<List<Task>> {
        val uri = URI.create("$baSakUrl/api/e2e/task/$key/$value")

        return getForEntity(uri, headers)
    }

    fun forhaandsvisHenleggelseBrev(behandlingId: Long, manueltBrevRequest: ManueltBrevRequest): Ressurs<ByteArray>? {
        val uri = URI.create("$baSakUrl/api/dokument/forhaandsvis-brev/${behandlingId}")
        return postForEntity(uri, manueltBrevRequest, headers)
    }
}