package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ZipTidslinjeTest {

    val kombinator = { venstre: Char?, høyre: Char? ->
        (venstre?.toString() ?: "").trim() + (høyre?.toString() ?: "").trim()
    }

    @Test
    fun testZipMedNesteTidslinje() {
        val aTilF = ('a'..'f').toList().joinToString("")
        val bokstavTidslinje = aTilF.tilCharTidslinje(YearMonth.now())
        val bokstavParTidslinje = bokstavTidslinje.zipMedNeste()

        assertThat(aTilF).isEqualTo("abcdef")

        assertThat(bokstavParTidslinje.perioder().map { it.innhold }).isEqualTo(
            listOf(Pair(null, 'a'), Pair('a', 'b'), Pair('b', 'c'), Pair('c', 'd'), Pair('d', 'e'), Pair('e', 'f')),
        )

        println(listOf(Pair(null, 'a'), Pair('a', 'b'), Pair('b', 'c'), Pair('c', 'd'), Pair('d', 'e'), Pair('e', 'f')))
    }
}
