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
    triggerTid: LocalDateTime = LocalDateTime.now(),
    forsinkelse: Duration = Duration.ofSeconds(0),
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

