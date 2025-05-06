package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterTilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandling/{behandlingId}/tilbakekrevingsvedtak-motregning")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TilbakekrevingsvedtakMotregningController(
    val tilgangService: TilgangService,
    val tilbakekrevingsvedtakMotregningService: TilbakekrevingsvedtakMotregningService,
    val tilbakekrevingsvedtakMotregningBrevService: TilbakekrevingsvedtakMotregningBrevService,
) {
    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentTilbakekrevingsvedtakMotregning(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestTilbakekrevingsvedtakMotregning?>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Hent TilbakekrevingsvedtakMotregning",
        )
        val tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(tilbakekrevingsvedtakMotregning?.tilRestTilbakekrevingsvedtakMotregning()))
    }

    @PatchMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterTilbakekrevingsvedtakMotregning(
        @PathVariable behandlingId: Long,
        @RequestBody restOppdaterTilbakekrevingsvedtakMotregning: RestOppdaterTilbakekrevingsvedtakMotregning,
    ): ResponseEntity<Ressurs<RestTilbakekrevingsvedtakMotregning>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater tilbakekrevingsvedtak motregning",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val oppdatertTilbakekrevingsvedtakMotregning =
            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandlingId,
                samtykke = restOppdaterTilbakekrevingsvedtakMotregning.samtykke,
                årsakTilFeilutbetaling = restOppdaterTilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling,
                vurderingAvSkyld = restOppdaterTilbakekrevingsvedtakMotregning.vurderingAvSkyld,
                varselDato = restOppdaterTilbakekrevingsvedtakMotregning.varselDato,
                heleBeløpetSkalKrevesTilbake = restOppdaterTilbakekrevingsvedtakMotregning.heleBeløpetSkalKrevesTilbake,
            )

        return ResponseEntity.ok(Ressurs.success(oppdatertTilbakekrevingsvedtakMotregning.tilRestTilbakekrevingsvedtakMotregning()))
    }

    @DeleteMapping
    fun slettTilbakekrevingsvedtakMotregning(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett TilbakekrevingsvedtakMotregning",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        tilbakekrevingsvedtakMotregningService.slettTilbakekrevingsvedtakMotregning(behandlingId)

        return ResponseEntity.ok(Ressurs.success("TilbakekrevingsvedtakMotregning for behandling=$behandlingId slettet OK."))
    }

    @Operation(summary = "Henter eksisterende Tilbakekrevingsvedtak motregning pdf.")
    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/pdf"],
    )
    fun hentTilbakekrevingsvedtakMotregningPdf(
        @PathVariable behandlingId: Long,
    ): Ressurs<ByteArray> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Hent TilbakekrevingsvedtakMotregning pdf",
        )

        val vedtakPdf =
            tilbakekrevingsvedtakMotregningService.hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId = behandlingId).vedtakPdf
                ?: throw FunksjonellFeil("Det har ikke blitt opprettet Tilbakekrevingsvedtak motregning pdf for behandling $behandlingId")

        return Ressurs.success(vedtakPdf)
    }

    @Operation(summary = "Oppretter og henter Tilbakekrevingsvedtak motregning pdf.")
    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/pdf"],
    )
    fun opprettOgHentTilbakekrevingsvedtakMotregningPdf(
        @PathVariable behandlingId: Long,
    ): Ressurs<ByteArray> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppretter TilbakekrevingsvedtakMotregning pdf",
        )

        val tilbakekrevingsvedtakMotregningPdf =
            tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(behandlingId).vedtakPdf
                ?: throw Feil("Tilbakekrevingsvedtak motregning pdf ble ikke opprettet for behandling $behandlingId.")

        return Ressurs.success(tilbakekrevingsvedtakMotregningPdf)
    }
}
