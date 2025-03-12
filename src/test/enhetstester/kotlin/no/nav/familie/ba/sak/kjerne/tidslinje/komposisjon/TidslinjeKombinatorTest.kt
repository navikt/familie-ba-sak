package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilStringTidslinje
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TidslinjeKombinatorTest {
    /*
    Dager:       | 1. januar | 2. januar | 3. januar | 4. januar | 5. januar |
    Tidslinje 1: |     a     |     a     |     a     |     a     |
    Tidslinje 2:                         |     b     |     b     |     b     |
    Tidslinje 3: |     c     |    null   |    null   |     c     |
     */
    private val tidslinje1 = "aaaa".tilCharTidslinje(1.jan(2000))
    private val tidslinje2 = "bbb".tilCharTidslinje(3.jan(2000))
    private val tidslinje3 = "c  c".tilCharTidslinje(1.jan(2000))

    @Test
    fun kombinerUtenNull() {
        val faktisk = listOf(tidslinje1, tidslinje2, tidslinje3).kombinerUtenNull { it.joinToString("") }
        val forventet = listOf("ac", "a", "ab", "abc", "b").tilStringTidslinje(1.jan(2000))

        assertThat(faktisk).isEqualTo(forventet)
    }

    @Test
    fun kombinerUtenNullOgIkkeTom() {
        val faktisk = listOf(tidslinje1, tidslinje2, tidslinje3).kombinerUtenNullOgIkkeTom { it.joinToString("") }
        val forventet = listOf("ac", "a", "ab", "abc", "b").tilStringTidslinje(1.jan(2000))

        assertThat(faktisk).isEqualTo(forventet)
    }

    @Test
    fun kombiner() {
        val faktisk = listOf(tidslinje1, tidslinje2, tidslinje3).kombiner().tilPerioder()

        assertThat(faktisk.size).isEqualTo(5)
        assertThat(faktisk[0].verdi).containsExactly('a', 'c')
        assertThat(faktisk[1].verdi).containsExactly('a')
        assertThat(faktisk[2].verdi).containsExactly('a', 'b')
        assertThat(faktisk[3].verdi).containsExactly('a', 'b', 'c')
        assertThat(faktisk[4].verdi).containsExactly('b')
    }

    @Test
    fun kombinerKunVerdiMedMap() {
        val tidslinjeMap = mapOf("key1" to tidslinje1, "key2" to tidslinje2)

        val faktisk =
            tidslinjeMap.kombinerKunVerdiMed(tidslinje3) { c1, c2 ->
                c1.toString() + c2.toString()
            }

        val forventet1 = listOf("ac", null, null, "ac").tilStringTidslinje(1.jan(2000))
        val forventet2 = listOf(null, null, null, "bc", null).tilStringTidslinje(1.jan(2000))

        assertThat(faktisk["key1"]).isEqualTo(forventet1)
        assertThat(faktisk["key2"]).isEqualTo(forventet2)
    }

    @Test
    fun kombinerKunVerdiMed() {
        val faktisk =
            tidslinje1.kombinerKunVerdiMed(tidslinje2, tidslinje3) { c1, c2, c3 ->
                c1.toString() + c2.toString() + c3.toString()
            }

        val forventet = listOf(null, null, null, "abc", null).tilStringTidslinje(1.jan(2000))

        assertThat(faktisk).isEqualTo(forventet)
    }

    @Test
    fun erIkkeTom() {
        assertThat(tomTidslinje<Char>().erIkkeTom()).isFalse()
        assertThat("a".tilCharTidslinje(1.jan(2000)).erIkkeTom()).isTrue()
    }

    @Test
    fun harOverlappMed() {
        assertThat(tidslinje1.harOverlappMed(tidslinje2)).isTrue()

        val tidslinjeUtenOverlappMedTidslinje1 = "a".tilCharTidslinje(1.feb(2000))
        assertThat(tidslinje1.harOverlappMed(tidslinjeUtenOverlappMedTidslinje1)).isFalse()
    }

    @Test
    fun harIkkeOverlappMed() {
        assertThat(tidslinje1.harIkkeOverlappMed(tidslinje2)).isFalse()

        val tidslinjeUtenOverlappMedTidslinje1 = "a".tilCharTidslinje(1.feb(2000))
        assertThat(tidslinje1.harIkkeOverlappMed(tidslinjeUtenOverlappMedTidslinje1)).isTrue()
    }

    @Test
    fun `kombinerMedNullable skal returnere this hvis den andre tidslinjen er null`() {
        val nullTidslinje: Tidslinje<Char>? = null
        val faktisk = tidslinje1.kombinerMedNullable(nullTidslinje) { c1, c2 -> c1 }
        assertThat(faktisk).isEqualTo(tidslinje1)
    }

    @Test
    fun kombinerMedNullable() {
        val faktisk =
            tidslinje1.kombinerMedNullable(tidslinje2) { c1, c2 ->
                if (c1 == null || c2 == null) null else c1
            }

        val forventet = "  aa ".tilCharTidslinje(1.jan(2000))

        assertThat(faktisk).isEqualTo(forventet)
    }
}
