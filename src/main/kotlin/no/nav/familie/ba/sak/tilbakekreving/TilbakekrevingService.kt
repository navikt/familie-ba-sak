package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingService(
        private val tilbakekrevingRepository: TilbakekrevingRepository,
        private val vedtakRepository: VedtakRepository,
        private val behandlingRepository: BehandlingRepository,
        private val simuleringService: SimuleringService,
        private val tilgangService: TilgangService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val tilbakekrevingKlient: TilbakekrevingKlient,
) {

    fun validerRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, behandlingId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette tilbakekreving")

        val simulering = simuleringService.hentSimuleringPåBehandling(behandlingId)
        val feilutbetaling = vedtakSimuleringMottakereTilRestSimulering(simulering).feilutbetaling
        validerVerdierPåRestTilbakekreving(restTilbakekreving, feilutbetaling)
    }

    @Transactional
    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving, behandlingId: Long): Tilbakekreving? {
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        val tilbakekreving = Tilbakekreving(
                begrunnelse = restTilbakekreving.begrunnelse,
                behandling = behandling,
                valg = restTilbakekreving.valg,
                varsel = restTilbakekreving.varsel,
                tilbakekrevingsbehandlingId = tilbakekrevingRepository
                        .findByBehandlingId(behandling.id)?.tilbakekrevingsbehandlingId,
        )

        tilbakekrevingRepository.deleteByBehandlingId(behandlingId)
        return tilbakekrevingRepository.save(tilbakekreving)
    }

    fun slettTilbakekrevingPåBehandling(behandlingId: Long) =
            tilbakekrevingRepository.findByBehandlingId(behandlingId)?.let { tilbakekrevingRepository.delete(it) }

    fun hentForhåndsvisningVarselbrev(
            behandlingId: Long,
            forhåndsvisTilbakekrevingsvarselbrevRequest: ForhåndsvisTilbakekrevingsvarselbrevRequest,
    ): ByteArray {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                                                      handling = "hent forhåndsvisning av varselbrev for tilbakekreving")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)
                     ?: throw Feil("Fant ikke vedtak for behandling $behandlingId ved forhåndsvisning av varselbrev" +
                                   " for tilbakekreving.")

        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId)
                             ?: throw Feil("Fant ikke aktivt persongrunnlag ved forhåndsvisning av varselbrev" +
                                           " for tilbakekreving.")
        val arbeidsfordeling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId)

        return tilbakekrevingKlient.hentForhåndsvisningVarselbrev(
                forhåndsvisVarselbrevRequest = ForhåndsvisVarselbrevRequest(
                        varseltekst = forhåndsvisTilbakekrevingsvarselbrevRequest.fritekst,
                        ytelsestype = Ytelsestype.BARNETRYGD,
                        behandlendeEnhetId = arbeidsfordeling.behandlendeEnhetId,
                        behandlendeEnhetsNavn = arbeidsfordeling.behandlendeEnhetNavn,
                        språkkode = persongrunnlag.søker.målform.tilSpråkkode(),
                        feilutbetaltePerioderDto = FeilutbetaltePerioderDto(
                                sumFeilutbetaling = 0,
                                perioder = emptyList()
                        ),
                        fagsystem = Fagsystem.BA,
                        eksternFagsakId = vedtak.behandling.fagsak.id.toString(),
                        ident = persongrunnlag.søker.personIdent.ident,
                        saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerNavn()
                ))
    }

    fun søkerHarÅpenTilbakekreving(fagsakId: Long): Boolean = tilbakekrevingKlient.harÅpenTilbakekreingBehandling(fagsakId)

    fun opprettTilbakekreving(behandling: Behandling): TilbakekrevingId =
            tilbakekrevingKlient.opprettTilbakekrevingBehandling(lagOpprettTilbakekrevingRequest(behandling))

    fun lagOpprettTilbakekrevingRequest(behandling: Behandling): OpprettTilbakekrevingRequest {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id) ?: throw Feil(
                message = "Finner ikke personopplysningsgrunnlag på vedtak ${behandling.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake",
        )

        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id)

        val aktivtVedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
                           ?: throw Feil("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        val revurderingsvedtaksdato = aktivtVedtak.vedtaksdato?.toLocalDate() ?: throw Feil(
                message = "Finner ikke revurderingsvedtaksdato på vedtak ${aktivtVedtak.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake"
        )

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
                             ?: throw Feil("Fant ikke tilbakekreving på behandling ${behandling.id}")

        return OpprettTilbakekrevingRequest(
                fagsystem = Fagsystem.BA,
                ytelsestype = Ytelsestype.BARNETRYGD,
                eksternFagsakId = behandling.fagsak.id.toString(),
                personIdent = personopplysningGrunnlag.søker.personIdent.ident,
                eksternId = behandling.id.toString(),
                behandlingstype = Behandlingstype.TILBAKEKREVING,
                // Manuelt opprettet er per nå ikke håndtert i familie-tilbake.
                manueltOpprettet = false,
                språkkode = personopplysningGrunnlag.søker.målform.tilSpråkkode(),
                enhetId = enhet.behandlendeEnhetId,
                enhetsnavn = enhet.behandlendeEnhetNavn,
                varsel = opprettVarsel(tilbakekreving, simuleringService.hentSimuleringPåBehandling(behandling.id)),
                revurderingsvedtaksdato = revurderingsvedtaksdato,
                // Verge er per nå ikke støttet i familie-ba-sak.
                verge = null,
                faktainfo = hentFaktainfoForTilbakekreving(behandling, tilbakekreving),
        )
    }
}