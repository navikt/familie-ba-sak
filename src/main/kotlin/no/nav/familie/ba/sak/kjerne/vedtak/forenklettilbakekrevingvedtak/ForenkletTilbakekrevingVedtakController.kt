package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingvedtak

import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestForenkletTilbakekrevingVedtak
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterForenkletTilbakekrevingVedtakFritekst
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterForenkletTilbakekrevingVedtakSamtykke
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandling/{behandlingId}/forenklet-tilbakekreving-vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForenkletTilbakekrevingVedtakController(
    val tilgangService: TilgangService,
    val forenkletTilbakekrevingVedtakService: ForenkletTilbakekrevingVedtakService,
) {
    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForenkletTilbakekrevingVedtak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestForenkletTilbakekrevingVedtak?>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Hent ForenkletTilbakekrevingVedtak",
        )
        val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.finnForenkletTilbakekrevingVedtak(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(forenkletTilbakekrevingVedtak?.tilRestForenkletTilbakekrevingVedtak()))
    }

    @PatchMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/fritekst"],
    )
    fun oppdaterFritekstPåForenkletTilbakekrevingVedtak(
        @PathVariable behandlingId: Long,
        @RequestBody restOppdaterForenkletTilbakekrevingVedtakFritekst: RestOppdaterForenkletTilbakekrevingVedtakFritekst,
    ): ResponseEntity<Ressurs<RestForenkletTilbakekrevingVedtak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater fritekst på forenklet tilbakekreving vedtak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val oppdatertForenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.oppdaterFritekstPåForenkletTilbakekrevingVedtak(behandlingId, restOppdaterForenkletTilbakekrevingVedtakFritekst.fritekst)

        return ResponseEntity.ok(Ressurs.success(oppdatertForenkletTilbakekrevingVedtak.tilRestForenkletTilbakekrevingVedtak()))
    }

    @PatchMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/samtykke"],
    )
    fun oppdaterSamtykkePåForenkletTilbakekrevingVedtak(
        @PathVariable behandlingId: Long,
        @RequestBody restOppdaterForenkletTilbakekrevingVedtakSamtykke: RestOppdaterForenkletTilbakekrevingVedtakSamtykke,
    ): ResponseEntity<Ressurs<RestForenkletTilbakekrevingVedtak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater samtykke på forenklet tilbakekreving vedtak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val oppdatertForenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.oppdaterSamtykkePåForenkletTilbakekrevingVedtak(behandlingId, restOppdaterForenkletTilbakekrevingVedtakSamtykke.samtykke)

        return ResponseEntity.ok(Ressurs.success(oppdatertForenkletTilbakekrevingVedtak.tilRestForenkletTilbakekrevingVedtak()))
    }

    @DeleteMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettForenkletTilbakekrevingVedtak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett ForenkletTilbakekrevingVedtak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        forenkletTilbakekrevingVedtakService.slettForenkletTilbakekrevingVedtak(behandlingId)

        return ResponseEntity.ok(Ressurs.success("ForenkletTilbakekrevingVedtak for behandling=$behandlingId slettet OK."))
    }
}
