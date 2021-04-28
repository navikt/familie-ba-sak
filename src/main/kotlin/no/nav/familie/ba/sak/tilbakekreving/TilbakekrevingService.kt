package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(
        private val tilbakekrevingRepository: TilbakekrevingRepository,
        private val vedtakRepository: VedtakRepository,
        private val simuleringService: SimuleringService,
        private val tilgangService: TilgangService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val tilbakekrevingRestClient: TilbakekrevingRestClient
) {

    fun validerRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, vedtakId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette tilbakekreving")

        val simulering = simuleringService.hentSimuleringPåVedtak(vedtakId)
        val feilutbetaling = vedtakSimuleringMottakereTilRestSimulering(simulering).feilutbetaling
        validerVerdierPåRestTilbakekreving(restTilbakekreving, feilutbetaling)
    }

    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving): Tilbakekreving? {
        val vedtak = vedtakRepository.finnVedtak(restTilbakekreving.vedtakId)
        vedtak.tilbakekreving = Tilbakekreving(
                begrunnelse = restTilbakekreving.begrunnelse,
                vedtak = vedtak,
                valg = restTilbakekreving.valg,
                varsel = restTilbakekreving.varsel,
                tilbakekrevingsbehandlingId = tilbakekrevingRepository.findByVedtakId(vedtak.id)?.tilbakekrevingsbehandlingId,
        )
        return vedtakRepository.save(vedtak).tilbakekreving
    }

    fun slettTilbakekrevingPåAktivtVedtak(behandlingId: Long) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)
                     ?: throw Feil("Fant ikke vedtak for behandling $behandlingId ved sletting av tilbakekreving")
        vedtak.tilbakekreving = null
        vedtakRepository.save(vedtak)
    }

    fun hentForhåndsvisningVarselbrev(behandlingId: Long): ByteArray {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                                                      handling = "hent forhåndsvisning av varselbrev for tilbakekreving")

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)
                     ?: throw Feil("Fant ikke vedtak for behandling $behandlingId ved forhåndsvisning av varselbrev for tilbakekreving.")

        val tilbakekreving = vedtak.tilbakekreving
        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId)
                             ?: throw Feil("Fant ikke aktivt persongrunnlag ved forhåndsvisning av varselbrev for tilbakekreving.")
        val arbeidsfordeling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId)

        val simulering = simuleringService.hentSimuleringPåVedtak(vedtakId = vedtak.id)

        return tilbakekrevingRestClient.hentForhåndsvisningVarselbrev(
                forhåndsvisVarselbrevRequest = ForhåndsvisVarselbrevRequest(
                        varseltekst = tilbakekreving?.varsel,
                        ytelsestype = Ytelsestype.BARNETRYGD,
                        behandlendeEnhetId = arbeidsfordeling.behandlendeEnhetId,
                        behandlendeEnhetsNavn = arbeidsfordeling.behandlendeEnhetNavn,
                        språkkode = when (persongrunnlag.søker.målform) {
                            Målform.NB -> Språkkode.NB
                            Målform.NN -> Språkkode.NN
                        },
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
}
