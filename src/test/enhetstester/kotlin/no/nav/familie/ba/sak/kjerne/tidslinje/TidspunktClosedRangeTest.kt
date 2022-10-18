package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.ogSenere
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.ogTidligere
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TidspunktClosedRangeTest {
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
    fun testTidsromMedUendeligFremtidFraEtTidspunkt() {
        val fom = YearMonth.of(2020, 1).tilTidspunkt()
        val tidsrom = fom.ogSenere()

        Assertions.assertEquals(1, tidsrom.count())
        Assertions.assertEquals(fom.somUendeligLengeTil(), tidsrom.first())
        Assertions.assertEquals(fom.somUendeligLengeTil(), tidsrom.last())
    }

    @Test
    fun testTidsromMedUendeligFortidFraEtTidspunkt() {
        val tom = YearMonth.of(2020, 1).tilTidspunkt()
        val tidsrom = tom.ogTidligere()

        Assertions.assertEquals(1, tidsrom.count())
        Assertions.assertEquals(tom.somUendeligLengeSiden(), tidsrom.first())
        Assertions.assertEquals(tom.somUendeligLengeSiden(), tidsrom.last())
    }
}
