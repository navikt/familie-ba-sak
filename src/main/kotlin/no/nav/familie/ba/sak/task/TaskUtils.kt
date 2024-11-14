package no.nav.familie.ba.sak.task

import no.nav.familie.util.VirkedagerProvider
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

val senesteKjøretid = LocalTime.of(21, 0)
val tidligsteKjøretid = LocalTime.of(6, 0)

/**
 * Finner neste gyldige kjøringstidspunkt for tasker som kun skal kjøre på "dagtid".
 *
 * Dagtid er nå definert som hverdager mellom 06-21. Faste helligdager er tatt høyde for, men flytende
 * er ikke kodet inn.
 */
fun nesteGyldigeTriggertidForBehandlingIHverdager(
    minutesToAdd: Long = 0,
    triggerTid: LocalDateTime = LocalDateTime.now(),
): LocalDateTime {
    var date = triggerTid.plusMinutes(minutesToAdd)

    val erFørSenesteKjøretid = date.toLocalTime().isBefore(senesteKjøretid)
    val erEtterTidligsteKjøretid = date.toLocalTime().isAfter(tidligsteKjøretid)

    if (date.erHverdag() && erFørSenesteKjøretid && !erEtterTidligsteKjøretid) {
        return LocalDateTime.of(date.toLocalDate(), LocalTime.of(6, 0))
    }
    if (date.erHverdag() && erFørSenesteKjøretid && erEtterTidligsteKjøretid) {
        return date
    } else {
        val nesteVirkedag = VirkedagerProvider.nesteVirkedag(date.toLocalDate())
        return LocalDateTime.of(nesteVirkedag, LocalTime.of(6, 0))
    }
}

private fun LocalDateTime.erHverdag(): Boolean =
    when (this.dayOfWeek) {
        DayOfWeek.MONDAY -> true
        DayOfWeek.TUESDAY -> true
        DayOfWeek.WEDNESDAY -> true
        DayOfWeek.THURSDAY -> true
        DayOfWeek.FRIDAY -> true
        DayOfWeek.SATURDAY -> false
        DayOfWeek.SUNDAY -> false
        else -> error("Not implemented")
    }

fun erKlokkenMellom21Og06(localTime: LocalTime = LocalTime.now()): Boolean = localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))

fun kl06IdagEllerNesteDag(date: LocalDateTime = LocalDateTime.now()): LocalDateTime =
    if (date.toLocalTime().isBefore(LocalTime.of(6, 0))) {
        date.withHour(6)
    } else {
        date.plusDays(1).withHour(6)
    }
