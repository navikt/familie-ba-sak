package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TilpassKompetanserTilRegelverkTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testTilpassKompetanserUtenKompetanser() {
        val kompetanser: List<Kompetanse> = emptyList()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkTidslinje(jan2020)
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("---  ----", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserUtenEøsPerioder() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val eøsPerioder = emptyMap<Aktør, Tidslinje<Regelverk, Måned>>()

        val forventedeKompetanser = emptyList<Kompetanse>()

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsEttBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkTidslinje(jan2020)
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS  SS--", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, eøsPerioder)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsToBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS--SSSS", barn1, barn2)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilRegelverkTidslinje(jan2020),
            barn2.aktør to "EEEENNEEE".tilRegelverkTidslinje(jan2020)
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS-   SS-", barn1, barn2)
            .medKompetanse("     S", barn1)
            .medKompetanse("   - ", barn2)
            .byggKompetanser().sortedBy { it.fom }

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
            barn1.aktør to "EEENNEEEE".tilRegelverkTidslinje(jan2020),
            barn2.aktør to "EEE--NNNN".tilRegelverkTidslinje(jan2020),
            barn3.aktør to "EEEEEEEEE".tilRegelverkTidslinje(jan2020)
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
        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }
}
