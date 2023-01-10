package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderRequest
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/skatt")
@ProtectedWithClaims(issuer = "azuread")
class SkatteetatenController(
    private val skatteetatenService: SkatteetatenService,
    private val featureToggleService: FeatureToggleService
) {

    @GetMapping(
        value = ["/personer"],
        produces = ["application/json;charset=UTF-8"]
    )
    fun finnPersonerMedUtvidetBarnetrygd(
        @NotNull
        @RequestParam(value = "aar", required = true)
        aar: String
    ): ResponseEntity<Ressurs<SkatteetatenPersonerResponse>> {
        logger.info("Treff på finnPersonerMedUtvidetBarnetrygd")
        val respons = skatteetatenService.finnPersonerMedUtvidetBarnetrygd(aar)

        return ResponseEntity(Ressurs.success(respons), HttpStatus.valueOf(200))
    }

    @GetMapping(
        value = ["/personer/test"],
        produces = ["application/json;charset=UTF-8"]
    )
    fun finnPersonerMedUtvidetBarnetrygdTest(
        @NotNull
        @RequestParam(value = "aar", required = true)
        aar: String
    ): ResponseEntity<Ressurs<SkatteetatenPersonerResponse>> {
        logger.info("Treff på finnPersonerMedUtvidetBarnetrygdTest")
        val respons = skatteetatenService.finnPersonerMedUtvidetBarnetrygd(aar)
        return ResponseEntity(Ressurs.success(respons), HttpStatus.valueOf(200))
    }

    @PostMapping(
        value = ["/perioder"],
        produces = ["application/json;charset=UTF-8"],
        consumes = ["application/json"]
    )
    fun hentPerioderMedUtvidetBarnetrygd(
        @Valid @RequestBody
        perioderRequest: SkatteetatenPerioderRequest
    ): ResponseEntity<Ressurs<SkatteetatenPerioderResponse>> {
        logger.info("Treff på hentPerioderMedUtvidetBarnetrygd")
        val response = skatteetatenService.finnPerioderMedUtvidetBarnetrygd(perioderRequest.identer, perioderRequest.aar)

        return ResponseEntity(
            Ressurs.Companion.success(response),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping(
        value = ["/perioder/test"],
        produces = ["application/json;charset=UTF-8"],
        consumes = ["application/json"]
    )
    fun hentPerioderMedUtvidetBarnetrygdForMidlertidigTest(
        @Valid @RequestBody
        perioderRequest: SkatteetatenPerioderRequest
    ): ResponseEntity<Ressurs<SkatteetatenPerioderResponse>> {
        logger.info("Treff på hentPerioderMedUtvidetBarnetrygdForMidlertidigTest")
        val response =
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(perioderRequest.identer, perioderRequest.aar)

        return ResponseEntity(
            Ressurs.Companion.success(response),
            HttpStatus.valueOf(200)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SkatteetatenController::class.java)
    }
}
