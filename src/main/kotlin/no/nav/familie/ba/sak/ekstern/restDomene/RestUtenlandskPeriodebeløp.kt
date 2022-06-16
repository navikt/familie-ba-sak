package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.YearMonth
import javax.validation.constraints.DecimalMin

data class RestUtenlandskPeriodebeløp(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    @field:DecimalMin(value = "0.0", message = "Beløp kan ikke være negativt.") val beløp: BigDecimal?,
    val valutakode: String?,
    val intervall: String?,
    val utbetalingsland: String?,
    override val status: UtfyltStatus = UtfyltStatus.IKKE_UTFYLT
) : AbstractUtfyltStatus<RestUtenlandskPeriodebeløp>() {
    override fun medUtfyltStatus(): RestUtenlandskPeriodebeløp {
        return this.copy(status = utfyltStatus(finnAntallUtfylt(listOf(this.beløp, this.valutakode, this.intervall)), 3))
    }
}

fun RestUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(barnAktører: List<Aktør>) = UtenlandskPeriodebeløp(
    fom = this.fom,
    tom = this.tom,
    barnAktører = barnAktører.toSet(),
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall,
    utbetalingsland = this.utbetalingsland
)

fun UtenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp() = RestUtenlandskPeriodebeløp(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall,
    utbetalingsland = this.utbetalingsland
).medUtfyltStatus()
