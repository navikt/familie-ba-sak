package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/overstyrtutbetaling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OverstyrtUtbetalingController(
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) {

    @PutMapping(path = ["{behandlingId}/{overstyrtbetalingId}"])
    fun oppdaterOverstyrtUtbetalingOgOppdaterTilkjentYtelse(
        @PathVariable(name = "behandlingId") behandlingId: Long,
        @PathVariable(name = "behandlingId") overstyrtbetalingId: Long,
        @RequestBody restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater overstyrtbetaling"
        )

        val behandling = behandlingService.hent(behandlingId)

        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling,
            overstyrtbetalingId,
            restEndretUtbetalingAndel
        )

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @DeleteMapping(path = ["{behandlingId}/{overstyrtbetalingId}"])
    fun fjernOverstyrtUtbetalingOgOppdaterTilkjentYtelse(
        @PathVariable(name = "behandlingId") behandlingId: Long,
        @PathVariable(name = "overstyrtbetalingId") overstyrtbetalingId: Long,
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater overstyrtbetaling"
        )

        val behandling = behandlingService.hent(behandlingId)

        endretUtbetalingAndelService.fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling, overstyrtbetalingId)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    @Transactional
    @PostMapping(path = ["/{behandlingId}"])
    fun lagreOverstyrtUtbetalingOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @RequestBody restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett overstyrtbetaling"
        )

        val behandling = behandlingService.hent(behandlingId)
        endretUtbetalingAndelService.opprettEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling, restEndretUtbetalingAndel)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }
}