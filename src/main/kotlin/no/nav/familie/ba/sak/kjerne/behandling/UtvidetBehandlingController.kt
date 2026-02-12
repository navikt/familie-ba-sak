package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class UtvidetBehandlingController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilgangService: TilgangService,
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    ) {
    @GetMapping(path = ["/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtvidetBehandling(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "Henter utvidet behandling",
        )
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }

    @PutMapping(path = ["/{behandlingId}/oppdatert-valutakurs"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtvidetBehandlingMedOppdatertValutakursOgSimulering(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "Henter utvidet behandling med oppdatert valutakurs og simulering ved behov",
        )
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        if (behandling.status == BehandlingStatus.FATTER_VEDTAK) {
            automatiskOppdaterValutakursService.oppdaterValutakurserOgSimulerVedBehov(behandlingId)
        }

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingId)))
    }
}
