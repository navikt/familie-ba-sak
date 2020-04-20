package no.nav.familie.ba.sak.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.ba.sak.behandling.vedtak.YtelseType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.util.*

@RestController
@RequestMapping("/api/kalkulator")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class YtelseKalkulatorController(
        val satsService: SatsService
) {
    @PutMapping(produces = ["application/json"])
    fun kalkulerYtelserJson(@RequestBody
                            personligeYtelser: List<PersonligYtelseForPeriode>): ResponseEntity<Ressurs<YtelseKalkulatorResponse>> {

        val response = kalkulerYtelser(personligeYtelser)

        return ResponseEntity.ok(Ressurs.success(response))
    }

    @PutMapping(produces = ["text/html"])
    fun kalkulerYtelserHtml(@RequestBody personligeYtelser: List<PersonligYtelseForPeriode>): ResponseEntity<String> {

        val response = kalkulerYtelser(personligeYtelser)

        return ResponseEntity.ok(HtmlTabell.lagTabell(response))
    }

    private fun kalkulerYtelser(personligeYtelser: List<PersonligYtelseForPeriode>): YtelseKalkulatorResponse {
        var gjeldendeMåned = personligeYtelser.map { it.stønadFom }.min()!!
        val størsteTomMåned = personligeYtelser.map { it.stønadTom }.max()!!

        val persoligeYtelserMedSats = personligeYtelser
                .flatMap { lagPersonligeYtelserMedSatsForPerioder(it) }

        val alleMånedersTotalePersonligeYtelser: MutableList<MånedensTotalePersonligeYtelser> = ArrayList()

        while (gjeldendeMåned < størsteTomMåned) {

            val personligeYtelserMedBeløpDenneMåned = persoligeYtelserMedSats
                    .filter { it.stønadFom <= gjeldendeMåned && it.stønadTom >= gjeldendeMåned }
                    .map { PersonligYtelseMedBeløp(it.personligYtelse!!, it.beløp!!) }

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

    private fun lagPersonligeYtelserMedSatsForPerioder(periodeYtelse: PersonligYtelseForPeriode): List<PersonligYtelseForPeriode> {

        val personligeYtelse = periodeYtelse.personligYtelse!!

        val divisor = if (personligeYtelse.halvYtelse) 2 else 1
        val satsType = YtelseSatsMapper.map(personligeYtelse.ytelseType, personligeYtelse.alder)

        val beløpsperioder: List<SatsService.BeløpPeriode>? = satsType?.let {
            satsService.hentGyldigSatsFor(it, periodeYtelse.stønadFom, periodeYtelse.stønadTom, YearMonth.now())
        }
        return beløpsperioder
                       ?.map { PersonligYtelseForPeriode(personligeYtelse, it.beløp / divisor, it.fraOgMed, it.tilOgMed) }
               ?: listOf(PersonligYtelseForPeriode(personligeYtelse, periodeYtelse.beløp, periodeYtelse.stønadFom, periodeYtelse.stønadTom))
    }
}

data class PersonligYtelse(
        val personident: String,
        val ytelseType: YtelseType,
        val alder: Int? = null,
        val halvYtelse: Boolean = false
)

data class PersonligYtelseForPeriode(
        @JsonIgnore private val _personligYtelse: PersonligYtelse?,
        val beløp: Int?,
        val stønadFom: YearMonth,
        val stønadTom: YearMonth
) {

    @field:JsonUnwrapped
    val personligYtelse: PersonligYtelse? = _personligYtelse

    constructor(personident: String,
                ytelseType: YtelseType,
                fullYtelse: Boolean,
                stønadFraOgMed: YearMonth,
                stønadTilOgMed: YearMonth,
                alder: Int?=null) :
            this(PersonligYtelse(personident, ytelseType, alder, fullYtelse), null, stønadFraOgMed, stønadTilOgMed)
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
            builder.append(header.map { "${it.personident} ${it.ytelseType} ${tilProsent(!it.halvYtelse)}" }
                                   .joinToString("</th><th>", "<tr><th>Periode</th><th>Total</th><th>", "</th></tr>"))
            builder.append(personYtelseSum.joinToString("</td><td>", "<tr><td>Alle</td><td>$totalbeløp</td><td>", "</td></tr>"))

            response.perioder
                    .map { lagPeriodeRad(it.måned, it.personligeYtelser, header) }
                    .forEach { builder.append(it) }

            builder.append("</table>")
            return builder.toString()
        }

        private fun tilProsent(fullYtelse: Boolean) = if (fullYtelse) "100%" else "50%"

        private fun lagPeriodeRad(måned: YearMonth,
                                  personligeYtelser: TotalePersonligeYtelser,
                                  header: List<PersonligYtelse>): String {
            val builder = StringBuilder()

            val personligYtelseTilBeløpMap =
                    personligeYtelser.personligeYtelserMedBeløp.map { it.personligYtelse to it.beløp }.toMap()

            builder.append("<tr>")
            builder.append("<td>${måned}</td>")
            builder.append("<td>${personligeYtelser.totalbeløp}</td>")

            header.map { personligYtelseTilBeløpMap.getOrDefault(it, 0) }
                    .forEach { builder.append("<td>${it}</td>") }

            builder.append("</tr>")

            return builder.toString()
        }
    }
}

