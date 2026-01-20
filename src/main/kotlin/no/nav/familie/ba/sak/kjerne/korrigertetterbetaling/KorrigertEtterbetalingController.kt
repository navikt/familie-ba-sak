package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import no.nav.familie.ba.sak.common.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.KorrigertEtterbetalingDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilKorrigertEtterbetalingDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
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
@RequestMapping("/api/korrigertetterbetaling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KorrigertEtterbetalingController(
    private val tilgangService: TilgangService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettKorrigertEtterbetalingPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody korrigertEtterbetalingRequest: KorrigertEtterbetalingRequest,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.CREATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Opprett korrigert etterbetaling",
        )
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        validerBehandlingKanRedigeres(behandling)

        val korrigertEtterbetaling = korrigertEtterbetalingRequest.tilKorrigertEtterbetaling(behandling)

        korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId)))
    }

    @GetMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleKorrigerteEtterbetalingPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<KorrigertEtterbetalingDto>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Henter korrigert etterbetaling",
        )

        val korrigerteEtterbetalinger =
            korrigertEtterbetalingService
                .finnAlleKorrigeringerPåBehandling(behandlingId)
                .map { it.tilKorrigertEtterbetalingDto() }

        return ResponseEntity.ok(Ressurs.success(korrigerteEtterbetalinger))
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settKorrigertEtterbetalingTilInaktivPåBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "Oppdaterer korrigert etterbetaling",
        )
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        validerBehandlingKanRedigeres(behandling)

        korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }
}
