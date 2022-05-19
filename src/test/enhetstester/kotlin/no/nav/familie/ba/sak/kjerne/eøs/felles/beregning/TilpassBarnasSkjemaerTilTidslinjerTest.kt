package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilEøsRegelverkTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TilpassBarnasSkjemaerTilTidslinjerTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    val tomKompetanseForBarn: (Aktør) -> Kompetanse =
        { aktør -> Kompetanse(null, null, barnAktører = setOf(aktør)) }

    @Test
    fun testTilpassKompetanserUtenKompetanser() {
        val kompetanser: List<Kompetanse> = emptyList()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilEøsRegelverkTidslinje(jan2020)
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("---  ----", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserUtenEøsPerioder() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val eøsPerioder = emptyMap<Aktør, Tidslinje<Regelverk, Måned>>()

        val forventedeKompetanser = emptyList<Kompetanse>()

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsEttBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSSSSSS", barn1)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilEøsRegelverkTidslinje(jan2020)
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS  SS--", barn1)
            .byggKompetanser()

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsToBarn() {
        val kompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS--SSSS", barn1, barn2)
            .byggKompetanser()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEENNEEEE".tilEøsRegelverkTidslinje(jan2020),
            barn2.aktør to "EEEENNEEE".tilEøsRegelverkTidslinje(jan2020)
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SS-   SS-", barn1, barn2)
            .medKompetanse("     S", barn1)
            .medKompetanse("   - ", barn2)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)

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
            barn1.aktør to "EEENNEEEE".tilEøsRegelverkTidslinje(jan2020),
            barn2.aktør to "EEE--NNNN".tilEøsRegelverkTidslinje(jan2020),
            barn3.aktør to "EEEEEEEEE".tilEøsRegelverkTidslinje(jan2020)
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

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)
            .sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser til barn med åpne regelverkstidslinjer`() {

        val kompetanser: List<Kompetanse> = emptyList()

        val eøsPerioder = mapOf(
            barn1.aktør to "EEEEEEEEE>".tilEøsRegelverkTidslinje(jan2020),
            barn2.aktør to "  EEEEEEEEE>".tilEøsRegelverkTidslinje(jan2020),
        )

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("--", barn1)
            .medKompetanse("  ->", barn1, barn2)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)
            .sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser til barn som ikke lenger har EØS-perioder`() {
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
            barn1.aktør to "EEENNEEEE".tilEøsRegelverkTidslinje(jan2020),
            barn2.aktør to "EEE--NNNN".tilEøsRegelverkTidslinje(jan2020),
            barn3.aktør to "NNNN-----".tilEøsRegelverkTidslinje(jan2020)
        )

        // SSS  SS--, barn1
        // SSS      , barn2

        val forventedeKompetanser = KompetanseBuilder(jan2020)
            .medKompetanse("SSS      ", barn1, barn2)
            .medKompetanse("     SS--", barn1)
            .byggKompetanser().sortedBy { it.fom }

        val faktiskeKompetanser = kompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(eøsPerioder) { Kompetanse.NULL }
            .tilSkjemaer(0L)
            .sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }
}
