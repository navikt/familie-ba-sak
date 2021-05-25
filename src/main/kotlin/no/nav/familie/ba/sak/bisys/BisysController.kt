package no.nav.familie.ba.sak.bisys

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/bisys")
@ProtectedWithClaims(issuer = "azuread")
class BisysController {
    @PostMapping(path = ["/hent-utvidet-barnetrygd"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtvidetBarnetrygd(@RequestBody request: BisysUtvidetBarnetrygdRequest): ResponseEntity<BisysUtvidetBarnetrygdResponse> {
        return ResponseEntity.ok(BisysUtvidetBarnetrygdResponse(listOf(
            UtvidetBarnetrygdPeriode(
                BisysStønadstype.UTVIDET,
                YearMonth.of(2020, 1),
                YearMonth.of(2020, 1),
                1024))))
    }
}

class BisysUtvidetBarnetrygdRequest(val ident: String, val fraDato: LocalDate)
class BisysUtvidetBarnetrygdResponse(val perioder: List<UtvidetBarnetrygdPeriode>)
class UtvidetBarnetrygdPeriode(val stønadstype: BisysStønadstype, val fomMåned: YearMonth, val tomMåned: YearMonth, val beløp: Int)
enum class BisysStønadstype { UTVIDET, SMÅBARNSTILLEGG }