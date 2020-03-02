package no.nav.familie.ba.sak.behandling.beregning

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/kalkulator")
// Skal IKKE kreve autentisering
@Validated
class YtelseKalkulatorController {

    @PostMapping(path = ["/"])
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
        @JsonUnwrapped
        val personligYtelse: PersonligYtelse,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate
)

data class PersonligYtelseMedBeløp(
        @JsonUnwrapped
        val personligYtelse: PersonligYtelse,
        val beløp: Int
)

data class TotalePersonligeYtelser(
        val totalbeløp: Int,
        val personligeYtelserMedBeløp: List<PersonligYtelseMedBeløp>
)

data class MånedensTotalePersonligeYtelser(
        val måned: LocalDate,
        @JsonUnwrapped
        val personligeYtelser: TotalePersonligeYtelser
)

data class YtelseKalkulatorResponse(
        val totaler: TotalePersonligeYtelser,
        val perioder: List<MånedensTotalePersonligeYtelser>
)