package no.nav.familie.ba.sak.tilbake

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.TilbakeRestClient
import no.nav.familie.ba.sak.simulering.TilbakekrevingId
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
import no.nav.familie.ba.sak.tilbakekreving.slåsammenNærliggendeFeilutbtalingPerioder
import no.nav.familie.kontrakter.felles.tilbakekreving.*
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(
        private val vedtakService: VedtakService,
        private val tilbakeRestClient: TilbakeRestClient,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val simuleringService: SimuleringService,
) {

    fun søkerHarÅpenTilbakekreving(vedtakId: Long): Boolean = tilbakeRestClient.harÅpenTilbakekreingBehandling(vedtakId)

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
                // Manuelt opprettet er per nå ikke håndtert i familie-tilbake.
                manueltOpprettet = false,
                språkkode = språkkode,
                enhetId = enhet.behandlendeEnhetId,
                enhetsnavn = enhet.behandlendeEnhetNavn,
                varsel = opprettVarsel(vedtak),
                revurderingsvedtaksdato = revurderingsvedtaksdato,
                // Verge er per nå ikke støttet i familie-ba-sak.
                verge = null,
                faktainfo = hentFaktainfoForTilbakekreving(vedtak),
        )
    }

    private fun opprettVarsel(vedtak: Vedtak): Varsel? {
        if (vedtak.tilbakekreving?.valg == Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL) {
            val varseltekst = vedtak.tilbakekreving?.varsel ?: throw FunksjonellFeil("Varseltekst er ikke satt")
            val restSimulering =
                    vedtakSimuleringMottakereTilRestSimulering(simuleringService.hentSimuleringPåVedtak(vedtakId = vedtak.id))


            return Varsel(varseltekst = varseltekst,
                          sumFeilutbetaling = restSimulering.feilutbetaling,
                          perioder = slåsammenNærliggendeFeilutbtalingPerioder(restSimulering.perioder))
        }
        return null
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