package no.nav.familie.ba.sak.bisys

import no.nav.familie.ba.sak.infotrygd.MigreringService
import no.nav.familie.ba.sak.infotrygd.Personident
import no.nav.familie.ba.sak.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/bisys")
@ProtectedWithClaims(issuer = "azuread")
// TODO:
// En ide å gjøre dette endepunktet unprotected og istedenfor inspisere tokenet og sammenligne det med client id fra bisys?
class BisysController(private val bisysService: BisysService) {

    @PostMapping(path = ["/hent-utvidet-barnetrygd"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtvidetBarnetrygd(@RequestBody request: BisysUtvidetBarnetrygdRequest): ResponseEntity<BisysUtvidetBarnetrygdResponse> {
        return ResponseEntity.ok(BisysUtvidetBarnetrygdResponse(bisysService.hentUtvidetBarnetrygd(request.ident, request.fraDato)))
    }
}

class BisysUtvidetBarnetrygdRequest(val ident: String, val fraDato: LocalDate)

class BisysUtvidetBarnetrygdResponse(val perioder: List<UtvidetBarnetrygdPeriode>)

// beløp bør være Int dersom beløpet ikke nå, i fortid og fremtid inneholder øre.
// Ellers bør beløp være BigDecimal.
class UtvidetBarnetrygdPeriode(val stønadstype: BisysStønadstype, val fraMåned: YearMonth, val tomMåned: YearMonth, val beløp: Int)

enum class BisysStønadstype {
    UTVIDET,
    SMÅBARNSTILLEGG
}