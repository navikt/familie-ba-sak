package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class RestValutakurs(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val valutakursdato: LocalDate,
    val valutakode: String,
    val kurs: BigDecimal
)

fun RestValutakurs.tilValutakurs(barnAktører: List<Aktør>) = Valutakurs(
    fom = this.fom,
    tom = this.tom,
    barnAktører = barnAktører.toSet(),
    valutakursdato = this.valutakursdato,
    valutakode = this.valutakode,
    kurs = this.kurs
)
