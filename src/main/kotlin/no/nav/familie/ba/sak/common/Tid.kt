package no.nav.familie.ba.sak.common

import java.time.LocalDate
import java.time.Month

object Tid {
    var TIDENES_BEGYNNELSE: LocalDate? = null
    var TIDENES_ENDE: LocalDate? = null

    init {
        TIDENES_BEGYNNELSE = LocalDate.of(-4712, Month.JANUARY, 1)
        TIDENES_ENDE = LocalDate.of(9999, Month.DECEMBER, 31)
    }
}