package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.DeaktiverMinsideTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/minside")
class ForvalterMinSide(
    private val personidentRepository: PersonidentRepository,
    private val tilgangService: TilgangService,
    private val opprettTaskService: OpprettTaskService,
) {
    @PostMapping("/aktiver-for-ident")
    @Operation(
        summary = "Sender Kafka-melding om å aktivere MinSide for en ident",
    )
    fun aktiverMinsideForIdent(
        @RequestBody ident: String,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for å aktivere minside for ident",
        )

        val personIdent = personidentRepository.findByFødselsnummerOrNull(ident) ?: return ResponseEntity.status(404).body("Finner ikke person")

        opprettTaskService.opprettAktiverMinsideTask(personIdent.aktør)

        return ResponseEntity.ok("Task for aktivering av minside for ident opprettet")
    }

    @PostMapping("/deaktiver-for-ident")
    @Operation(
        summary = "Sender Kafka-melding om å deaktivere MinSide for en ident",
    )
    fun deaktiverMinsideForIdent(
        @RequestBody ident: String,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for å deaktivere minside for ident",
        )

        val personIdent = personidentRepository.findByFødselsnummerOrNull(ident) ?: return ResponseEntity.status(404).body("Finner ikke person")

        DeaktiverMinsideTask.opprettTask(personIdent.aktør)

        return ResponseEntity.ok("Task for deaktivering av minside for ident opprettet")
    }
}
