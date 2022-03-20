package no.nav.familie.ba.sak.kjerne.tidslinje

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TidsromTest {
    @Test
    fun testTidsromMedMåneder() {
        val fom = Tidspunkt.uendeligLengeSiden(YearMonth.of(2020, 1))
        val tom = Tidspunkt.uendeligLengeTil(YearMonth.of(2020, 10))
        val tidsrom = fom..tom

        Assertions.assertEquals(10, tidsrom.count())
        Assertions.assertEquals(fom, tidsrom.first())
        Assertions.assertEquals(tom, tidsrom.last())
    }

    @Test
    fun testTidsromMedDager() {
        val fom = Tidspunkt.uendeligLengeSiden(LocalDate.of(2020, 1, 1))
        val tom = Tidspunkt.uendeligLengeTil(LocalDate.of(2020, 10, 31))
        val tidsrom = fom..tom

        Assertions.assertEquals(305, tidsrom.count())
        Assertions.assertEquals(fom, tidsrom.first())
        Assertions.assertEquals(tom, tidsrom.last())
    }

    @Test
    fun testTidsromFomErMånedOgTomErDag() {
        val fom = Tidspunkt.uendeligLengeSiden(YearMonth.of(2020, 1))
        val tom = Tidspunkt.uendeligLengeTil(LocalDate.of(2020, 10, 31))
        val tidsrom = fom..tom

        Assertions.assertEquals(305, tidsrom.count())
        Assertions.assertEquals(Tidspunkt.uendeligLengeSiden(LocalDate.of(2020, 1, 1)), tidsrom.first())
        Assertions.assertEquals(tom, tidsrom.last())
    }

    @Test
    fun testTidsromFomErDagOgTomErMåned() {
        val fom = Tidspunkt.uendeligLengeSiden(LocalDate.of(2020, 1, 1))
        val tom = Tidspunkt.uendeligLengeTil(YearMonth.of(2020, 10))
        val tidsrom = fom..tom

        Assertions.assertEquals(305, tidsrom.count())
        Assertions.assertEquals(fom, tidsrom.first())
        Assertions.assertEquals(Tidspunkt.uendeligLengeTil(LocalDate.of(2020, 10, 31)), tidsrom.last())
    }
}
