package no.nav.familie.ba.sak.behandling.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.util.ArrayList

@RestController
@RequestMapping("/api/kalkulator")
@Unprotected // Skal IKKE kreve autentisering
class YtelseKalkulatorController {

    @PutMapping(produces = ["application/json"])
    fun kalkulerYtelserJson(@RequestBody personligeYtelser: List<PersonligYtelseForPeriode>): ResponseEntity<YtelseKalkulatorResponse> {

        val response = kalkulerYtelser(personligeYtelser)

        return ResponseEntity.ok(response)
    }

    @PutMapping(produces = ["text/html"])
    fun kalkulerYtelserHtml(@RequestBody personligeYtelser: List<PersonligYtelseForPeriode>): ResponseEntity<String> {

        val response = kalkulerYtelser(personligeYtelser)

        return ResponseEntity.ok(HtmlTabell.lagTabell(response))
    }

    private fun kalkulerYtelser(personligeYtelser: List<PersonligYtelseForPeriode>): YtelseKalkulatorResponse {
        var gjeldendeMåned = personligeYtelser.map { it.stønadFraOgMed }.min()!!
        val størsteTomMåned = personligeYtelser.map { it.stønadTilOgMed }.max()!!

        val alleMånedersTotalePersonligeYtelser: MutableList<MånedensTotalePersonligeYtelser> = ArrayList()

        while (gjeldendeMåned < størsteTomMåned) {

            val personligeYtelserMedBeløpDenneMåned = personligeYtelser
                    .filter { it.stønadFraOgMed <= gjeldendeMåned && it.stønadTilOgMed >= gjeldendeMåned }
                    .map { PersonligYtelseMedBeløp(it.personligYtelse!!, dummySatsService(it.personligYtelse!!, it.beløp)) }

            val sumYtelserDenneMåned = personligeYtelserMedBeløpDenneMåned.map { it.beløp }.sum()

            val totalePersonligYtelserDenneMåned =
                    TotalePersonligeYtelser(sumYtelserDenneMåned, personligeYtelserMedBeløpDenneMåned)

            val månedensTotalePersonligeYtelser =
                    MånedensTotalePersonligeYtelser(totalePersonligYtelserDenneMåned, gjeldendeMåned)

            alleMånedersTotalePersonligeYtelser.add(månedensTotalePersonligeYtelser)

            gjeldendeMåned = gjeldendeMåned.plusMonths(1)
        }

        val personligeYtelserSumAlleMåneder = alleMånedersTotalePersonligeYtelser
                .flatMap { it.personligeYtelser.personligeYtelserMedBeløp }
                .groupingBy { it.personligYtelse }.fold(0, { sum, ytelse -> sum + ytelse.beløp })
                .map { PersonligYtelseMedBeløp(it.key, it.value) }

        val sumYtelser = personligeYtelserSumAlleMåneder.map { it.beløp }.sum();

        val totalePersonligeYtelser = TotalePersonligeYtelser(sumYtelser, personligeYtelserSumAlleMåneder)

        val response = YtelseKalkulatorResponse(totalePersonligeYtelser, alleMånedersTotalePersonligeYtelser)
        return response
    }

    fun dummySatsService(personligeYtelse: PersonligYtelse, fastsattBeløp: Int?): Int {
        return when (personligeYtelse.ytelsetype) {
            Ytelsetype.UTVIDET_BARNETRYGD -> if(personligeYtelse.fullYtelse) 1054 else 527
            Ytelsetype.ORDINÆR_BARNETRYGD -> if(personligeYtelse.fullYtelse) 1054 else 527
            Ytelsetype.SMÅBARNSTILLEGG -> if(personligeYtelse.fullYtelse) 660 else 330
            else -> fastsattBeløp ?: 0
        }
    }
}

data class PersonligYtelse (
        val personident: String,
        val ytelsetype: Ytelsetype,
        val fullYtelse: Boolean = true
)

data class PersonligYtelseForPeriode(
        @JsonIgnore private val _personligYtelse: PersonligYtelse?,
        val beløp: Int?,
        val stønadFraOgMed: YearMonth,
        val stønadTilOgMed: YearMonth
) {
    @field:JsonUnwrapped
    val personligYtelse: PersonligYtelse? = _personligYtelse

    constructor(personident: String, ytelsetype: Ytelsetype,fullYtelse: Boolean,stønadFraOgMed: YearMonth,stønadTilOgMed: YearMonth) :
            this(PersonligYtelse(personident,ytelsetype,fullYtelse), null, stønadFraOgMed, stønadTilOgMed)
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
        val måned: YearMonth
) {
    @JsonUnwrapped
    var personligeYtelser: TotalePersonligeYtelser = _personligeYtelser
}

data class YtelseKalkulatorResponse(
        val totaler: TotalePersonligeYtelser,
        val perioder: List<MånedensTotalePersonligeYtelser>
)


private class HtmlTabell {

    companion object {
        fun lagTabell(response: YtelseKalkulatorResponse): String {
            val header = response.totaler.personligeYtelserMedBeløp.map { it.personligYtelse }
            val personYtelseSum = response.totaler.personligeYtelserMedBeløp.map { it.beløp }
            val totalbeløp = response.totaler.totalbeløp

            val builder = StringBuilder()

            builder.append("<table>")
            builder.append(header
                                   .map { "${it.personident} ${it.ytelsetype} ${tilProsent(it.fullYtelse)}" }
                                   .joinToString("</th><th>", "<tr><th>Periode</th><th>Total</th><th>", "</th></tr>"))
            builder.append(personYtelseSum.joinToString("</td><td>", "<tr><td>Alle</td><td>$totalbeløp</td><td>", "</td></tr>"))

            response.perioder
                    .map { lagPeriodeRad(it.måned, it.personligeYtelser, header) }
                    .forEach { builder.append(it) }

            builder.append("</table>")
            return builder.toString()
        }

        private fun tilProsent(fullYtelse: Boolean) = if(fullYtelse) "100%" else "50%"

        private fun lagPeriodeRad(måned: YearMonth, personligeYtelser: TotalePersonligeYtelser, header: List<PersonligYtelse>): String {
            val builder = StringBuilder()

            val personligYtelseTilBeløpMap = personligeYtelser.personligeYtelserMedBeløp.map { it.personligYtelse to it.beløp }.toMap()

            builder.append("<tr>")
            builder.append("<td>${måned}</td>")
            builder.append("<td>${personligeYtelser.totalbeløp}</td>")

            header.map { personligYtelseTilBeløpMap.getOrDefault(it,0) }
                    .forEach { builder.append("<td>${it}</td>") }

            builder.append("</tr>")

            return builder.toString()
        }
    }
}

