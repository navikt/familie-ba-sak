package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.common.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.EndreSøknadstidspunktRequestDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/registrert-soknadstidspunkt")
@Validated
class RegistrertSøknadstidspunktController(
    private val tilgangService: TilgangService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val registrertSøknadstidspunktService: RegistrertSøknadstidspunktService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
) {
    @GetMapping("/behandling/{behandlingId}")
    fun hentRegistrertSøknadstidspunkt(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<RegistrertSøknadstidspunktDto>>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent søknadstidspunkt",
        )

        return ResponseEntity.ok(Ressurs.success(registrertSøknadstidspunktService.hentForBehandling(behandlingId)))
    }

    @PutMapping("/behandling/{behandlingId}")
    fun endreSøknadstidspunkt(
        @PathVariable behandlingId: Long,
        @RequestBody request: EndreSøknadstidspunktRequestDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "endre søknadstidspunkt",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        validerBehandlingKanRedigeres(behandling)

        endretUtbetalingAndelService.endreSøknadstidspunktOgGenererEtterbetalingsandeler(
            behandling = behandling,
            søknadstidspunktPerPerson = request.søknadstidspunktPerPerson,
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId)))
    }
}
