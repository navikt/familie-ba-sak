package no.nav.familie.ba.sak.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

val nbLocale = Locale("nb", "Norway")

fun LocalDate.tilKortString() = this.format(DateTimeFormatter.ofPattern("dd.MM.YY", nbLocale))
fun LocalDate.tilDagMånedÅr() = this.format(DateTimeFormatter.ofPattern("dd. MMMM YYYY", nbLocale))
fun LocalDate.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM YYYY", nbLocale))

fun LocalDate.sisteDagIForrigeMåned(): LocalDate {
    val sammeDagForrigeMåned = this.minusMonths(1)
    return sammeDagForrigeMåned.sisteDagIMåned()
}

fun LocalDate.sisteDagIMåned(): LocalDate {
    return YearMonth.from(this).atEndOfMonth()
}

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)

private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
