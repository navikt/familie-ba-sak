package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class TilbakekrevingService(
        private val tilbakekrevingRepository: TilbakekrevingRepository,
        private val vedtakRepository: VedtakRepository,
        private val simuleringService: SimuleringService,
        private val tilgangService: TilgangService,
) {

    fun validerRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, vedtakId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette tilbakekreving")

        val simulering = simuleringService.hentSimuleringPåVedtak(vedtakId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(simulering)
        if (restSimulering.feilutbetaling != BigDecimal.ZERO && restTilbakekreving == null) {
            throw FunksjonellFeil("Simuleringen har en feilutbetaling, men restTilbakekreving var null",
                                  frontendFeilmelding = "Du må velge en tilbakekrevingsstrategi siden det er en feilutbetaling.")
        }
        if (restSimulering.feilutbetaling == BigDecimal.ZERO && restTilbakekreving != null) {
            throw FunksjonellFeil(
                    "Simuleringen har ikke en feilutbetaling, men restTilbakekreving var ikke null",
                    frontendFeilmelding = "Du kan ikke opprette en tilbakekreving når det ikke er en feilutbetaling."
            )
        }
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
}