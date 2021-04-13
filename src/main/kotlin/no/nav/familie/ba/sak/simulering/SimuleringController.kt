package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.simulering.domene.RestVedtakSimulering
import no.nav.familie.ba.sak.simulering.tilbakekreving.TilbakekrevingDto
import no.nav.familie.ba.sak.simulering.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/simulering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
        private val stegService: StegService,
        private val simuleringService: SimuleringService,
        private val vedtakService: VedtakService,
        private val tilbakekrevingService: TilbakekrevingService,
) {

    @GetMapping(path = ["/{vedtakId}"])
    fun hentSimulering(@PathVariable @VedtaktilgangConstraint
                       vedtakId: Long): ResponseEntity<Ressurs<RestVedtakSimulering>> {
        val vedtakSimuleringMottaker = simuleringService.hentEllerOppdaterSimuleringPåVedtak(vedtakId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottaker)
        return ResponseEntity.ok(Ressurs.success(restSimulering))
    }

    @PostMapping(path = ["/{vedtakId}/bekreft"])
    fun bekreftSimulering(@PathVariable vedtakId: Long,
                          @RequestBody tilbakekrevingDto: TilbakekrevingDto?): ResponseEntity<Ressurs<Unit>> {
        if (tilbakekrevingDto != null) {
            tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto)
        }

        val behandling = vedtakService.hent(vedtakId).behandling
        stegService.håndterSimulering(behandling)

        return ResponseEntity.ok(null)
    }
}