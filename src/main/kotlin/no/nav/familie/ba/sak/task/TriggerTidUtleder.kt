package no.nav.familie.ba.sak.task

import no.nav.familie.util.VirkedagerProvider
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

private val tidligsteKjøretidIHverdag = LocalTime.of(6, 0)
private val senesteKjøretidIHverdag = LocalTime.of(21, 0)

/**
 * Finner neste trigger tid for tasker som kun skal kjøre på "dagtid" i hverdager. Dagtid er
 * definert som hverdager mellom kl. 06-21. Faste og flytende helligdager er tatt høyde for
 * og vil ikke bli valgt.
 */
fun utledNesteTriggerTidIHverdagerForTask(
    triggerTid: LocalDateTime = LocalDateTime.now(),
    minimumForsinkelse: Duration = Duration.ofSeconds(0),
): LocalDateTime {
    val nyTriggerTid = triggerTid.plus(minimumForsinkelse)
    val nyTid = nyTriggerTid.toLocalTime()
    val nyDato = nyTriggerTid.toLocalDate()

    val nesteVirkedag = VirkedagerProvider.nesteVirkedag(nyDato)

    return when {
        VirkedagerProvider.erHelgEllerHelligdag(nyDato) -> LocalDateTime.of(nesteVirkedag, tidligsteKjøretidIHverdag)
        !nyTid.isAfter(tidligsteKjøretidIHverdag) -> LocalDateTime.of(nyDato, tidligsteKjøretidIHverdag)
        !nyTid.isAfter(senesteKjøretidIHverdag) -> nyTriggerTid
        else -> LocalDateTime.of(nesteVirkedag, tidligsteKjøretidIHverdag)
    }
}