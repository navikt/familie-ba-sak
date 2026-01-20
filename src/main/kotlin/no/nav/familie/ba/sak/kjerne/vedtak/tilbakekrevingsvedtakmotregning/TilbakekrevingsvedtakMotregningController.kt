package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.OppdaterTilbakekrevingsvedtakMotregningDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
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
    val utvidetBehandlingService: UtvidetBehandlingService,
) {
    @PatchMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterTilbakekrevingsvedtakMotregning(
        @PathVariable behandlingId: Long,
        @RequestBody oppdaterTilbakekrevingsvedtakMotregningDto: OppdaterTilbakekrevingsvedtakMotregningDto,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater tilbakekrevingsvedtak motregning",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
            behandlingId = behandlingId,
            samtykke = oppdaterTilbakekrevingsvedtakMotregningDto.samtykke,
            årsakTilFeilutbetaling = oppdaterTilbakekrevingsvedtakMotregningDto.årsakTilFeilutbetaling,
            vurderingAvSkyld = oppdaterTilbakekrevingsvedtakMotregningDto.vurderingAvSkyld,
            varselDato = oppdaterTilbakekrevingsvedtakMotregningDto.varselDato,
            heleBeløpetSkalKrevesTilbake = oppdaterTilbakekrevingsvedtakMotregningDto.heleBeløpetSkalKrevesTilbake,
        )

        val restUtvidetBehandling = utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(restUtvidetBehandling))
    }

    @DeleteMapping
    fun slettTilbakekrevingsvedtakMotregning(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett TilbakekrevingsvedtakMotregning",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        tilbakekrevingsvedtakMotregningService.slettTilbakekrevingsvedtakMotregning(behandlingId)

        val restUtvidetBehandling = utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(restUtvidetBehandling))
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
