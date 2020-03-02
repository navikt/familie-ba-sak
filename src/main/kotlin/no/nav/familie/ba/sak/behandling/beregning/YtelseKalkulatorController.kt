package no.nav.familie.ba.sak.behandling.beregning

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/kalkulator")
@Unprotected // Skal IKKE kreve autentisering
class YtelseKalkulatorController {

    @PostMapping
    fun oppdaterVedtakMedBeregning(@RequestBody personytelser: List<PersonligYtelseForPeriode>): ResponseEntity<YtelseKalkulatorResponse> {

        return ResponseEntity.ok(YtelseKalkulatorResponse(
                totaler = TotalePersonligeYtelser(personligeYtelserMedBeløp =  emptyList(), totalbeløp = 0),
                perioder = emptyList()
        ))
    }
}

data class PersonligYtelse (
        val personident: String,
        val ytelsetype: Ytelsetype,
        val fullYtelse: Boolean = true
)

data class PersonligYtelseForPeriode(

        val stønadFraOgMed: LocalDate,
        val stønadTilOgMed: LocalDate
) {
    @JsonUnwrapped
    lateinit var personligYtelse: PersonligYtelse // Kjipt hack for å unwrappe typen i json'en
}

data class PersonligYtelseMedBeløp(

        val beløp: Int
) {
    @JsonUnwrapped
    lateinit var personligYtelse: PersonligYtelse // Kjipt hack for å unwrappe typen i json'en
}

data class TotalePersonligeYtelser(
        val totalbeløp: Int,
        val personligeYtelserMedBeløp: List<PersonligYtelseMedBeløp>
)

data class MånedensTotalePersonligeYtelser(
        val måned: LocalDate
) {
    @JsonUnwrapped
    lateinit var personligeYtelser: TotalePersonligeYtelser // Kjipt hack for å unwrappe typen i json'en
}

data class YtelseKalkulatorResponse(
        val totaler: TotalePersonligeYtelser,
        val perioder: List<MånedensTotalePersonligeYtelser>
)