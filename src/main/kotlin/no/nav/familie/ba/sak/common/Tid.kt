package no.nav.familie.ba.sak.common

import java.time.LocalDate
import java.time.ZoneId
import java.util.*

fun LocalDate.sisteDagIForrigeMåned() : LocalDate {

    val kalender = Calendar.getInstance()
    val sammeDagForrigeMåned = this.minusMonths(1)
    kalender.time = sammeDagForrigeMåned.toDate()

    val sisteDagIForrigeMåned = kalender.getActualMaximum(Calendar.DAY_OF_MONTH);
    return sammeDagForrigeMåned.withDayOfMonth(sisteDagIForrigeMåned)
}

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)

private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
