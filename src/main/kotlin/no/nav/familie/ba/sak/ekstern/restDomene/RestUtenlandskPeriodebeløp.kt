package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.erSkuddår
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.konverterBeløpTilMånedlig
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import java.math.BigDecimal
import java.time.YearMonth

data class RestUtenlandskPeriodebeløp(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val beløp: BigDecimal?,
    val valutakode: String?,
    val intervall: Intervall?,
    val kalkulertMånedligBeløp: BigDecimal?,
    override val status: UtfyltStatus = UtfyltStatus.IKKE_UTFYLT
) : AbstractUtfyltStatus<RestUtenlandskPeriodebeløp>() {
    override fun medUtfyltStatus(): RestUtenlandskPeriodebeløp {
        return this.copy(status = utfyltStatus(finnAntallUtfylt(listOf(this.beløp, this.valutakode, this.intervall)), 3))
    }
}

fun RestUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(barnAktører: List<Aktør>, eksisterendeUtenlandskPeriodebeløp: UtenlandskPeriodebeløp) = UtenlandskPeriodebeløp(
    fom = this.fom,
    tom = this.tom,
    barnAktører = barnAktører.toSet(),
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall,
    utbetalingsland = eksisterendeUtenlandskPeriodebeløp.utbetalingsland,
    kalkulertMånedligBeløp = this.tilKalkulertMånedligBeløp()
)

fun RestUtenlandskPeriodebeløp.tilKalkulertMånedligBeløp() =
    if (this.intervall != null && this.beløp != null && this.fom != null) {
        this.intervall.konverterBeløpTilMånedlig(
            this.beløp, this.fom.tilTidspunkt().erSkuddår()
        )
    } else null

fun UtenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp() = RestUtenlandskPeriodebeløp(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall,
    kalkulertMånedligBeløp = this.kalkulertMånedligBeløp
).medUtfyltStatus()
