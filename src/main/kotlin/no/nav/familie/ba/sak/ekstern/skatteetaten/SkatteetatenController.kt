package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.ba.skatteetaten.model.Periode
import no.nav.familie.ba.skatteetaten.model.Perioder
import no.nav.familie.ba.skatteetaten.model.PerioderRequest
import no.nav.familie.ba.skatteetaten.model.PerioderResponse
import no.nav.familie.ba.skatteetaten.model.Person
import no.nav.familie.ba.skatteetaten.model.PersonerResponse
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.validation.Valid
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/skatt")
@ProtectedWithClaims(issuer = "azuread")
class SkatteetatenController {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    @GetMapping(
        value = ["/personer"],
        produces = ["application/json;charset=UTF-8"]
    )
    fun finnPersonerMedUtvidetBarnetrygd(
        @NotNull @RequestParam(value = "aar", required = true) aar: kotlin.String
    ): ResponseEntity<Ressurs<PersonerResponse>> {
        logger.info("Treff på finnPersonerMedUtvidetBarnetrygd")
        val stubbedResponse = PersonerResponse(listOf(Person("12345678901", OffsetDateTime.now())))
        return ResponseEntity(Ressurs.success(stubbedResponse), HttpStatus.valueOf(200))
    }


    @PostMapping(
        value = ["/perioder"],
        produces = ["application/json;charset=UTF-8"],
        consumes = ["application/json"]
    )
    fun hentPerioderMedUtvidetBarnetrygd(
        @Valid @RequestBody perioderRequest: PerioderRequest
    ): ResponseEntity<Ressurs<PerioderResponse>> {
        logger.info("Treff på hentPerioderMedUtvidetBarnetrygd")
        val stubbedResponse =  PerioderResponse(listOf(Perioder("01017000110", OffsetDateTime.now(), perioder = listOf(Periode("2020-02", Periode.MaxDelingsprosent._50, tomMaaned = "2022-12")))))
        return ResponseEntity(
            Ressurs.Companion.success(stubbedResponse),
            HttpStatus.valueOf(200)
        )
    }
}