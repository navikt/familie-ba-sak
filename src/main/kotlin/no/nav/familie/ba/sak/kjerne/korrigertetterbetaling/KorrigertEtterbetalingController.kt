package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import no.nav.familie.ba.sak.ekstern.restDomene.RestKorrigertEtterbetaling
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKorrigertEtterbetaling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/korrigertetterbetaling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KorrigertEtterbetalingController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettKorrigertEtterbetalingPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody korrigertEtterbetalingRequest: KorrigertEtterbetalingRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val korrigertEtterbetaling = korrigertEtterbetalingRequest.tilKorrigertEtterbetaling(behandling)

        korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId)))
    }

    @GetMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleKorrigerteEtterbetalingPåBehandling(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<List<RestKorrigertEtterbetaling>>> {
        val korrigerteEtterbetalinger = korrigertEtterbetalingService.finnAlleKorrigeringerPåBehandling(behandlingId)
            .map { it.tilRestKorrigertEtterbetaling() }

        return ResponseEntity.ok(Ressurs.success(korrigerteEtterbetalinger))
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settKorrigertEtterbetalingTilInaktivPåBehandling(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}
