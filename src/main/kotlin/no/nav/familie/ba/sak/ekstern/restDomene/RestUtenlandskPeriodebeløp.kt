package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.eøs.utenlandsperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.YearMonth

data class RestUtenlandskPeriodebeløp(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val beløp: BigDecimal?,
    val valutakode: String?,
    val intervall: String?
)

fun RestUtenlandskPeriodebeløp.tilUtenlandskPeriodebeløp(barnAktører: List<Aktør>) = UtenlandskPeriodebeløp(
    fom = this.fom,
    tom = this.tom,
    barnAktører = barnAktører.toSet(),
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall
)

fun UtenlandskPeriodebeløp.tilRestUtenlandskPeriodebeløp() = RestUtenlandskPeriodebeløp(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
    beløp = this.beløp,
    valutakode = this.valutakode,
    intervall = this.intervall
)
