package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.FiltreringResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilFiltreringResultatDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
class FiltreringResultatController(
    private val filtreringResultatRepository: FiltreringResultatRepository,
    private val tilgangService: TilgangService,
) {
    @GetMapping(path = ["/{behandlingId}/filtreringsresultater"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFiltreringsresultater(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<FiltreringResultatDto>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Henter filtreringsresultater",
        )

        val filtreringResultater =
            filtreringResultatRepository
                .finnFiltreringResultater(behandlingId = behandlingId)
                .map { it.tilFiltreringResultatDto() }

        return ResponseEntity.ok(Ressurs.success(filtreringResultater))
    }
}
