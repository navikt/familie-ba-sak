package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.toLocalDate
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class TestClockProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ClockProvider {
    override fun get(): Clock = clock

    companion object {
        private val zoneId = ZoneId.of("Europe/Oslo")

        fun lagClockProviderMedFastTidspunkt(localDateTime: ZonedDateTime): TestClockProvider = TestClockProvider(Clock.fixed(localDateTime.toInstant(), zoneId))

        fun lagClockProviderMedFastTidspunkt(localDate: LocalDate): TestClockProvider = lagClockProviderMedFastTidspunkt(localDate.atStartOfDay(zoneId))

        fun lagClockProviderMedFastTidspunkt(yearMonth: YearMonth): TestClockProvider = lagClockProviderMedFastTidspunkt(yearMonth.toLocalDate())
    }
}
