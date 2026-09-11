package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatusScheduler
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/fagsaklåsing")
class ForvalterFagsaklåsingController(
    private val tilgangService: TilgangService,
    private val fagsakStatusScheduler: FagsakStatusScheduler,
    private val fagsakService: FagsakService,
) {
    @PostMapping("/start-batch")
    @Operation(summary = "Start batch for å låse fagsaker iht. arkivloven. maksAntall begrenser hvor mange fagsaker som låses i denne kjøringen.")
    fun startFagsakLåsingBatch(
        @RequestParam maksAntall: Int,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Start fagsaklåsing-batch",
        )
        val startet = fagsakStatusScheduler.startFagsakLåsing(maksAntall = maksAntall)
        val melding = if (startet) "Fagsaklåsing-batch startet med maks $maksAntall fagsaker" else "Fagsaklåsing-batch ble ikke startet: toggle er av"
        return ResponseEntity.ok(Ressurs.success(melding))
    }

    @PostMapping("/lås-fagsak/{fagsakId}")
    @Operation(summary = "Lås én fagsak iht. arkivloven")
    fun låsFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Lås fagsak",
        )
        fagsakService.låsFagsak(fagsakId)
        return ResponseEntity.ok(Ressurs.success("Fagsak $fagsakId er låst"))
    }
}
