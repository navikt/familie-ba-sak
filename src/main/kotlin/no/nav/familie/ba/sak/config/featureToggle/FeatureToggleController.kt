package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feature")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FeatureToggleController(
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {

    @PostMapping("/er-toggler-enabled", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun erTogglerEnabled(@RequestBody toggles: List<String>): ResponseEntity<Ressurs<Map<String, Boolean>>> {
        return RessursUtils.ok(
            toggles.fold(mutableMapOf()) { acc, toggleId ->
                acc[toggleId] = unleashNextMedContextService.isEnabled(toggleId)
                acc
            },
        )
    }
}
