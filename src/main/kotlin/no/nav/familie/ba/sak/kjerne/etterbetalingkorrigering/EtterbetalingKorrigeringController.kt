package no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering

import no.nav.familie.ba.sak.ekstern.restDomene.RestEtterbetalingKorrigering
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestEtterbetalingKorrigering
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
@RequestMapping("/api/etterbetalingkorrigering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EtterbetalingKorrigeringController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val etterbetalingKorrigeringService: EtterbetalingKorrigeringService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettEtterbetalingKorrigering(
        @PathVariable behandlingId: Long,
        @RequestBody etterbetalingKorrigeringRequest: EtterbetalingKorrigeringRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val etterbetalingKorrigering = etterbetalingKorrigeringRequest.tilEtterbetalingKorrigering(behandling)

        etterbetalingKorrigeringService.lagreEtterbetalingKorrigering(etterbetalingKorrigering)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId)))
    }

    @GetMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleEtterbetalingKorrigeringerPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<RestEtterbetalingKorrigering>>> {
        val etterbetalingKorrigeringer = etterbetalingKorrigeringService.finnAlleKorrigeringerPåBehandling(behandlingId)
            .map { it.tilRestEtterbetalingKorrigering() }

        return ResponseEntity.ok(Ressurs.success(etterbetalingKorrigeringer))
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settEtterbetalingKorrigeringPåBehandlingTilInaktiv(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        etterbetalingKorrigeringService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}
