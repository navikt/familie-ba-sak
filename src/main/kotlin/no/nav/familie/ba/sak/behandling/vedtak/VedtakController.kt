package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.domene.RestVedtakSimulering
import no.nav.familie.ba.sak.tilbakekreving.RestTilbakekreving
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
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
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(
        private val vedtakService: VedtakService,
        private val fagsakService: FagsakService,
        private val stegService: StegService,
        private val tilbakekrevingService: TilbakekrevingService,
        private val simuleringService: SimuleringService,
) {

    @GetMapping(path = ["/{vedtakId}/simulering"])
    fun hentSimulering(@PathVariable @VedtaktilgangConstraint
                       vedtakId: Long): ResponseEntity<Ressurs<RestVedtakSimulering>> {
        val vedtakSimuleringMottaker = simuleringService.hentEllerOppdaterSimuleringPåVedtak(vedtakId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottaker)
        return ResponseEntity.ok(Ressurs.success(restSimulering))
    }

    @Transactional
    @PostMapping(path = ["/{vedtakId}/tilbakekreving"])
    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
            @PathVariable vedtakId: Long,
            @RequestBody restTilbakekreving: RestTilbakekreving?): ResponseEntity<Ressurs<RestFagsak>> {

        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, vedtakId)
        if (restTilbakekreving != null) {
            tilbakekrevingService.lagreTilbakekreving(restTilbakekreving)
        }

        val behandling = vedtakService.hent(vedtakId).behandling
        stegService.håndterSimulering(behandling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }
}

data class RestBeslutningPåVedtak(
        val beslutning: Beslutning,
        val begrunnelse: String? = null
)

enum class Beslutning {
    GODKJENT,
    UNDERKJENT;

    fun erGodkjent() = this == GODKJENT
}