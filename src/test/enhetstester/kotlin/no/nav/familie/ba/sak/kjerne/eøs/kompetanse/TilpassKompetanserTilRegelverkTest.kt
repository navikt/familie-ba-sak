package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somBoolskTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilAnnenForelderOmfattetAvNorskLovgivningTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkResultatTidslinje
import no.nav.familie.tidslinje.Tidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class TilpassKompetanserTilRegelverkTest {
    val jan2020 = jan(2020)
    val barn1 = tilfeldigPerson()
    val barn2 = tilfeldigPerson()
    val barn3 = tilfeldigPerson()

    @Test
    fun testTilpassKompetanserUtenKompetanser() {
        val kompetanser: List<Kompetanse> = emptyList()

        val eøsPerioder =
            mapOf(
                barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
            )
        val annenForelderOmfattetTidslinje =
            "++++-----++++++".tilAnnenForelderOmfattetAvNorskLovgivningTidslinje(jan2020)

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse(
                    "---      ",
                    barn1,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = true,
                ).medKompetanse(
                    "     ----",
                    barn1,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = false,
                ).byggKompetanser()

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = eøsPerioder,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                annenForelderOmfattetAvNorskLovgivningTidslinje = annenForelderOmfattetTidslinje,
                inneværendeMåned = YearMonth.now(),
            )

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserUtenEøsPerioder() {
        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SSSSSSS", barn1)
                .byggKompetanser()

        val eøsPerioder = emptyMap<Aktør, Tidslinje<RegelverkResultat>>()

        val forventedeKompetanser = emptyList<Kompetanse>()

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = eøsPerioder,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                inneværendeMåned = YearMonth.now(),
            )

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsEttBarn() {
        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SSSSSSS", barn1)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
            )

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SSS  SS--", barn1)
                .byggKompetanser()

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                inneværendeMåned = YearMonth.now(),
            )

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsToBarn() {
        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SS--SSSS", barn1, barn2)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
                barn2.aktør to "EEEENNEEE".tilRegelverkResultatTidslinje(jan2020),
            )

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SS-   SS-", barn1, barn2)
                .medKompetanse("     S", barn1)
                .medKompetanse("   - ", barn2)
                .byggKompetanser()
                .sortedBy { it.fom }

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                inneværendeMåned = YearMonth.now(),
            ).sortedBy { it.fom }

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun testTilpassKompetanserMotEøsForFlereBarn() {
        // "SSSSSSS", barn1
        // "SSSPPSS", barn2
        // "-SSSSSS", barn3

        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse(" SS  SS", barn1, barn2, barn3)
                .medKompetanse("S      ", barn1, barn2)
                .medKompetanse("   SS  ", barn1, barn3)
                .medKompetanse("   PP  ", barn2)
                .medKompetanse("-      ", barn3)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
                barn2.aktør to "EEE--NNNN".tilRegelverkResultatTidslinje(jan2020),
                barn3.aktør to "EEEEEEEEE".tilRegelverkResultatTidslinje(jan2020),
            )

        // SSS  SS--, barn1
        // SSS      , barn2
        // -SSSSSS--, barn3

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse(" SS      ", barn1, barn2, barn3)
                .medKompetanse("S        ", barn1, barn2)
                .medKompetanse("     SS--", barn1, barn3)
                .medKompetanse("-  SS    ", barn3)
                .byggKompetanser()
                .sortedBy { it.fom }

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                inneværendeMåned = YearMonth.now(),
            ).sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser til barn med åpne regelverkstidslinjer`() {
        val kompetanser: List<Kompetanse> = emptyList()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEEEEEEEE>".tilRegelverkResultatTidslinje(jan2020),
                barn2.aktør to "  EEEEEEEEE>".tilRegelverkResultatTidslinje(jan2020),
            )

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("--", barn1)
                .medKompetanse("  ->", barn1, barn2)
                .byggKompetanser()
                .sortedBy { it.fom }

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                inneværendeMåned = YearMonth.now(),
            ).sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser til barn som ikke lenger har EØS-perioder`() {
        // "SSSSSSS", barn1
        // "SSSPPSS", barn2
        // "-SSSSSS", barn3

        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse(" SS  SS", barn1, barn2, barn3)
                .medKompetanse("S      ", barn1, barn2)
                .medKompetanse("   SS  ", barn1, barn3)
                .medKompetanse("   PP  ", barn2)
                .medKompetanse("-      ", barn3)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
                barn2.aktør to "EEE--NNNN".tilRegelverkResultatTidslinje(jan2020),
                barn3.aktør to "NNNN-----".tilRegelverkResultatTidslinje(jan2020),
            )

        // SSS  SS--, barn1
        // SSS      , barn2

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SSS      ", barn1, barn2)
                .medKompetanse("     SS--", barn1)
                .byggKompetanser()
                .sortedBy { it.fom }

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                kompetanser,
                barnasRegelverkResultatTidslinjer,
                emptyMap(),
                inneværendeMåned = YearMonth.now(),
            ).sortedBy { it.fom }

        Assertions.assertEquals(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `tilpass kompetanser mot eøs for to barn, der ett barn har etterbetaling 3 år`() {
        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("SS--SSSS", barn1, barn2)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEENNEEEE".tilRegelverkResultatTidslinje(jan2020),
                barn2.aktør to "EEEENNEEE".tilRegelverkResultatTidslinje(jan2020),
            )

        val endredeUtbetalingPerioderSomKreverKompetanseTidlinjer =
            mapOf(
                barn1.aktør to "FFF      ".somBoolskTidslinje(jan2020),
            )

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("      SS-", barn1, barn2)
                .medKompetanse("     S", barn1)
                .medKompetanse("SS-- ", barn2)
                .byggKompetanser()
                .sortedBy { it.fom }

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = endredeUtbetalingPerioderSomKreverKompetanseTidlinjer,
                inneværendeMåned = YearMonth.now(),
            ).sortedBy { it.fom }

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal klippe kompetansene basert på endret utbetaling som fører til stans av utbetaling`() {
        val kompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("->", barn1, barn2)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEEEEEEEE".tilRegelverkResultatTidslinje(jan2020),
                barn2.aktør to "EEEEEEEEE".tilRegelverkResultatTidslinje(jan2020),
            )

        val endredeUtbetalingPerioderSomKreverKompetanseTidlinjer =
            mapOf(
                Pair(barn1.aktør, "     FFFF".somBoolskTidslinje(jan2020)),
            )

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = endredeUtbetalingPerioderSomKreverKompetanseTidlinjer,
                inneværendeMåned = YearMonth.now(),
            ).sortedBy { it.fom }

        val forventedeKompetanser =
            KompetanseBuilder(jan2020)
                .medKompetanse("-----", barn1, barn2)
                .medKompetanse("     ----", barn2)
                .byggKompetanser()
                .sortedBy { it.fom }

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal sette tom på kompetanse til uendelig dersom tom på tidslinje er senere nåværende måned`() {
        val inneværendeMåned = YearMonth.of(2024, 11)
        val sep2024 = sep(2024)

        val kompetanser =
            KompetanseBuilder(sep2024)
                .medKompetanse("----", barn1, barn2)
                .medKompetanse("    --", barn1)
                .medKompetanse("      ->", barn1, barn2)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEEEEEEEEEE".tilRegelverkResultatTidslinje(sep2024),
                barn2.aktør to "EEEE  EE".tilRegelverkResultatTidslinje(sep2024),
            )

        val endredeUtbetalingPerioderSomKreverKompetanseTidlinjer =
            mapOf(
                Pair(barn1.aktør, "TTTTTTTTTTT".somBoolskTidslinje(sep2024)),
                Pair(barn2.aktør, "TTTTFFTT".somBoolskTidslinje(sep2024)),
            )

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = endredeUtbetalingPerioderSomKreverKompetanseTidlinjer,
                inneværendeMåned = inneværendeMåned,
            )

        val forventedeKompetanser =
            KompetanseBuilder(sep2024)
                .medKompetanse("->", barn1, barn2)
                .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal ikke sette tom på kompetanse til uendelig dersom tom på tidslinje er samme som inneværende måned`() {
        val inneværendeMåned = YearMonth.of(2024, 12)
        val sep2024 = sep(2024)

        val kompetanser =
            KompetanseBuilder(sep2024)
                .medKompetanse("----", barn1, barn2)
                .medKompetanse("    --", barn1)
                .medKompetanse("      ->", barn1, barn2)
                .byggKompetanser()

        val barnasRegelverkResultatTidslinjer =
            mapOf(
                barn1.aktør to "EEEEEEEEEEE".tilRegelverkResultatTidslinje(sep2024),
                barn2.aktør to "EEEE  EE".tilRegelverkResultatTidslinje(sep2024),
            )

        val endredeUtbetalingPerioderSomKreverKompetanseTidlinjer =
            mapOf(
                Pair(barn1.aktør, "TTTTTTTTTTT".somBoolskTidslinje(sep2024)),
                Pair(barn2.aktør, "TTTTFFTT".somBoolskTidslinje(sep2024)),
            )

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = kompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = endredeUtbetalingPerioderSomKreverKompetanseTidlinjer,
                inneværendeMåned = inneværendeMåned,
            )

        val forventedeKompetanser =
            KompetanseBuilder(sep2024)
                .medKompetanse("----", barn1, barn2)
                .medKompetanse("    ->", barn1)
                .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal ikke generere kompetanser hvis eøs-regelverk er frem i tid`() {
        val barnaRegelverkTidslinjer =
            mapOf(barn1.aktør to "EEE".tilRegelverkResultatTidslinje(jan(2025)))

        val faktiskeKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = emptyList(),
                barnaRegelverkTidslinjer = barnaRegelverkTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = emptyMap(),
                inneværendeMåned = des(2024),
            )

        val forventedeKompetanser = emptyList<Kompetanse>()

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }
}
