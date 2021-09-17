package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderRequest
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
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
import java.time.LocalDateTime
import javax.validation.Valid
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/skatt")
@ProtectedWithClaims(issuer = "azuread")
class SkatteetatenController(private val skatteetatenService: SkatteetatenService,
                             private val featureToggleService: FeatureToggleService) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    @GetMapping(
        value = ["/personer"],
        produces = ["application/json;charset=UTF-8"]
    )
    fun finnPersonerMedUtvidetBarnetrygd(
        @NotNull @RequestParam(value = "aar", required = true) aar: String
    ): ResponseEntity<Ressurs<SkatteetatenPersonerResponse>> {
        logger.info("Treff på finnPersonerMedUtvidetBarnetrygd")
        val respons = if (featureToggleService.isEnabled(FeatureToggleConfig.SKATTEETATEN_API_STUB)) {
            SkatteetatenPersonerResponse(listOf(SkatteetatenPerson("12345678901", LocalDateTime.now())))
        } else {
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(aar)
        }
        return ResponseEntity(Ressurs.success(respons), HttpStatus.valueOf(200))
    }


    @PostMapping(
        value = ["/perioder"],
        produces = ["application/json;charset=UTF-8"],
        consumes = ["application/json"]
    )
    fun hentPerioderMedUtvidetBarnetrygd(
        @Valid @RequestBody perioderRequest: SkatteetatenPerioderRequest
    ): ResponseEntity<Ressurs<SkatteetatenPerioderResponse>> {
        logger.info("Treff på hentPerioderMedUtvidetBarnetrygd")
        val response = if (featureToggleService.isEnabled(FeatureToggleConfig.SKATTEETATEN_API_STUB)) {
            SkatteetatenPerioderResponse(listOf(SkatteetatenPerioder("01017000110", LocalDateTime.now(), perioder = listOf(SkatteetatenPeriode("2020-02", SkatteetatenPeriode.MaxDelingsprosent._50, tomMaaned = "2022-12")))))
        } else {
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(perioderRequest.identer, perioderRequest.aar)
        }
        return ResponseEntity(
            Ressurs.Companion.success(response),
            HttpStatus.valueOf(200)
        )
    }
}