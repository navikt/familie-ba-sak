package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.tidslinje.TidsEnhet.MÅNED
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

internal class BeskjæreTidslinjeTest {
    @Test
    fun `skal beskjære endelig tidslinje på begge sider`() {
        val hovedlinje = "aaaaaa".tilCharTidslinje(des(2001))
        val beskjæring = "bbb".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaa".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beholde tidslinje som allerede er innenfor beskjæring`() {
        val hovedlinje = "aaa".tilCharTidslinje(feb(2002))
        val beskjæring = "bbbbbbbbb".tilCharTidslinje(des(2001))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaa".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beholde tidslinje som er innenfor en uendelig beskjæring`() {
        val hovedlinje = "aaa".tilCharTidslinje(feb(2002))
        val beskjæring = "<b>".tilCharTidslinje(mar(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaa".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `beskjæring utenfor tidslinjen skal gi tom tidslinje`() {
        val hovedlinje = "aaaaaa".tilCharTidslinje(des(2001))
        val beskjæring = "bbb".tilCharTidslinje(feb(2009))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = tomTidslinje<Char>(beskjæring.startsTidspunkt, MÅNED).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beskjære uendelig tidslinje begge veier mot endelig tidsline`() {
        val hovedlinje = "<aaaaaa>".tilCharTidslinje(des(2002))
        val beskjæring = "bbb".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaa".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beskjære tidslinje som går fra uendelig lenge siden til et endelig tidspunkt i fremtiden`() {
        val hovedlinje = "<aaaaaa".tilCharTidslinje(des(2038))
        val beskjæring = "bbbbb".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaaaa".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beskjære tidslinje som går fra et endelig tidspunkt i fortiden til uendelig lenge til`() {
        val hovedlinje = "aaaaaa>".tilCharTidslinje(des(1993))
        val beskjæring = "bbbbb".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaaaa".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beskjære uendelig fremtid slik at den blir kortest mulig`() {
        val hovedlinje = "aaaaaa>".tilCharTidslinje(des(1993))
        val beskjæring = "bbb>".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "a>".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beskjære uendelig fortid slik at den inneholder tidligste fra-og-med, beskjæring er tidligst`() {
        val hovedlinje = "<a".tilCharTidslinje(des(2038))
        val beskjæring = "<bbb".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring)
        val forventedePerioder = "<a".tilCharTidslinje(apr(2002))

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `skal beskjære uendelig fortid slik at den inneholder tidligste fra-og-med, beskjæring er senest`() {
        val hovedlinje = "<bbb".tilCharTidslinje(feb(2002))
        val beskjæring = "<a".tilCharTidslinje(des(2038))

        val faktiskePerioder = hovedlinje.beskjærEtter(beskjæring).tilPerioder()
        val forventedePerioder = "<bbb".tilCharTidslinje(feb(2002)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun `beskjære mot tom tidslinje skal gi tom tidslinje`() {
        val hovedlinje = "bbb".tilCharTidslinje(feb(2002))

        val faktiskePerioder = hovedlinje.beskjærEtter(tomTidslinje<Char>()).tilPerioder()
        val forventedePerioder = tomTidslinje<Char>(tidsEnhet = MÅNED).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun beskjærTilOgMedEtterTomTidslinje() {
        val hovedlinje = "aaaaa".tilCharTidslinje(jan(2000))
        val beskjæring = tomTidslinje<Char>()

        val faktiskePerioder = hovedlinje.beskjærTilOgMedEtter(beskjæring).tilPerioder()
        val forventedePerioder = tomTidslinje<Char>().tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun beskjærTilOgMedEtter() {
        val hovedlinje = "aaaaa".tilCharTidslinje(jan(2000))
        val beskjæring = "bbb".tilCharTidslinje(feb(2000))

        val faktiskePerioder = hovedlinje.beskjærTilOgMedEtter(beskjæring).tilPerioder()
        val forventedePerioder = "aaaa".tilCharTidslinje(jan(2000)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun beskjær() {
        val hovedlinje = "aaaaa".tilCharTidslinje(jan(2000))
        val fom = 1.feb(2000)
        val tom = 30.apr(2000)

        val faktiskePerioder = hovedlinje.beskjær(fom, tom).tilPerioder()
        val forventedePerioder = "aaa".tilCharTidslinje(feb(2000)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun beskjærFom() {
        val hovedlinje = "aaaaa".tilCharTidslinje(jan(2000))
        val fom = 1.feb(2000)

        val faktiskePerioder = hovedlinje.beskjærFraOgMed(fom).tilPerioder()
        val forventedePerioder = "aaaa".tilCharTidslinje(feb(2000)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun beskjærTom() {
        val hovedlinje = "aaaaa".tilCharTidslinje(jan(2000))
        val tom = 30.apr(2000)

        val faktiskePerioder = hovedlinje.beskjærTilOgMed(tom).tilPerioder()
        val forventedePerioder = "aaaa".tilCharTidslinje(jan(2000)).tilPerioder()

        assertEquals(forventedePerioder, faktiskePerioder)
    }

    @Test
    fun beskjærTomMap() {
        val hovedlinje1 = 1 to "aaaaa".tilCharTidslinje(jan(2000))
        val hovedlinje2 = 2 to "aaaaa".tilCharTidslinje(feb(2000))
        val tidslinjeMap = mapOf(hovedlinje1, hovedlinje2)
        val tom = 30.apr(2000)

        val faktiskePerioder = tidslinjeMap.beskjærTilOgMed(tom)
        val forventedePerioder1 = "aaaa".tilCharTidslinje(jan(2000)).tilPerioder()
        val forventedePerioder2 = "aaa".tilCharTidslinje(feb(2000)).tilPerioder()

        assertEquals(forventedePerioder1, faktiskePerioder[1]!!.tilPerioder())
        assertEquals(forventedePerioder2, faktiskePerioder[2]!!.tilPerioder())
    }

    @Test
    fun `beskjæring av tom tidslinje skal gi tom tidslinje`() {
        val hovedlinje = tomTidslinje<Char>()
        val beskjæring = "aaa".tilCharTidslinje(jan(2000))
        val forventedePerioder = tomTidslinje<Char>().tilPerioder()

        val faktiskePerioder =
            listOf(
                hovedlinje.beskjærEtter(beskjæring).tilPerioder(),
                hovedlinje.beskjærTilOgMedEtter(beskjæring).tilPerioder(),
                hovedlinje.beskjærTilOgMed(beskjæring.startsTidspunkt).tilPerioder(),
                hovedlinje.beskjærFraOgMed(beskjæring.kalkulerSluttTidspunkt()).tilPerioder(),
                hovedlinje.beskjær(beskjæring.startsTidspunkt, beskjæring.kalkulerSluttTidspunkt()).tilPerioder(),
            )

        faktiskePerioder.forEach { assertEquals(forventedePerioder, it) }
    }
}
