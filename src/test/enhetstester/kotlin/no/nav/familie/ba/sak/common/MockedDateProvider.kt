package no.nav.familie.ba.sak.common

import java.time.LocalDate

class MockedDateProvider(val mockedDate: LocalDate) : LocalDateProvider {
    override fun now(): LocalDate = this.mockedDate
}
