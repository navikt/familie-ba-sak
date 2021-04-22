package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger

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

    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving): Tilbakekreving {
        val vedtak = vedtakRepository.finnVedtak(restTilbakekreving.vedtakId)
        val tidligereTilbakekreving = tilbakekrevingRepository.findByVedtakId(vedtak.id)

        if (tidligereTilbakekreving != null) {
            return oppdaterTilbakekreving(tidligereTilbakekreving, restTilbakekreving)
        } else {
            return opprettNyTilbakekreving(restTilbakekreving, vedtak)
        }
    }

    private fun oppdaterTilbakekreving(tidligereTilbakekreving: Tilbakekreving,
                                       restTilbakekreving: RestTilbakekreving): Tilbakekreving {
        tidligereTilbakekreving.begrunnelse = restTilbakekreving.begrunnelse
        tidligereTilbakekreving.valg = restTilbakekreving.valg
        tidligereTilbakekreving.varsel = restTilbakekreving.varsel
        return tilbakekrevingRepository.save(tidligereTilbakekreving)
    }

    private fun opprettNyTilbakekreving(restTilbakekreving: RestTilbakekreving,
                                        vedtak: Vedtak) =
            tilbakekrevingRepository.save(Tilbakekreving(
                    begrunnelse = restTilbakekreving.begrunnelse,
                    vedtak = vedtak,
                    valg = restTilbakekreving.valg,
                    varsel = restTilbakekreving.varsel,
                    tilbakekrevingsbehandlingId = null,
            ))
}