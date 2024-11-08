package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.common.ClockProvider
import java.time.Clock

class TestClockProvider(
    private val clock: Clock,
) : ClockProvider {
    override fun get(): Clock = clock
}
