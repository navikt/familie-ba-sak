package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.sikkerhet.validering.BehandlingstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class SimuleringController(
    private val simuleringService: SimuleringService
) {

    @GetMapping(path = ["/{behandlingId}/simulering"])
    fun hentSimulering(
        @PathVariable @BehandlingstilgangConstraint behandlingId: Long
    ): ResponseEntity<Ressurs<RestSimulering>> {
        val vedtakSimuleringMottaker = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottaker)
        return ResponseEntity.ok(Ressurs.success(restSimulering))
    }
}
