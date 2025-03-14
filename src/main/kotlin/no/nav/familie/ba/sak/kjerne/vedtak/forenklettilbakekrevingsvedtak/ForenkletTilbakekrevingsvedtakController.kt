package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestForenkletTilbakekrevingsvedtak
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterForenkletTilbakekrevingsvedtakFritekst
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterForenkletTilbakekrevingsvedtakSamtykke
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
@RequestMapping("/api/behandling/{behandlingId}/forenklet-tilbakekrevingsvedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ForenkletTilbakekrevingsvedtakController(
    val tilgangService: TilgangService,
    val forenkletTilbakekrevingsvedtakService: ForenkletTilbakekrevingsvedtakService,
) {
    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForenkletTilbakekrevingsvedtak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestForenkletTilbakekrevingsvedtak?>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Hent ForenkletTilbakekrevingsvedtak",
        )
        val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.finnForenkletTilbakekrevingsvedtak(behandlingId = behandlingId)

        return ResponseEntity.ok(Ressurs.success(forenkletTilbakekrevingsvedtak?.tilRestForenkletTilbakekrevingsvedtak()))
    }

    @PatchMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/fritekst"],
    )
    fun oppdaterFritekstPåForenkletTilbakekrevingsvedtak(
        @PathVariable behandlingId: Long,
        @RequestBody restOppdaterForenkletTilbakekrevingsvedtakFritekst: RestOppdaterForenkletTilbakekrevingsvedtakFritekst,
    ): ResponseEntity<Ressurs<RestForenkletTilbakekrevingsvedtak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater fritekst på forenklet tilbakekrevingsvedtak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val oppdatertForenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.oppdaterFritekstPåForenkletTilbakekrevingsvedtak(behandlingId, restOppdaterForenkletTilbakekrevingsvedtakFritekst.fritekst)

        return ResponseEntity.ok(Ressurs.success(oppdatertForenkletTilbakekrevingsvedtak.tilRestForenkletTilbakekrevingsvedtak()))
    }

    @PatchMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/samtykke"],
    )
    fun oppdaterSamtykkePåForenkletTilbakekrevingsvedtak(
        @PathVariable behandlingId: Long,
        @RequestBody restOppdaterForenkletTilbakekrevingsvedtakSamtykke: RestOppdaterForenkletTilbakekrevingsvedtakSamtykke,
    ): ResponseEntity<Ressurs<RestForenkletTilbakekrevingsvedtak>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdater samtykke på forenklet tilbakekrevingsvedtak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        val oppdatertForenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.oppdaterSamtykkePåForenkletTilbakekrevingsvedtak(behandlingId, restOppdaterForenkletTilbakekrevingsvedtakSamtykke.samtykke)

        return ResponseEntity.ok(Ressurs.success(oppdatertForenkletTilbakekrevingsvedtak.tilRestForenkletTilbakekrevingsvedtak()))
    }

    @DeleteMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun slettForenkletTilbakekrevingsvedtak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Slett ForenkletTilbakekrevingsvedtak",
        )
        tilgangService.validerKanRedigereBehandling(behandlingId)

        forenkletTilbakekrevingsvedtakService.slettForenkletTilbakekrevingsvedtak(behandlingId)

        return ResponseEntity.ok(Ressurs.success("ForenkletTilbakekrevingsvedtak for behandling=$behandlingId slettet OK."))
    }

    @GetMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/pdf"],
    )
    fun hentForenkletTilbakekrevingsvedtakPdf(
        @PathVariable behandlingId: Long,
    ): Ressurs<ByteArray> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Hent ForenkletTilbakekrevingsvedtak pdf",
        )
        val forenkletTilbakekrevingsvedtak =
            forenkletTilbakekrevingsvedtakService.finnForenkletTilbakekrevingsvedtak(behandlingId = behandlingId)
                ?: throw FunksjonellFeil("Det er ikke opprettet forenklet tilbakekrevingsvedtak for behandling $behandlingId.")

        val vedtakPdf =
            forenkletTilbakekrevingsvedtak.vedtakPdf
                ?: throw FunksjonellFeil("Det har ikke blitt opprettet forenklet tilbakekrevingsvedtak pdf for behandling $behandlingId")

        return Ressurs.success(vedtakPdf)
    }

    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        path = ["/pdf"],
    )
    fun opprettOgHentForenkletTilbakekrevingsvedtakPdf(
        @PathVariable behandlingId: Long,
    ): Ressurs<ByteArray> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppretter ForenkletTilbakekrevingsvedtak pdf",
        )

        val forenkletTilbakekrevingsvedtakPdf =
            forenkletTilbakekrevingsvedtakService.opprettOgLagreForenkletTilbakekrevingsvedtakPdf(behandlingId).vedtakPdf
                ?: throw Feil("ForenkletTilbakekrevingsvedtak pdf ikke opprettet")

        return Ressurs.success(forenkletTilbakekrevingsvedtakPdf)
    }
}
