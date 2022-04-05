package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/endretutbetalingandel")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EndretUtbetalingAndelController(
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val tilgangService: TilgangService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) {

    @PutMapping(path = ["{behandlingId}/{endretUtbetalingAndelId}"])
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable endretUtbetalingAndelId: Long,
        @RequestBody restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater endretutbetalingandel"
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndelId,
            restEndretUtbetalingAndel
        )

        tilbakestillBehandlingService.resettStegVedEndringPåBehandlingsresultatSteg(behandlingId = behandling.id)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandling.id)
            )
        )
    }

    @DeleteMapping(path = ["{behandlingId}/{endretUtbetalingAndelId}"])
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @PathVariable endretUtbetalingAndelId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.DELETE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater endretutbetalingandel"
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        endretUtbetalingAndelService.fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            endretUtbetalingAndelId
        )

        tilbakestillBehandlingService.resettStegVedEndringPåBehandlingsresultatSteg(behandlingId = behandling.id)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandling.id)
            )
        )
    }

    @PostMapping(path = ["/{behandlingId}"])
    fun lagreEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett endretutbetalingandel"
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        endretUtbetalingAndelService.opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling)

        tilbakestillBehandlingService.resettStegVedEndringPåBehandlingsresultatSteg(behandlingId = behandling.id)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandling.id)
            )
        )
    }
}
