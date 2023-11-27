package no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.neste
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TidspunktClosedRangeTest {
    val a = YearMonth.of(2020, 1).tilTidspunkt()
    val b = a.neste()
    val c = b.neste()
    val d = c.neste()
    val e = d.neste()
    val f = e.neste()

    val tomListe = emptyList<Tidspunkt<Måned>>()

    @Test
    fun `A til A`() {
        val tidsrom = a..a
        assertEquals(listOf(a), tidsrom.toList())
    }

    @Test
    fun `A til B`() {
        val tidsrom = a..b
        assertEquals(listOf(a, b), tidsrom.toList())
    }

    @Test
    fun `A til C`() {
        val tidsrom = a..c
        assertEquals(listOf(a, b, c), tidsrom.toList())
    }

    @Test
    fun `B til A`() {
        val tidsrom = b..a
        assertEquals(tomListe, tidsrom.toList())
    }

    @Test
    fun `←A til A`() {
        val tidspunkter = (a.somUendeligLengeSiden()..a).toList()
        assertEquals(listOf(a), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
    }

    @Test
    fun `←A til B`() {
        val tidspunkter = (a.somUendeligLengeSiden()..b).toList()
        assertEquals(listOf(a, b), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erEndelig())
    }

    @Test
    fun `←A til ←A`() {
        val tidspunkter = (a.somUendeligLengeSiden()..a.somUendeligLengeSiden()).toList()
        assertEquals(listOf(a), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
    }

    @Test
    fun `←A til ←C`() {
        val tidspunkter = (a.somUendeligLengeSiden()..c.somUendeligLengeSiden()).toList()
        assertEquals(listOf(a, b, c), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
        assertTrue(tidspunkter[1].erEndelig())
        assertTrue(tidspunkter[2].erEndelig())
    }

    @Test
    fun `←B til A`() {
        val tidspunkter = (b.somUendeligLengeSiden()..a).toList()
        assertEquals(listOf(a), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
    }

    @Test
    fun `←B til ←A`() {
        val tidspunkter = (b.somUendeligLengeSiden()..a.somUendeligLengeSiden()).toList()
        assertEquals(listOf(a), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeSiden())
    }

    @Test
    fun `A til A→`() {
        val tidspunkter = (a..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(a), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `A→ til A→`() {
        val tidspunkter = (a.somUendeligLengeTil()..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(a), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `A→ til C→`() {
        val tidspunkter = (a.somUendeligLengeTil()..c.somUendeligLengeTil()).toList()
        assertEquals(listOf(a, b, c), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erEndelig())
        assertTrue(tidspunkter[1].erEndelig())
        assertTrue(tidspunkter[2].erUendeligLengeTil())
    }

    @Test
    fun `B til A→`() {
        val tidspunkter = (b..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(b), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `B→ til A→`() {
        val tidspunkter = (b.somUendeligLengeTil()..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(b), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter[0].erUendeligLengeTil())
    }

    @Test
    fun `←A til A→`() {
        val tidspunkter = (a.somUendeligLengeSiden()..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(a, b), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←A til B→`() {
        val tidspunkter = (a.somUendeligLengeSiden()..b.somUendeligLengeTil()).toList()
        assertEquals(listOf(a, b), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←A til C→`() {
        val tidspunkter = (a.somUendeligLengeSiden()..c.somUendeligLengeTil()).toList()
        assertEquals(listOf(a, b, c), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←B til A→`() {
        val tidspunkter = (b.somUendeligLengeSiden()..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(a, b), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `←E til A→`() {
        val tidspunkter = (e.somUendeligLengeSiden()..a.somUendeligLengeTil()).toList()
        assertEquals(listOf(a, b, c, d, e), tidspunkter.map { it.somEndelig() })
        assertTrue(tidspunkter.first().erUendeligLengeSiden())
        assertTrue(tidspunkter.last().erUendeligLengeTil())
    }

    @Test
    fun `A→ til ←A`() {
        val tidsrom = a.somUendeligLengeTil()..a.somUendeligLengeSiden()
        assertEquals(listOf(a), tidsrom.toList())
    }

    @Test
    fun `A→ til ←B`() {
        val tidsrom = a.somUendeligLengeTil()..b.somUendeligLengeSiden()
        assertEquals(listOf(a, b), tidsrom.toList())
    }

    @Test
    fun `A→ til ←E`() {
        val tidsrom = a.somUendeligLengeTil()..e.somUendeligLengeSiden()
        assertEquals(listOf(a, b, c, d, e), tidsrom.toList())
    }

    @Test
    fun `B→ til ←A`() {
        val tidsrom = b.somUendeligLengeTil()..a.somUendeligLengeSiden()
        assertEquals(tomListe, tidsrom.toList())
    }

    @Test
    fun testTidsromMedMåneder() {
        val fom = MånedTidspunkt.uendeligLengeSiden(YearMonth.of(2020, 1))
        val tom = MånedTidspunkt.uendeligLengeTil(YearMonth.of(2020, 10))
        val tidsrom = fom..tom

        assertEquals(10, tidsrom.count())
        assertEquals(fom, tidsrom.first())
        assertEquals(tom, tidsrom.last())
    }

    @Test
    fun testTidsromMedDager() {
        val fom = DagTidspunkt.uendeligLengeSiden(LocalDate.of(2020, 1, 1))
        val tom = DagTidspunkt.uendeligLengeTil(LocalDate.of(2020, 10, 31))
        val tidsrom = fom..tom

        assertEquals(305, tidsrom.count())
        assertEquals(fom, tidsrom.first())
        assertEquals(tom, tidsrom.last())
    }
}
