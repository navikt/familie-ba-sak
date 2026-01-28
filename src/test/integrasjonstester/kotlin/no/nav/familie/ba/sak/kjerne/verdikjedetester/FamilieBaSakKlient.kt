package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.EndretUtbetalingAndelDto
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.HentFagsakForPersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.JournalføringDto
import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.PutVedtaksperiodeMedStandardbegrunnelserDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.kjerne.behandling.HenleggBehandlingInfoDto
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelserDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriUtils.encodePath
import java.net.URI
import java.time.LocalDate

class FamilieBaSakKlient(
    private val baSakUrl: String,
    restOperations: RestOperations,
    private val headers: HttpHeaders,
) : AbstractRestClient(restOperations, "familie-ba-sak") {
    fun opprettFagsak(søkersIdent: String): Ressurs<MinimalFagsakDto> {
        val uri = URI.create("$baSakUrl/api/fagsaker")

        return postForEntity(
            uri,
            FagsakRequest(
                personIdent = søkersIdent,
            ),
            headers,
        )
    }

    fun hentFagsak(fagsakId: Long): Ressurs<FagsakDto> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId")

        return getForEntity(
            uri,
            headers,
        )
    }

    fun hentMinimalFagsakPåPerson(personIdent: String): Ressurs<MinimalFagsakDto> {
        val uri = URI.create("$baSakUrl/api/fagsaker/hent-fagsak-paa-person")

        return postForEntity(
            uri,
            HentFagsakForPersonDto(personIdent),
            headers,
        )
    }

    fun journalfør(
        journalpostId: String,
        oppgaveId: String,
        journalførendeEnhet: String,
        journalføringDto: JournalføringDto,
    ): Ressurs<String> {
        val uri =
            URI.create(encodePath("$baSakUrl/api/journalpost/$journalpostId/journalfør/$oppgaveId") + "?journalfoerendeEnhet=$journalførendeEnhet")
        return postForEntity(
            uri,
            journalføringDto,
            headers,
        )
    }

    fun behandlingsresultatStegOgGåVidereTilNesteSteg(behandlingId: Long): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/behandlingsresultat")

        return postForEntity(uri, "", headers)
    }

    fun henleggSøknad(
        behandlingId: Long,
        henleggBehandlingInfoDto: HenleggBehandlingInfoDto,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/henlegg")
        return putForEntity(uri, henleggBehandlingInfoDto, headers)
    }

    fun hentBehandlingslogg(behandlingId: Long): Ressurs<List<Logg>> {
        val uri = URI.create("$baSakUrl/api/logg/$behandlingId")
        return getForEntity(uri, headers)
    }

    fun opprettBehandling(
        søkersIdent: String,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
        behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
        fagsakId: Long,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create("$baSakUrl/api/behandlinger")

        return postForEntity(
            uri,
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = behandlingUnderkategori,
                søkersIdent = søkersIdent,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                søknadMottattDato = if (behandlingÅrsak == BehandlingÅrsak.SØKNAD) LocalDate.now().minusYears(18) else null,
                fagsakId = fagsakId,
            ),
            headers,
        )
    }

    fun registrererSøknad(
        behandlingId: Long,
        registrerSøknadDto: RegistrerSøknadDto,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri =
            URI.create(encodePath("$baSakUrl/api/behandlinger/$behandlingId/steg/registrer-søknad", "UTF-8"))

        return postForEntity(uri, registrerSøknadDto, headers)
    }

    fun putVilkår(
        behandlingId: Long,
        vilkårId: Long,
        personResultatDto: PersonResultatDto,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create(encodePath("$baSakUrl/api/vilkaarsvurdering/$behandlingId/$vilkårId"))

        return putForEntity(uri, personResultatDto, headers)
    }

    fun validerVilkårsvurdering(behandlingId: Long): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create(encodePath("$baSakUrl/api/behandlinger/$behandlingId/steg/vilkårsvurdering"))
        return postForEntity(uri, "", headers)
    }

    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        behandlingId: Long,
        tilbakekrevingDto: TilbakekrevingDto,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/tilbakekreving")

        return postForEntity(uri, tilbakekrevingDto, headers)
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId: Long,
        putVedtaksperiodeMedStandardbegrunnelserDto: PutVedtaksperiodeMedStandardbegrunnelserDto,
    ): Ressurs<List<UtvidetVedtaksperiodeMedBegrunnelserDto>> {
        val uri = URI.create("$baSakUrl/api/vedtaksperioder/standardbegrunnelser/$vedtaksperiodeId")

        return putForEntity(uri, putVedtaksperiodeMedStandardbegrunnelserDto, headers)
    }

    fun sendTilBeslutter(behandlingId: Long): Ressurs<UtvidetBehandlingDto> {
        val uri =
            URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/send-til-beslutter?behandlendeEnhet=$DEFAULT_JOURNALFØRENDE_ENHET")

        return postForEntity(uri, "", headers)
    }

    fun leggTilEndretUtbetalingAndel(
        behandlingId: Long,
        endretUtbetalingAndelDto: EndretUtbetalingAndelDto,
    ): Ressurs<UtvidetBehandlingDto> {
        val uriPost = URI.create("$baSakUrl/api/endretutbetalingandel/$behandlingId")
        val utvidetBehandlingDto = postForEntity<Ressurs<UtvidetBehandlingDto>>(uriPost, "", headers)

        val endretUtbetalingAndelId =
            utvidetBehandlingDto.data!!
                .endretUtbetalingAndeler
                .first { it.tom == null && it.fom == null }
                .id
        val uriPut = URI.create("$baSakUrl/api/endretutbetalingandel/$behandlingId/$endretUtbetalingAndelId")

        return putForEntity(uriPut, endretUtbetalingAndelDto, headers)
    }

    fun fjernEndretUtbetalingAndel(
        behandlingId: Long,
        endretUtbetalingAndelId: Long,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create("$baSakUrl/api/endretutbetalingandel/$behandlingId/$endretUtbetalingAndelId")

        return deleteForEntity(uri, "", headers)
    }

    fun iverksettVedtak(
        behandlingId: Long,
        beslutningPåVedtakDto: BeslutningPåVedtakDto,
        beslutterHeaders: HttpHeaders,
    ): Ressurs<UtvidetBehandlingDto> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/iverksett-vedtak")

        return postForEntity(uri, beslutningPåVedtakDto, beslutterHeaders)
    }

    fun forhaandsvisHenleggelseBrev(
        behandlingId: Long,
        manueltBrevRequest: ManueltBrevRequest,
    ): Ressurs<ByteArray> {
        val uri = URI.create("$baSakUrl/api/dokument/forhaandsvis-brev/$behandlingId")
        return postForEntity(uri, manueltBrevRequest, headers)
    }

    fun encodePath(path: String): String = encodePath(path, "UTF-8")
}
