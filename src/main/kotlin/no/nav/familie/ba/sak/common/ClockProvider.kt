package no.nav.familie.ba.sak.common

import java.time.Clock

fun interface ClockProvider {
    fun get(): Clock
}
