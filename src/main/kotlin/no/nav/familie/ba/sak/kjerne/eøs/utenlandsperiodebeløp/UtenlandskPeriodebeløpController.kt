package no.nav.familie.ba.sak.kjerne.eøs.utenlandsperiodebeløp

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/differanseberegning/utenlandskperidebeløp")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class UtenlandskPeriodebeløpController(
    private val featureToggleService: FeatureToggleService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PutMapping(path = ["{behandlingId}/{utenlandskPeriodebeløpId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterValutakurs(
        @PathVariable behandlingId: Long,
        @PathVariable utenlandskPeriodebeløpId: Long,
        @RequestBody restUtenlandskPeriodebeløp: RestUtenlandskPeriodebeløp
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        val barnAktører = restUtenlandskPeriodebeløp.barnIdenter.map { personidentService.hentAktør(it) }
        val utenlandskPeriodebeløp = restUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(barnAktører = barnAktører)

        utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(utenlandskPeriodebeløpId, utenlandskPeriodebeløp)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{utenlandskPeriodebeløpId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettValutakurs(
        @PathVariable behandlingId: Long,
        @PathVariable utenlandskPeriodebeløpId: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        utenlandskPeriodebeløpService.slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}
