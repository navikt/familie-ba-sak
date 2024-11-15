package no.nav.familie.ba.sak.task

import no.nav.familie.util.VirkedagerProvider
import java.time.Duration
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
fun finnNesteTriggerTidIHverdagerForTask(
    forsinkelse: Duration = Duration.ofSeconds(0),
    triggerTid: LocalDateTime = LocalDateTime.now(),
): LocalDateTime {
    val nyTriggerTid = triggerTid.plus(forsinkelse)
    val nyTid = nyTriggerTid.toLocalTime()
    val nyDato = nyTriggerTid.toLocalDate()

    val nesteVirkedag = VirkedagerProvider.nesteVirkedag(nyDato)

    val erHelgEllerHelligdag = VirkedagerProvider.erHelgEllerHelligdag(nyDato)
    if (erHelgEllerHelligdag) {
        return LocalDateTime.of(nesteVirkedag, tidligsteKjøretid)
    }

    val erFørTidligsteKjøretid = nyTid.isBefore(tidligsteKjøretid)
    if (erFørTidligsteKjøretid) {
        return LocalDateTime.of(nyDato, tidligsteKjøretid)
    }

    val erFørSenesteKjøretid = nyTid.isBefore(senesteKjøretid)
    if (erFørSenesteKjøretid) {
        return nyTriggerTid
    }

    return LocalDateTime.of(nesteVirkedag, tidligsteKjøretid)
}

fun erKlokkenMellom21Og06(localTime: LocalTime = LocalTime.now()): Boolean = localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))

fun kl06IdagEllerNesteDag(date: LocalDateTime = LocalDateTime.now()): LocalDateTime =
    if (date.toLocalTime().isBefore(LocalTime.of(6, 0))) {
        date.withHour(6)
    } else {
        date.plusDays(1).withHour(6)
    }
