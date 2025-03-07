package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ZipTidslinjeTest {
    @Test
    fun testZipMedNesteTidslinje() {
        val aTilF = ('a'..'f').toList().joinToString("")
        val bokstavTidslinje = aTilF.tilCharTidslinje(YearMonth.now())
        val bokstavParTidslinje = bokstavTidslinje.zipMedNeste(ZipPadding.FØR)

        assertThat(aTilF).isEqualTo("abcdef")

        assertThat(bokstavParTidslinje.tilPerioder().map { it.verdi }).isEqualTo(
            listOf(Pair(null, 'a'), Pair('a', 'b'), Pair('b', 'c'), Pair('c', 'd'), Pair('d', 'e'), Pair('e', 'f')),
        )
    }

    @Test
    fun testZipMedNesteTidslinjePaddingEtter() {
        val aTilF = ('a'..'f').toList().joinToString("")
        val bokstavTidslinje = aTilF.tilCharTidslinje(YearMonth.now())
        val bokstavParTidslinje = bokstavTidslinje.zipMedNeste(ZipPadding.ETTER)

        assertThat(aTilF).isEqualTo("abcdef")

        assertThat(bokstavParTidslinje.tilPerioder().map { it.verdi }).isEqualTo(
            listOf(Pair('a', 'b'), Pair('b', 'c'), Pair('c', 'd'), Pair('d', 'e'), Pair('e', 'f'), Pair('f', null)),
        )
    }

    @Test
    fun testZipMedNesteTidslinjeIngenPadding() {
        val aTilF = ('a'..'f').toList().joinToString("")
        val bokstavTidslinje = aTilF.tilCharTidslinje(YearMonth.now())
        val bokstavParTidslinje = bokstavTidslinje.zipMedNeste(ZipPadding.INGEN_PADDING)

        assertThat(aTilF).isEqualTo("abcdef")

        assertThat(bokstavParTidslinje.tilPerioder().map { it.verdi }).isEqualTo(
            listOf(Pair('a', 'b'), Pair('b', 'c'), Pair('c', 'd'), Pair('d', 'e'), Pair('e', 'f')),
        )
    }
}
