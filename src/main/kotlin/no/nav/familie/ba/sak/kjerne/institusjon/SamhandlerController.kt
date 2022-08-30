package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/samhandler")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SamhandlerController(
    private val institusjonService: InstitusjonService
) {

    @GetMapping(path = ["/orgnr/{orgnr}"])
    fun hentSamhandlerDataForOrganisasjon(
        @PathVariable("orgnr") orgNummer: String
    ): Ressurs<SamhandlerInfo> {
        return Ressurs.success(institusjonService.hentSamhandler(orgNummer))
    }

    @PostMapping(path = ["/navn"])
    fun søkSamhandlerinfoFraNavn(
        @RequestBody request: SøkSamhandlerInfoRequest
    ): Ressurs<List<SamhandlerInfo>> {
        return Ressurs.success(institusjonService.søkSamhandlere(request.navn.uppercase()))
    }
}

data class SøkSamhandlerInfoRequest(
    val navn: String
)
