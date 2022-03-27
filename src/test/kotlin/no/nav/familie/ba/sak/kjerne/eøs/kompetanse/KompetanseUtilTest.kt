package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.slåSammenKompetanser
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somRegelverk
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KompetanseUtilTest {

    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testOppdatering() {
        val kompetanse = KompetanseBuilder(jan2020)
            .medKompetanse("------", barn1, barn2, barn3)
            .byggKompetanser().first()

        val oppdatertKompetanse = KompetanseBuilder(jan2020)
            .medKompetanse("  SS  ", barn1)
            .byggKompetanser().first()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("------", barn2, barn3)
            .medKompetanse("--  --", barn1)
            .byggKompetanser()

        val restKompetanser = kompetanse.minus(oppdatertKompetanse)

        assertEquals(3, restKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, restKompetanser)
    }

    @Test
    fun testMergePåfølgendePerioder() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS", barn1)
            .medKompetanse("   SSS", barn1)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSS", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = slåSammenKompetanser(kompetanser)
        assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testMergeForPerioderMedMellomrom() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS", barn1, barn2, barn3)
            .medKompetanse("    SSS", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS SSS", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = slåSammenKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testMergeForPerioderDerTidligstePeriodeHarÅpemTOM() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .medKompetanse("   -----", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = slåSammenKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
        assertEquals(null, faktiskeKompetanser.first().tom)
    }

    @Test
    fun testMergeForPerioderMedOverlapp() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("-----", barn1, barn2, barn3)
            .medKompetanse("   -----", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("--------", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = slåSammenKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testMergeForPerioderDerSenestePeriodeHarÅpemTOM() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("------", barn1, barn2, barn3)
            .medKompetanse("   ----->", barn1, barn2, barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("->", barn1, barn2, barn3)
            .byggKompetanser()

        val faktiskeKompetanser = slåSammenKompetanser(kompetanser)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
        assertEquals(jan2020.tilYearMonth(), faktiskeKompetanser.first().fom)
        assertEquals(null, faktiskeKompetanser.first().tom)
    }

    @Test
    fun komplekseSlåSammenKommpetanserTest() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .medKompetanse("SSSPPSS", barn2)
            .medKompetanse("-SSSSSS", barn3)
            .byggKompetanser()

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS  SS", barn1, barn2, barn3)
            .medKompetanse("S      ", barn1, barn2)
            .medKompetanse("   SS  ", barn1, barn3)
            .medKompetanse("       ", barn2, barn3)
            .medKompetanse("       ", barn1)
            .medKompetanse("   PP  ", barn2)
            .medKompetanse("-      ", barn3)
            .byggKompetanser()

        val faktiskeKompetanser = slåSammenKompetanser(kompetanser)
        assertEquals(6, faktiskeKompetanser.size)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsEttBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktørId to "EEENNEEEE".tilCharTidslinje(jan2020).somRegelverk()
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS  SS--", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsForFlereBarn() {
        // "SSSSSSS", barn1
        // "SSSPPSS", barn2
        // "-SSSSSS", barn3

        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS  SS", barn1, barn2, barn3)
            .medKompetanse("S      ", barn1, barn2)
            .medKompetanse("   SS  ", barn1, barn3)
            .medKompetanse("   PP  ", barn2)
            .medKompetanse("-      ", barn3)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktørId to "EEENNEEEE".tilCharTidslinje(jan2020).somRegelverk(),
            barn2.aktørId to "EEE--NNNN".tilCharTidslinje(jan2020).somRegelverk(),
            barn3.aktørId to "EEEEEEEEE".tilCharTidslinje(jan2020).somRegelverk()
        )

        // SSS  SS--, barn1
        // SSS      , barn2
        // -SSSSSS--, barn3

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse(" SS      ", barn1, barn2, barn3)
            .medKompetanse("S        ", barn1, barn2)
            .medKompetanse("     SS--", barn1, barn3)
            .medKompetanse("-  SS    ", barn3)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder).sortedBy { it.fom }
        assertEquals(forventedeKompetanser, faktiskeKompetanser)
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

private val Person.aktørId
    get() = this.aktør.aktørId
