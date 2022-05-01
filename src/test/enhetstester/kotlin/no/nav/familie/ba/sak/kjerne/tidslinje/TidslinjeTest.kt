package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje.Companion.TidslinjeFeilException
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class TidslinjeTest {

    @Test
    fun `skal validere at perioder ikke kan ha fra-og-med etter til-og-med`() {

        assertThrows<TidslinjeFeilException> {
            TestTidslinje(
                Periode(15.jan(2020), 14.jan(2020), 'A')
            ).perioder()
        }
    }

    @Test
    fun `skal validere at perioder som overlapper med kun én dag ikke er lov`() {
        assertThrows<TidslinjeFeilException> {
            TestTidslinje(
                Periode(1.jan(2020), 31.mar(2020), 'A'),
                Periode(31.mar(2020), 31.mai(2020), 'B')
            ).perioder()
        }
    }

    @Test
    fun `skal validere at periode som ligger inni en annen ikke er lov`() {

        assertThrows<TidslinjeFeilException> {
            TestTidslinje(
                Periode(1.jan(2020), 31.mai(2020), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B')
            ).perioder()
        }
    }

    @Test
    fun `skal validere at uendelig i begge ender av tidslinjen er lov`() {
        assertDoesNotThrow {
            TestTidslinje(
                Periode(1.jan(2020).somUendeligLengeSiden(), 1.jan(2020).somUendeligLengeTil(), 'A'),
            ).perioder()
        }

        assertDoesNotThrow {
            TestTidslinje(
                Periode(1.jan(2020).somUendeligLengeSiden(), 29.feb(2020), 'A'),
                Periode(1.mar(2020), 30.apr(2020).somUendeligLengeTil(), 'B')
            ).perioder()
        }
    }

    @Test
    fun `skal validere at uendelige perioder inni en tidslinje ikke er lov`() {

        assertThrows<TidslinjeFeilException> {
            TestTidslinje(
                Periode(1.jan(2020), 31.jan(2020), 'A'),
                Periode(1.feb(2020).somUendeligLengeSiden(), 29.feb(2020), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B')
            ).perioder()
        }

        assertThrows<TidslinjeFeilException> {
            TestTidslinje(
                Periode(1.jan(2020), 31.jan(2020), 'A'),
                Periode(1.feb(2020), 29.feb(2020).somUendeligLengeTil(), 'A'),
                Periode(1.mar(2020), 30.apr(2020), 'B')
            ).perioder()
        }
    }
}

internal class TestTidslinje(vararg val perioder: Periode<Char, Dag>) : Tidslinje<Char, Dag>() {
    override fun lagPerioder() = perioder.toList()
}
