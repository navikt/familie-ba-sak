package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.toLocalDate
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class TestClockProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ClockProvider {
    override fun get(): Clock = clock

    companion object {
        fun lagClockProviderMedFastTidspunkt(localDateTime: LocalDateTime): TestClockProvider =
            TestClockProvider(Clock.fixed(localDateTime.toInstant(ZoneOffset.UTC), ZoneId.systemDefault()))

        fun lagClockProviderMedFastTidspunkt(localDate: LocalDate): TestClockProvider =
            lagClockProviderMedFastTidspunkt(localDate.atStartOfDay())

        fun lagClockProviderMedFastTidspunkt(yearMonth: YearMonth): TestClockProvider =
            lagClockProviderMedFastTidspunkt(yearMonth.toLocalDate())
    }
}
