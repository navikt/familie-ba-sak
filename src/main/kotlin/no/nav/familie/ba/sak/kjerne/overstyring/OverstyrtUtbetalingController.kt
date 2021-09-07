package no.nav.familie.ba.sak.kjerne.overstyring

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestOverstyrtUtbetaling
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
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
    private val overstyrtUtbetalingService: OverstyrtUtbetalingService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) {

    @PutMapping(path = ["{behandlingId}/{overstyrtbetalingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterOverstyrtUtbetalingOgOppdaterTilkjentYtelse(@PathVariable(name = "behandlingId") behandlingId: Long,
                                    @RequestBody henleggInfo: RestHenleggBehandlingInfo
    ): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "Oppdater overstyrtbetaling")

        val behandling = behandlingService.hent(behandlingId)
        val response = overstyrtUtbetalingService.opprettOverstyrtUtbetalingOgOppdaterTilkjentYtelse(behandling, henleggInfo)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = response.fagsak.id))
    }

    @Transactional
    @PostMapping(path = ["/{behandlingId}"])
    fun lagreOverstyrtUtbetalingOgOppdaterTilkjentYtelse(
        @PathVariable behandlingId: Long,
        @RequestBody restOverstyrtUtbetaling: RestOverstyrtUtbetaling): ResponseEntity<Ressurs<RestFagsak>> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "Opprett overstyrtbetaling")

        val behandling = behandlingService.hent(behandlingId)
        overstyrtUtbetalingService.lagreOverstyrtUtbetalingOgOppdaterTilkjentYtelse(behandling, restOverstyrtUtbetaling)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }
}