package no.nav.familie.ba.sak.ekstern.bisys

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/bisys")
@ProtectedWithClaims(issuer = "azuread")
class BisysController(private val bisysService: BisysService) {
    @PostMapping(path = ["/hent-utvidet-barnetrygd"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtvidetBarnetrygd(@RequestBody request: BisysUtvidetBarnetrygdRequest): ResponseEntity<BisysUtvidetBarnetrygdResponse> {
        return ResponseEntity.ok(bisysService.hentUtvidetBarnetrygd(request.personIdent, request.fraDato))
    }
}

class BisysUtvidetBarnetrygdRequest(val personIdent: String, val fraDato: LocalDate)
class BisysUtvidetBarnetrygdResponse(val perioder: List<UtvidetBarnetrygdPeriode>)
class UtvidetBarnetrygdPeriode(val stønadstype: BisysStønadstype, val fomMåned: YearMonth, val tomMåned: YearMonth?, val beløp: Double)
enum class BisysStønadstype { UTVIDET, SMÅBARNSTILLEGG }