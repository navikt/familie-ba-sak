package no.nav.familie.ba.sak.common

import java.time.LocalDate
import java.time.ZoneId
import java.util.*

fun LocalDate.sisteDagIForrigeMåned() : LocalDate {
    val sammeDagForrigeMåned = this.minusMonths(1)
    return sammeDagForrigeMåned.sisteDagIMåned()
}

fun LocalDate.sisteDagIMåned() : LocalDate {
    val kalender = Calendar.getInstance()
    kalender.time = this.toDate()
    val sisteDagIMåned = kalender.getActualMaximum(Calendar.DAY_OF_MONTH);
    return this.withDayOfMonth(sisteDagIMåned)
}

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)

private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
