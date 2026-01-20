package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingsbehandlingDto
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tilbakekreving")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TilbakekrevingController(
    private val tilgangService: TilgangService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val tilbakekrevingsbehandlingService: TilbakekrevingsbehandlingService,
) {
    @GetMapping(path = ["/fagsak/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTilbakekrevingsbehandlinger(
        @PathVariable fagsakId: Long,
    ): Ressurs<List<TilbakekrevingsbehandlingDto>> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            event = AuditLoggerEvent.ACCESS,
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente tilbakekrevingsbehandlinger",
        )

        val tilbakekrevingsbehandlinger = tilbakekrevingsbehandlingService.hentTilbakekrevingsbehandlingerDto((fagsakId))
        return Ressurs.success(tilbakekrevingsbehandlinger)
    }

    @PostMapping("/{behandlingId}/forhandsvis-varselbrev")
    fun hentForhåndsvisningVarselbrev(
        @PathVariable
        behandlingId: Long,
        @RequestBody
        forhåndsvisTilbakekrevingsvarselbrevRequest: ForhåndsvisTilbakekrevingsvarselbrevRequest,
    ): ResponseEntity<Ressurs<ByteArray>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent forhåndsvisning av varselbrev for tilbakekreving",
        )

        return ResponseEntity.ok(
            Ressurs.success(
                tilbakekrevingService.hentForhåndsvisningVarselbrev(
                    behandlingId,
                    forhåndsvisTilbakekrevingsvarselbrevRequest,
                ),
            ),
        )
    }
}

data class ForhåndsvisTilbakekrevingsvarselbrevRequest(
    val fritekst: String,
)
