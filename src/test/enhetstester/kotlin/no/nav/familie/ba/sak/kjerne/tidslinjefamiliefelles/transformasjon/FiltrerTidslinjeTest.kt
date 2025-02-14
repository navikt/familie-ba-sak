package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.somBoolskTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.tilCharTidslinje
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FiltrerTidslinjeTest {
    @Test
    fun filtrerIkkeNull() {
        val tidslinje = "aa  cc".tilCharTidslinje(feb(2000))
        val opprinneligePerioder = tidslinje.tilPerioder()

        assertThat(opprinneligePerioder[0]).isEqualTo(Periode('a', 1.feb(2000), 31.mar(2000)))
        assertThat(opprinneligePerioder[1]).isEqualTo(Periode(null, 1.apr(2000), 31.mai(2000)))
        assertThat(opprinneligePerioder[2]).isEqualTo(Periode('c', 1.jun(2000), 31.jul(2000)))

        val filtrertePerioder = tidslinje.filtrerIkkeNull { it == 'a' }.tilPerioder()

        assertThat(filtrertePerioder[0]).isEqualTo(Periode('a', 1.feb(2000), 31.mar(2000)))
        assertThat(filtrertePerioder[1]).isEqualTo(Periode(null, 1.apr(2000), 31.jul(2000)))
    }

    @Test
    fun filtrerMed() {
        val tidslinje = "aaaaa".tilCharTidslinje(feb(2000))
        val boolskTidslinje = "ftftftf".somBoolskTidslinje(jan(2000))

        val filtrertePerioder = tidslinje.filtrerMed(boolskTidslinje).tilPerioder()

        assertThat(filtrertePerioder.map { it.verdi }).containsExactly('a', null, 'a', null, 'a')
    }

    @Test
    fun filtrerHverKunVerdi() {
        val tidslinje1 = "ababa".tilCharTidslinje(jan(2000))
        val tidslinje2 = "babab".tilCharTidslinje(jan(2000))

        val filtrerteTidslinjer =
            mapOf(
                "1" to tidslinje1,
                "2" to tidslinje2,
            ).filtrerHverKunVerdi { it == 'a' }

        assertThat(filtrerteTidslinjer["1"]?.tilPerioder()?.map { it.verdi }).containsExactly('a', null, 'a', null, 'a')
        assertThat(filtrerteTidslinjer["2"]?.tilPerioder()?.map { it.verdi }).containsExactly(null, 'a', null, 'a', null)
    }
}
