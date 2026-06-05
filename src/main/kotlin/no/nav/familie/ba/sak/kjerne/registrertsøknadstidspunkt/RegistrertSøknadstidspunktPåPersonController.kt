package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.EndreSøknadstidspunktRequestDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktPåPersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilRegistrertSøknadstidspunkt
import no.nav.familie.ba.sak.ekstern.restDomene.tilRegistrertSøknadstidspunktPåPersonDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
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
@RequestMapping("/api/registrert-soknadstidspunkt-paa-person")
@Validated
class RegistrertSøknadstidspunktPåPersonController(
    private val tilgangService: TilgangService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val registrertSøknadstidspunktService: RegistrertSøknadstidspunktPåPersonService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val featureToggleService: FeatureToggleService,
) {
    @GetMapping("/behandling/{behandlingId}")
    fun hentRegistrertSøknadstidspunktPåPersoner(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<List<RegistrertSøknadstidspunktPåPersonDto>>> {
        validerAtRegistreringAvSøknadstidspunktErAktivert()
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent søknadstidspunkt",
        )

        validerAtBehandlingErSøknadsbehandling(behandlingHentOgPersisterService.hent(behandlingId))

        val registrerteSøknadstidspunkt =
            registrertSøknadstidspunktService
                .hentForBehandling(behandlingId)
                .map { it.tilRegistrertSøknadstidspunktPåPersonDto() }

        return ResponseEntity.ok(Ressurs.success(registrerteSøknadstidspunkt))
    }

    @PutMapping("/behandling/{behandlingId}")
    fun endreSøknadstidspunkter(
        @PathVariable behandlingId: Long,
        @RequestBody request: EndreSøknadstidspunktRequestDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        validerAtRegistreringAvSøknadstidspunktErAktivert()
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.UPDATE)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "endre søknadstidspunkt",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        validerBehandlingKanRedigeres(behandling)
        validerAtBehandlingErSøknadsbehandling(behandling)

        endretUtbetalingAndelService.endreSøknadstidspunktOgGenererEtterbetalingsandeler(
            behandling = behandling,
            søknadstidspunktPerPerson = request.søknadstidspunktPerPerson.map { it.tilRegistrertSøknadstidspunkt() },
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId)))
    }

    private fun validerAtRegistreringAvSøknadstidspunktErAktivert() {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_REGISTRERE_SØKNADSTIDSPUNKT_PÅ_PERSON)) {
            throw FunksjonellFeil("Registrering av søknadstidspunkt er ikke aktivert.")
        }
    }

    private fun validerAtBehandlingErSøknadsbehandling(behandling: Behandling) {
        if (behandling.opprettetÅrsak != BehandlingÅrsak.SØKNAD) {
            throw FunksjonellFeil("Søknadstidspunkt kan kun registreres for søknadsbehandlinger.")
        }
    }
}
