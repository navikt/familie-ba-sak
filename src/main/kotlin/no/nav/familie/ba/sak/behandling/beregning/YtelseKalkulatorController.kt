package no.nav.familie.ba.sak.behandling.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/kalkulator")
@Unprotected // Skal IKKE kreve autentisering
class YtelseKalkulatorController {

    @PostMapping
    fun oppdaterVedtakMedBeregning(@RequestBody personytelser: List<PersonligYtelseForPeriode>): ResponseEntity<YtelseKalkulatorResponse> {

        val pyfp = PersonligYtelseForPeriode(
                PersonligYtelse("123123",Ytelsetype.ORDINÆR_BARNETRYGD,true),
                LocalDate.now(),
                LocalDate.now()
        )

        println(pyfp.personligYtelse)

        println(personytelser[0].personligYtelse)

        return ResponseEntity.ok(YtelseKalkulatorResponse(
                totaler = TotalePersonligeYtelser(personligeYtelserMedBeløp =  listOf(
                        PersonligYtelseMedBeløp(personytelser[0].personligYtelse!!,565)
                ), totalbeløp = 0),
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
        @JsonIgnore private val _personligYtelse: PersonligYtelse?,
        val stønadFraOgMed: LocalDate,
        val stønadTilOgMed: LocalDate
) {
    @field:JsonUnwrapped
    val personligYtelse: PersonligYtelse? = _personligYtelse
}

data class PersonligYtelseMedBeløp(
        @JsonIgnore private val _personligYtelse: PersonligYtelse,
        val beløp: Int
) {
    @field:JsonUnwrapped
    val personligYtelse: PersonligYtelse = _personligYtelse
}

data class TotalePersonligeYtelser(
        val totalbeløp: Int,
        val personligeYtelserMedBeløp: List<PersonligYtelseMedBeløp>
)

data class MånedensTotalePersonligeYtelser(
        @JsonIgnore private val _personligeYtelser: TotalePersonligeYtelser,
        val måned: LocalDate
) {
    @JsonUnwrapped
    var personligeYtelser: TotalePersonligeYtelser = _personligeYtelser
}

data class YtelseKalkulatorResponse(
        val totaler: TotalePersonligeYtelser,
        val perioder: List<MånedensTotalePersonligeYtelser>
)