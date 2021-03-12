package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
        private val fagsakService: FagsakService,
) {

    @GetMapping(path = ["/{vedtakId}"])
    fun hentSimulering(@PathVariable @VedtaktilgangConstraint vedtakId: Long): List<VedtakSimuleringMottaker> {
        return simuleringService.hentEllerOppdaterSimuleringPåVedtak(vedtakId)
    }

    @PostMapping(path = ["/{vedtakId}/valider"])
    fun validerSimulering(@PathVariable vedtakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = vedtakService.hent(vedtakId).behandling
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }
}