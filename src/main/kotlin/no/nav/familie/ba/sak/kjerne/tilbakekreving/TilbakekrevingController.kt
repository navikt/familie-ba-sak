package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.RestTilbakekrevingsbehandling
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
    private val tilbakekrevingService: TilbakekrevingService,
    private val tilbakekrevingsbehandlingService: TilbakekrevingsbehandlingService
) {

    @PostMapping("/{behandlingId}/forhandsvis-varselbrev")
    fun hentForhåndsvisningVarselbrev(
        @PathVariable
        behandlingId: Long,
        @RequestBody
        forhåndsvisTilbakekrevingsvarselbrevRequest: ForhåndsvisTilbakekrevingsvarselbrevRequest
    ): ResponseEntity<Ressurs<ByteArray>> {
        return ResponseEntity.ok(
            Ressurs.success(
                tilbakekrevingService.hentForhåndsvisningVarselbrev(
                    behandlingId,
                    forhåndsvisTilbakekrevingsvarselbrevRequest
                )
            )
        )
    }

    @GetMapping("/fagsaker/{fagsakId}/hent-tilbakekrevingsbehandlinger")
    fun hentTilbakekrevingsbehandlinger(
        @PathVariable fagsakId: Long
    ): ResponseEntity<Ressurs<List<RestTilbakekrevingsbehandling>>> {
        val tilbakekrevingsbehandlinger = tilbakekrevingsbehandlingService.hentRestTilbakekrevingsbehandlinger(fagsakId)

        return ResponseEntity.ok(Ressurs.success(tilbakekrevingsbehandlinger))
    }
}

data class ForhåndsvisTilbakekrevingsvarselbrevRequest(
    val fritekst: String
)
