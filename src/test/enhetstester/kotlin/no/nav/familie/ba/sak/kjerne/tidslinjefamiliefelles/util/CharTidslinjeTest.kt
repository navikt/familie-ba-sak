package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util

import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.tilMåned
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CharTidslinjeTest {
    @Test
    fun testEnkelCharTidsline() {
        val tegn = "---------------"
        val charTidslinje = tegn.tilCharTidslinje(jan(2020)).tilMåned { it.single() }

        assertEquals(1.jan(2020), charTidslinje.startsTidspunkt)
        assertEquals(tegn.length, charTidslinje.innhold.sumOf { it.lengde })

        val periode = charTidslinje.tilPerioder().single()

        assertEquals(1.jan(2020), periode.fom)
        assertEquals(31.mar(2021), periode.tom)
        assertEquals('-', periode.verdi)
    }

    @Test
    fun testUendeligCharTidslinje() {
        val tegn = "<--->"
        val charTidslinje = tegn.tilCharTidslinje(jan(2020))

        assertEquals(PRAKTISK_TIDLIGSTE_DAG, charTidslinje.startsTidspunkt)

        val periode = charTidslinje.tilPerioder().single()

        assertNull(periode.fom)
        assertNull(periode.tom)
        assertEquals('-', periode.verdi)
    }

    @Test
    fun testSammensattTidsline() {
        val tegn = "aabbbbcdddddda"
        val charTidslinje = tegn.tilCharTidslinje(jan(2020)).tilMåned { it.single() }

        assertEquals(1.jan(2020), charTidslinje.startsTidspunkt)
        assertEquals(tegn.length, charTidslinje.innhold.sumOf { it.lengde })

        val perioder = charTidslinje.tilPerioder()
        assertEquals(5, perioder.size)
        assertEquals((jan(2020)..feb(2020)).med('a'), perioder[0])
        assertEquals((mar(2020)..jun(2020)).med('b'), perioder[1])
        assertEquals((jul(2020)..jul(2020)).med('c'), perioder[2])
        assertEquals((aug(2020)..jan(2021)).med('d'), perioder[3])
        assertEquals((feb(2021)..feb(2021)).med('a'), perioder[4])
    }
}
