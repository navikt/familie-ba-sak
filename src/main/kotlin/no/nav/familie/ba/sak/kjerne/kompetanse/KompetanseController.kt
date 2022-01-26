package no.nav.familie.ba.sak.kjerne.kompetanse

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.kompetanse.domene.Kompetanse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kompetanse")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KompetanseController(
    private val featureToggleService: FeatureToggleService,
    private val kompetanseService: KompetanseService = KompetanseService(MockKompetanseRepository())
) {
    @GetMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKompetanser(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<Collection<Kompetanse>>> {
        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.ok(Ressurs.success(kompetanseService.hentKompetanser(behandlingId)))
        else
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    @PutMapping(path = ["{behandlingId}/{kompetanseId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKompetanse(
        @PathVariable behandlingId: Long,
        @PathVariable kompetanseId: Long,
        @RequestBody kompetanse: Kompetanse
    ): ResponseEntity<Ressurs<Collection<Kompetanse>>> {
        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS))
            return ResponseEntity.ok(Ressurs.success(kompetanseService.oppdaterKompetanse(kompetanse)))
        else
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }
}
