package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.beregning.AvregningService
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
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
    private val simuleringService: SimuleringService,
    private val tilgangService: TilgangService,
    private val avregningService: AvregningService,
) {
    @GetMapping(path = ["/{behandlingId}/simulering"])
    fun hentSimulering(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<SimuleringDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        val vedtakSimuleringMottaker = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId)

        val simulering =
            vedtakSimuleringMottakereTilSimulering(
                økonomiSimuleringMottakere = vedtakSimuleringMottaker,
            )

        val overlappendeAvregningAndreFagsaker = avregningService.hentOverlappendePerioderMedAndreFagsaker(behandlingId)
        val etterBetalingEllerFeilutbetalingIAnnenFagsak = simuleringService.hentOverlappendeFeilOgEtterbetalingerFraAndreFagsakerForSøker(behandlingId, simulering)

        val overlappendePerioderMedAndreFagsaker = (overlappendeAvregningAndreFagsaker + etterBetalingEllerFeilutbetalingIAnnenFagsak).distinct()

        val avregningsperioder = avregningService.hentPerioderMedAvregning(behandlingId)

        val restSimulering = simulering.tilSimuleringDto(avregningsperioder, overlappendePerioderMedAndreFagsaker)
        return ResponseEntity.ok(Ressurs.success(restSimulering))
    }
}
