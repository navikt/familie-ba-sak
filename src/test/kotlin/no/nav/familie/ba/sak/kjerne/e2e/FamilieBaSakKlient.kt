package no.nav.familie.ba.sak.kjerne.e2e

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import java.net.URI

class FamilieBaSakKlient(
        private val baSakUrl: String,
        restOperations: RestOperations,
        private val headers: HttpHeaders
) : AbstractRestClient(restOperations, "familie-ba-sak") {

    fun hentFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId")

        return getForEntity(
                uri,
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

    fun leggTilVedtakBegrunnelse(fagsakId: Long, vedtakBegrunnelse: RestPostVedtakBegrunnelse): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId/vedtak/begrunnelser")

        return postForEntity(uri, vedtakBegrunnelse, headers)
    }

    fun oppdaterVedtaksperiodeMedBegrunnelser(vedtaksperiodeId: Long,
                                              restPutVedtaksperiodeMedBegrunnelse: RestPutVedtaksperiodeMedBegrunnelse): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/vedtaksperioder/$vedtaksperiodeId")

        return putForEntity(uri, restPutVedtaksperiodeMedBegrunnelse, headers)
    }


    fun sendTilBeslutter(fagsakId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId/send-til-beslutter?behandlendeEnhet=9999")

        return postForEntity(uri, "", headers)
    }

    fun iverksettVedtak(fagsakId: Long,
                        restBeslutningPåVedtak: RestBeslutningPåVedtak,
                        beslutterHeaders: HttpHeaders): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId/iverksett-vedtak")

        return postForEntity(uri, restBeslutningPåVedtak, beslutterHeaders)
    }
}