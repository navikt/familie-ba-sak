package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somRegelverk
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TilpassKompetanserTilRegelverkTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

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
    fun testTilpassKompetanserMotEøsToBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS--SSSS", barn1, barn2)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktørId to "EEENNEEEE".tilCharTidslinje(jan2020).somRegelverk(),
            barn2.aktørId to "EEEENNEEE".tilCharTidslinje(jan2020).somRegelverk()
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
        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }
}

private val Person.aktørId
    get() = this.aktør.aktørId
