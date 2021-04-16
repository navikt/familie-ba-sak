package no.nav.familie.ba.sak.tilbake

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.simulering.TilbakeRestClient
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Pattern

class TilbakekrevingService(
        private val vedtakService: VedtakService,
        private val tilbakeRestClient: TilbakeRestClient,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,

        ) {

    fun vedtakHarTilbakekreving(vedtakId: Long): Boolean = false

    fun opprettRequestMotFamilieTilbake(vedtak: Vedtak):
            String {

        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(vedtak)
        return tilbakeRestClient.opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest)
    }

    fun lagOpprettTilbakekrevingRequest(vedtak: Vedtak):OpprettTilbakekrevingRequest {
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
                // TODO: Sjekk opp med teamet. Til datavarehus. Dersom vi sender ting til datavarehus må vi ha samme ID.
                eksternId = "",
                // TODO: Starter med enkel tilbakekreving i førsteomgang. Lage favrokort på TILBAKEKREVING_REVURDERING?
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
        val tilbakekreving = hentTilbakekreving(vedtak.id);

        return Faktainfo(
                revurderingsårsak = vedtak.behandling.opprettetÅrsak.name,
                revurderingsresultat = vedtak.behandling.resultat.name,
                // TODO legge inn når funksjonalliteten rundt tilbakekreving finnes.
                tilbakekrevingsvalg = null,
                // TODO: Kommer senere
                konsekvensForYtelser = emptySet(),
        )
    }

    fun hentTilbakekreving(vedtakId: Long): Unit {}
}