package no.nav.familie.ba.sak.common

import org.springframework.stereotype.Service
import java.time.LocalDate

interface LocalDateProvider {
    fun now(): LocalDate
}

@Service
class RealDateProvider : LocalDateProvider {
    override fun now() = LocalDate.now()
}
