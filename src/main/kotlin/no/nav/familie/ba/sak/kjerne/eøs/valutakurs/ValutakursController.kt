package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestValutakurs
import no.nav.familie.ba.sak.ekstern.restDomene.tilValutakurs
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
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
import java.time.LocalDate

@RestController
@RequestMapping("/api/differanseberegning/valutakurs")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ValutakursController(
    private val featureToggleService: FeatureToggleService,
    private val valutakursService: ValutakursService,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val ecbService: ECBService
) {
    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterValutakurs(
        @PathVariable behandlingId: Long,
        @RequestBody restValutakurs: RestValutakurs
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        val barnAktører = restValutakurs.barnIdenter.map { personidentService.hentAktør(it) }

        if (skalManueltSetteValutakurs(restValutakurs)) {
            valutakursService.oppdaterValutakurs(BehandlingId(behandlingId), restValutakurs.tilValutakurs(barnAktører))
        } else {
            val valutakurs = oppdaterValutakursMedKursFraECB(restValutakurs, restValutakurs.tilValutakurs(barnAktører = barnAktører))
            valutakursService.oppdaterValutakurs(BehandlingId(behandlingId), valutakurs)
        }

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    @DeleteMapping(path = ["{behandlingId}/{valutakursId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettValutakurs(
        @PathVariable behandlingId: Long,
        @PathVariable valutakursId: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }

        valutakursService.slettValutakurs(valutakursId)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }

    private fun oppdaterValutakursMedKursFraECB(restValutakurs: RestValutakurs, valutakurs: Valutakurs) =
        if (valutakursErEndret(restValutakurs, valutakursService.hentValutakurs(restValutakurs.id))) {
            valutakurs.copy(kurs = ecbService.hentValutakurs(restValutakurs.valutakode!!, restValutakurs.valutakursdato!!))
        } else valutakurs

    /**
     * Sjekker om valuta er Islandske Kroner og kursdato er før 01.02.2018
     */
    private fun skalManueltSetteValutakurs(restValutakurs: RestValutakurs): Boolean {
        return restValutakurs.valutakursdato != null && restValutakurs.valutakode == "ISK" && restValutakurs.valutakursdato.isBefore(LocalDate.of(2018, 2, 1))
    }

    /**
     * Sjekker om *restValutakurs* inneholder nødvendige verdier og sammenligner disse med *eksisterendeValutakurs*
     */
    private fun valutakursErEndret(restValutakurs: RestValutakurs, eksisterendeValutakurs: Valutakurs): Boolean {
        return restValutakurs.valutakode != null && restValutakurs.valutakursdato != null && (eksisterendeValutakurs.valutakursdato != restValutakurs.valutakursdato || eksisterendeValutakurs.valutakode != restValutakurs.valutakode)
    }
}
