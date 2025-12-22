package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.AvregningService
import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val behandlingRepository: BehandlingRepository,
) {
    @GetMapping(path = ["/{behandlingId}/simulering"])
    fun hentSimulering(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestSimulering>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        val vedtakSimuleringMottaker = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId)
        val avregningsperioder = avregningService.hentPerioderMedAvregning(behandlingId)

        val fagsakId = behandlingRepository.finnBehandling(behandlingId).fagsak.id
        val overlappendePerioderMedAndreFagsaker = finnOverlappendePerioder(vedtakSimuleringMottaker, fagsakId)

        if (overlappendePerioderMedAndreFagsaker.isNotEmpty()) {
            logger.info("Fant overlappende perioder for fagsak $fagsakId. Overlapper med $overlappendePerioderMedAndreFagsaker")
        }

        val simulering =
            vedtakSimuleringMottakereTilRestSimulering(
                økonomiSimuleringMottakere = vedtakSimuleringMottaker,
            )

        val restSimulering =
            simulering.tilRestSimulering(avregningsperioder, overlappendePerioderMedAndreFagsaker)
        return ResponseEntity.ok(Ressurs.success(restSimulering))
    }

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(SimuleringController::class.java)
    }
}
