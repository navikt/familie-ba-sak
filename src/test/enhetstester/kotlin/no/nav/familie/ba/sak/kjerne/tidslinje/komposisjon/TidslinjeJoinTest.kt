package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilStringTidslinje
import no.nav.familie.tidslinje.utvidelser.join
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TidslinjeJoinTest {
    private val tidslinje1 = mapOf("key" to "abc".tilCharTidslinje(jan(2000)))
    private val tidslinje2 = mapOf("key" to "fgh".tilCharTidslinje(feb(2000)))

    @Test
    fun join() {
        val faktiskePerioder =
            tidslinje1
                .join(tidslinje2) { c1, c2 ->
                    when {
                        c1 == null && c2 == null -> null
                        c1 == null -> c2.toString()
                        c2 == null -> c1.toString()
                        else -> c1.toString() + c2.toString()
                    }
                }["key"]!!

        val forventedePerioder = listOf("a", "bf", "cg", "h").tilStringTidslinje(jan(2000))

        assertThat(faktiskePerioder).isEqualTo(forventedePerioder)
    }

    @Test
    fun joinIkkeNull() {
        val faktiskePerioder =
            tidslinje1
                .joinIkkeNull(tidslinje2) { c1, c2 ->
                    c1.toString() + c2.toString()
                }["key"]!!
                .tilPerioder()

        assertThat(faktiskePerioder).hasSize(4)

        assertThat(faktiskePerioder[0].verdi).isNull()
        assertThat(faktiskePerioder[0].fom).isEqualTo(1.jan(2000))
        assertThat(faktiskePerioder[0].tom).isEqualTo(31.jan(2000))

        assertThat(faktiskePerioder[1].verdi).isEqualTo("bf")
        assertThat(faktiskePerioder[1].fom).isEqualTo(1.feb(2000))
        assertThat(faktiskePerioder[1].tom).isEqualTo(29.feb(2000))

        assertThat(faktiskePerioder[2].verdi).isEqualTo("cg")
        assertThat(faktiskePerioder[2].fom).isEqualTo(1.mar(2000))
        assertThat(faktiskePerioder[2].tom).isEqualTo(31.mar(2000))

        assertThat(faktiskePerioder[3].verdi).isNull()
        assertThat(faktiskePerioder[3].fom).isEqualTo(1.apr(2000))
        assertThat(faktiskePerioder[3].tom).isEqualTo(30.apr(2000))
    }
}
