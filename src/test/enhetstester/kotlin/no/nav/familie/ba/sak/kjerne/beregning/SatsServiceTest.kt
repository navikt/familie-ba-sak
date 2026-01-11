package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.årMnd
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.ORBA
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.SMA
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.TILLEGG_ORBA
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.eøs.util.uendelig
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.plus
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilTidslinje
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsServiceTest {
    @Test
    fun `Skal opprette korrekt tidslinje for ordinær barnetrygd for barn som ble 6 år mens TILLEGGS_ORBA var aktivt`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2017, 4, 6))

        val ordinærTidslinje = lagOrdinærTidslinje(barn)
        val ordinærePerioder = ordinærTidslinje.tilPerioderIkkeNull().toList()

        assertEquals(12, ordinærePerioder.size)

        assertPeriode(TestKrPeriode(beløp = 970, fom = "2017-04", tom = "2019-02"), ordinærePerioder[0])
        assertPeriode(TestKrPeriode(beløp = 1054, fom = "2019-03", tom = "2020-08"), ordinærePerioder[1])
        assertPeriode(TestKrPeriode(beløp = 1354, fom = "2020-09", tom = "2021-08"), ordinærePerioder[2])
        assertPeriode(TestKrPeriode(beløp = 1654, fom = "2021-09", tom = "2021-12"), ordinærePerioder[3])
        assertPeriode(TestKrPeriode(beløp = 1676, fom = "2022-01", tom = "2023-02"), ordinærePerioder[4])
        assertPeriode(TestKrPeriode(beløp = 1723, fom = "2023-03", tom = "2023-03"), ordinærePerioder[5])
        assertPeriode(TestKrPeriode(beløp = 1083, fom = "2023-04", tom = "2023-06"), ordinærePerioder[6])
        assertPeriode(TestKrPeriode(beløp = 1310, fom = "2023-07", tom = "2023-12"), ordinærePerioder[7])
        assertPeriode(TestKrPeriode(beløp = 1510, fom = "2024-01", tom = "2024-08"), ordinærePerioder[8])
        assertPeriode(TestKrPeriode(beløp = 1766, fom = "2024-09", tom = "2025-04"), ordinærePerioder[9])
        assertPeriode(TestKrPeriode(beløp = 1968, fom = "2025-05", tom = "2026-01"), ordinærePerioder[10])
        assertPeriode(TestKrPeriode(beløp = 2012, fom = "2026-02", tom = null), ordinærePerioder[11])
    }

    @Test
    fun `Skal opprette korrekt tidslinje for ordinær barnetrygd for barn som er født etter at TILLEGGS_ORBA er ferdig`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2025, 1, 6))

        val ordinærTidslinje = lagOrdinærTidslinje(barn)
        val ordinærePerioder = ordinærTidslinje.tilPerioderIkkeNull().toList()

        assertEquals(3, ordinærePerioder.size)

        assertPeriode(TestKrPeriode(beløp = 1766, fom = "2025-01", tom = "2025-04"), ordinærePerioder[0])
        assertPeriode(TestKrPeriode(beløp = 1968, fom = "2025-05", tom = "2026-01"), ordinærePerioder[1])
        assertPeriode(TestKrPeriode(beløp = 2012, fom = "2026-02", tom = null), ordinærePerioder[2])
    }

    @Test
    fun `Skal opprette korrekt tidslinje for ordinær barnetrygd for barn som blir 6 år etter at TILLEGGS_ORBA er fjernet`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 12, 6))

        val ordinærTidslinje = lagOrdinærTidslinje(barn)
        val ordinærePerioder = ordinærTidslinje.tilPerioderIkkeNull().toList()

        assertEquals(8, ordinærePerioder.size)

        assertPeriode(TestKrPeriode(beløp = 1054, fom = "2019-12", tom = "2020-08"), ordinærePerioder[0])
        assertPeriode(TestKrPeriode(beløp = 1354, fom = "2020-09", tom = "2021-08"), ordinærePerioder[1])
        assertPeriode(TestKrPeriode(beløp = 1654, fom = "2021-09", tom = "2021-12"), ordinærePerioder[2])
        assertPeriode(TestKrPeriode(beløp = 1676, fom = "2022-01", tom = "2023-02"), ordinærePerioder[3])
        assertPeriode(TestKrPeriode(beløp = 1723, fom = "2023-03", tom = "2023-06"), ordinærePerioder[4])
        assertPeriode(TestKrPeriode(beløp = 1766, fom = "2023-07", tom = "2025-04"), ordinærePerioder[5])
        assertPeriode(TestKrPeriode(beløp = 1968, fom = "2025-05", tom = "2026-01"), ordinærePerioder[6])
        assertPeriode(TestKrPeriode(beløp = 2012, fom = "2026-02", tom = null), ordinærePerioder[7])
    }

    private fun assertPeriode(
        forventet: TestKrPeriode,
        faktisk: Periode<Int>,
    ) {
        assertEquals(forventet.beløp, faktisk.verdi, "Forskjell i beløp")
        assertEquals(
            forventet.fom?.let { årMnd(it) },
            faktisk.fom?.toYearMonth(),
            "Forskjell i fra-og-med",
        )
        assertEquals(
            forventet.tom?.let { årMnd(it) },
            faktisk.tom?.toYearMonth(),
            "Forskjell i til-og-med",
        )
    }

    private data class TestKrPeriode(
        val beløp: Int,
        val fom: String?,
        val tom: String?,
    )

    @Nested
    inner class SatstypeTidslinje {
        @Test
        fun `Skal gi riktig sats for ordinær barnetrygd, 6 til 18 år`() {
            val forventet =
                (uendelig..feb(2019)).tilTidslinje { 970 } +
                    (mar(2019)..feb(2023)).tilTidslinje { 1054 } +
                    (mar(2023)..jun(2023)).tilTidslinje { 1083 } +
                    (jul(2023)..des(2023)).tilTidslinje { 1310 } +
                    (jan(2024)..aug(2024)).tilTidslinje { 1510 } +
                    (sep(2024)..apr(2025)).tilTidslinje { 1766 } +
                    (mai(2025)..jan(2026)).tilTidslinje { 1968 } +
                    (feb(2026)..uendelig).tilTidslinje { 2012 }

            val faktisk = satstypeTidslinje(ORBA)

            assertEquals(forventet.tilPerioder(), faktisk.tilPerioder())
        }

        @Test
        fun `Skal gi riktig sats for tillegg ordinær barnetrygd, 0 til 6 år`() {
            val forventet =
                (uendelig..feb(2019)).tilTidslinje { 970 } +
                    (mar(2019)..aug(2020)).tilTidslinje { 1054 } +
                    (sep(2020)..aug(2021)).tilTidslinje { 1354 } +
                    (sep(2021)..des(2021)).tilTidslinje { 1654 } +
                    (jan(2022)..feb(2023)).tilTidslinje { 1676 } +
                    (mar(2023)..jun(2023)).tilTidslinje { 1723 } +
                    (jul(2023)..aug(2024)).tilTidslinje { 1766 }

            val faktisk = satstypeTidslinje(TILLEGG_ORBA)

            assertEquals(forventet.tilPerioder(), faktisk.tilPerioder())
        }

        @Test
        fun `Skal gi riktig sats for småbarnstillegg`() {
            val forventet =
                (uendelig..feb(2023)).tilTidslinje { 660 } +
                    (mar(2023)..jun(2023)).tilTidslinje { 678 } +
                    (jul(2023)..jan(2026)).tilTidslinje { 696 } +
                    (feb(2026)..uendelig).tilTidslinje { 712 }

            val faktisk = satstypeTidslinje(SMA)

            assertEquals(forventet.tilPerioder(), faktisk.tilPerioder())
        }

        @Test
        fun `Skal gi riktig sats for utvidet barnetrygd`() {
            val forventet =
                (uendelig..feb(2019)).tilTidslinje { 970 } +
                    (mar(2019)..feb(2023)).tilTidslinje { 1054 } +
                    (mar(2023)..jun(2023)).tilTidslinje { 2489 } +
                    (jul(2023)..jan(2026)).tilTidslinje { 2516 } +
                    (feb(2026)..uendelig).tilTidslinje { 2572 }

            val faktisk = satstypeTidslinje(UTVIDET_BARNETRYGD)

            assertEquals(forventet.tilPerioder(), faktisk.tilPerioder())
        }
    }
}
