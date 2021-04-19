package no.nav.familie.ba.sak.tilbake

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.simulering.TilbakeRestClient
import no.nav.familie.ba.sak.simulering.TilbakekrevingId
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(
        private val vedtakService: VedtakService,
        private val tilbakeRestClient: TilbakeRestClient,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,

        ) {

    fun søkerHarÅpenTilbakekreving(vedtakId: Long): Boolean =
            tilbakeRestClient.hentÅpenBehandling().isNotEmpty()

    fun opprettTilbakekreving(vedtak: Vedtak): TilbakekrevingId = tilbakeRestClient.opprettTilbakekrevingBehandling(
            lagOpprettTilbakekrevingRequest(vedtak))

    fun lagOpprettTilbakekrevingRequest(vedtak: Vedtak): OpprettTilbakekrevingRequest {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id) ?: throw Feil(
                message = "Finner ikke personopplysningsgrunnlag på vedtak ${vedtak.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake",
        )

        val språkkode = when (personopplysningGrunnlag.søker.målform) {
            Målform.NB -> Språkkode.NB
            Målform.NN -> Språkkode.NN
        }

        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id)

        val revurderingsvedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: throw Feil(
                message = "Finner ikke revurderingsvedtaksdato på vedtak ${vedtak.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake"
        )

        return OpprettTilbakekrevingRequest(
                fagsystem = Fagsystem.BA,
                ytelsestype = Ytelsestype.BARNETRYGD,
                eksternFagsakId = vedtak.behandling.fagsak.id.toString(),
                personIdent = personopplysningGrunnlag.søker.personIdent.ident,
                eksternId = vedtak.behandling.id.toString(),
                behandlingstype = Behandlingstype.TILBAKEKREVING,
                // TODO: Manuelt opprettet = true er ikke laget.
                manueltOpprettet = false,
                språkkode = språkkode,
                enhetId = enhet.behandlendeEnhetId,
                enhetsnavn = enhet.behandlendeEnhetNavn,
                // TODO legge inn varsel når funksjonalliteten finnes. Husk å slå sammen periodene
                varsel = null,
                revurderingsvedtaksdato = revurderingsvedtaksdato,
                // TODO: Kommer senere
                verge = null,
                faktainfo = hentFaktainfoForTilbakekreving(vedtak),
        )
    }

    fun hentFaktainfoForTilbakekreving(vedtak: Vedtak): Faktainfo {
        // TODO: Hent tilbakekrevinf info når db-modellen er utvidet.
        //val tilbakekreving = hentTilbakekreving(vedtak.id);

        return Faktainfo(
                revurderingsårsak = vedtak.behandling.opprettetÅrsak.name,
                revurderingsresultat = vedtak.behandling.resultat.name,
                // TODO legge inn når funksjonalliteten rundt tilbakekreving finnes.
                tilbakekrevingsvalg = null,
                // TODO: Kommer senere
                konsekvensForYtelser = emptySet(),
        )
    }
}