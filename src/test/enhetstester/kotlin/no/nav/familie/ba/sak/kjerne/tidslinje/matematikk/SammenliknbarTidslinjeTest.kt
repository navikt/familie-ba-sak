package no.nav.familie.ba.sak.kjerne.tidslinje.matematikk

import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SammenliknbarTidslinjeTest {
    @Test
    fun minsteAvHver() {
        val tidslinje1 = mapOf(1 to "acegi".tilCharTidslinje(jan(2000)))
        val tidslinje2 = mapOf(1 to "bbddi".tilCharTidslinje(jan(2000)))

        val minsteAvHver = minsteAvHver(tidslinje1, tidslinje2)[1]!!
        val forventetTidslinje = "abddi".tilCharTidslinje(jan(2000))

        assertThat(minsteAvHver).isEqualTo(forventetTidslinje)
    }
}
