package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class KompetanseUtilTest {

    val januar2020 = YearMonth.of(2020, 1)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testOppdatering() {
        val kompetanse = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("------", barn1, barn2, barn3)
            .byggKompetanser().first()

        val oppdatertKompetanse = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("  SS  ", barn1)
            .byggKompetanser().first()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("------", barn2, barn3)
            .medKompetanse("--  --", barn1)
            .byggKompetanser()

        val restKompetanser = KompetanseUtil.finnRestKompetanser(kompetanse, oppdatertKompetanse)

        assertEquals(3, restKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, restKompetanser)
    }

    @Test
    fun testMergePåfølgendePerioder() {
        val kompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("SSS", barn1)
            .medKompetanse("   SSS", barn1)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("SSSSSS", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = KompetanseUtil.mergeKompetanser(kompetanser)
        assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testMergeForPerioderMedMellomrom() {
        val kompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("SSS", barn1, barn2, barn3)
            .medKompetanse("    SSS", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("SSS SSS", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = KompetanseUtil.mergeKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testMergeForPerioderDerTidligstePeriodeHarÅpemTOM() {
        val kompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .medKompetanse("   -----", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = KompetanseUtil.mergeKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
        assertEquals(null, faktiskeKompetanser.first().tom)
    }

    @Test
    fun testMergeForPerioderMedOverlapp() {
        val kompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("-----", barn1, barn2, barn3)
            .medKompetanse("   -----", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("--------", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = KompetanseUtil.mergeKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testMergeForPerioderDerSenestePeriodeHarÅpemTOM() {
        val kompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("------", barn1, barn2, barn3)
            .medKompetanse("   ----->", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = KompetanseUtil.mergeKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
        assertEquals(januar2020, faktiskeKompetanser.first().fom)
        assertEquals(null, faktiskeKompetanser.first().tom)
    }

    @Test
    fun sammensattTest() {
        val kompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse("SSSSSSS", barn1)
            .medKompetanse("SSSPPSS", barn2)
            .medKompetanse("-SSSSSS", barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(behandlingId = 1, januar2020)
            .medKompetanse(" SS  SS", barn1, barn2, barn3)
            .medKompetanse("S      ", barn1, barn2)
            .medKompetanse("   SS  ", barn1, barn3)
            .medKompetanse("       ", barn2, barn3)
            .medKompetanse("       ", barn1)
            .medKompetanse("   PP  ", barn2)
            .medKompetanse("-      ", barn3)
            .byggKompetanser()

        val faktiskeKompetanser = KompetanseUtil.mergeKompetanser(kompetanser)
        assertEquals(6, faktiskeKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    private fun <T> assertEqualsUnordered(
        expected: Collection<T>,
        actual: Collection<T>
    ) {
        assertEquals(
            expected.size, actual.size,
            "Forskjellig antall. Forventet ${expected.size} men fikk ${actual.size}"
        )
        assertTrue(expected.containsAll(actual), "Forvantet liste inneholder ikke alle elementene fra faktisk liste")
        assertTrue(actual.containsAll(expected), "Faktisk liste inneholder ikke alle elementene fra forventet liste")
    }
}
