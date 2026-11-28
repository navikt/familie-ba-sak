package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.BrevmottakerDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brevmottaker")
@Validated
class BrevmottakerController(
    private val tilgangService: TilgangService,
    private val brevmottakerService: BrevmottakerService,
) {
    @PostMapping(path = ["{behandlingId}"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    fun leggTilBrevmottaker(
        @PathVariable behandlingId: Long,
        @RequestBody brevmottaker: BrevmottakerDto,
    ): ResponseEntity<Ressurs<List<BrevmottakerDto>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "legge til brevmottaker")
        tilgangService.validerKanRedigereBehandling(behandlingId)

        brevmottakerService.opprettBrevmottaker(brevmottaker.tilDomene(behandlingId))

        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId).map { BrevmottakerDto.fra(it) }

        return ResponseEntity.ok(Ressurs.success(brevmottakere))
    }

    @DeleteMapping(path = ["{behandlingId}/{mottakerId}"])
    fun slettBrevmottaker(
        @PathVariable mottakerId: Long,
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<BrevmottakerDto>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "fjerne brevmottaker")
        tilgangService.validerKanRedigereBehandling(behandlingId)

        brevmottakerService.slettBrevmottaker(id = mottakerId, behandlingId = behandlingId)

        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId).map { BrevmottakerDto.fra(it) }

        return ResponseEntity.ok(Ressurs.success(brevmottakere))
    }

    @GetMapping(path = ["{behandlingId}"], produces = [APPLICATION_JSON_VALUE])
    fun hentBrevmottakere(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<BrevmottakerDto>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.VEILEDER, "hente brevmottakere")

        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId).map { BrevmottakerDto.fra(it) }

        return ResponseEntity.ok(Ressurs.success(brevmottakere))
    }
}
